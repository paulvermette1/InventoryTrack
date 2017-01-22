package com.example.android.inventorytrack.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.example.android.inventorytrack.data.InventoryContract.ProductEntry;

import static android.provider.CalendarContract.CalendarCache.URI;


/**
 * Created by paulvermette on 2016-12-19.
 */


public class ProductProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = ProductProvider.class.getSimpleName();
    // Create static constants for the Product Sale method
    public static final int SALE_SUCCESS = 1;
    public static final int SALE_ERROR_GENERAL = 0;
    public static final int SALE_ERROR_NO_STOCK = -1;
    /**
     * URI matcher code for the content URI for the products table
     */
    private static final int PRODUCTS = 10;
    /**
     * URI matcher code for the content URI for a single product in the products table
     */
    private static final int PRODUCT_ID = 11;
    // Create static Constant for data validation
    private static final int DATA_OK = 1;
    private static final int DATA_BAD = 0;
    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.products/products" will map to the
        // integer code {@link #productS}. This URI is used to provide access to MULTIPLE rows
        // of the products table.
        sUriMatcher.addURI(InventoryContract.ProductEntry.CONTENT_AUTHORITY, InventoryContract.ProductEntry.PATH_PRODUCTS, PRODUCTS);

        // The content URI of the form "content://com.example.android.products/products/#" will map to the
        // integer code {@link #product_ID}. This URI is used to provide access to ONE single row
        // of the products table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.products/products/3" matches, but
        // "content://com.example.android.products/products" (without a number at the end) doesn't match.
        sUriMatcher.addURI(InventoryContract.ProductEntry.CONTENT_AUTHORITY, InventoryContract.ProductEntry.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /**
     * Database helper object
     */
    private InventoryDbHelper mDbHelper;

    // ProductSale method handles the sale of a one or many units of the product
    // It takes in the _ID of the product, the number sold and returns a Status Code
    public static int ProductSale(Context context, int product_Id, int numberSold) {

        // First, ensure the numberSold is a valid number. If not then return SALE_ERROR_GENERAL
        if (numberSold < 1) {
            return SALE_ERROR_GENERAL;
        }

        // Create a variable to hold the newly calculated qty on hand
        int updatedQty;

        // Next determine if there is enough stock on hand. If not, then return SALE_ERROR_NO_STOCK
        // Get the current qty on hand from the DB
        ProductProvider productProvider = new ProductProvider();
        Uri productUri = ContentUris.withAppendedId(InventoryContract.ProductEntry.CONTENT_URI, product_Id);

        String[] pProjection = {
                ProductEntry.COLUMN_PRODUCT_QUANTITY};

        // This loader will execute the ContentProvider's query method on a background thread
        Cursor cursor = context.getContentResolver().query(productUri, pProjection, null, null, null);

        if (cursor.moveToFirst()) {
            int qtyColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int qty = cursor.getInt(qtyColumnIndex);
            // Check if on hand qty < amount being sold
            if (qty < numberSold) {
                // not enough on hand
                return SALE_ERROR_NO_STOCK;
            } else {
                // else update the new qty on hand
                updatedQty = qty - numberSold;
            }
        } else {
            return SALE_ERROR_GENERAL;
        }

        // Finally, update the Products table. If no errors, then return SALE_SUCCESS. If errors
        // then return SALE_ERROR_GENERAL

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, updatedQty);
        int updateResult = context.getContentResolver().update(productUri, values, null, null);
        if (updateResult == -1) {
            return SALE_ERROR_GENERAL;
        } else {
            return SALE_SUCCESS;
        }
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // For the PRODUCTS code, query the products table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the products table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.products/products/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the products table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {

        // Call the sanitize helper method to ensure the data being written to the table is valid

        // Check if data provided is bad / beyond sanitizing
        if (badDataCheck(values) == DATA_BAD) {
            Log.e(LOG_TAG, "Failed to insert data. Invalid values.");
            // set the id to -1 to indicate that the save failed
            return ContentUris.withAppendedId(uri, -1);

        } else {
            // Sanitize data and then insert the data
            values = sanitizeData(values);

            // Gets the data repository in write mode
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            // Insert the new row, returning the primary key value of the new row
            long id = db.insert(ProductEntry.TABLE_NAME, null, values);

            if (id == -1) {
                Log.e(LOG_TAG, "Failed to insert row for " + uri);
            }

            // Notify all listeners that the data has changed for the pet content URI
            getContext().getContentResolver().notifyChange(uri, null);

            // Once we know the ID of the new row in the table,
            // return the new URI with the ID appended to the end of it
            return ContentUris.withAppendedId(uri, id);

        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {


        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }


    // Helper Classes to Sanitize data *********************************************

    /**
     * Update products in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more products).
     * Return the number of rows that were successfully updated.
     */
    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Check if data provided is bad / beyond sanitizing
        if (badDataCheck(values) == DATA_BAD) {
            Log.e(LOG_TAG, "Failed to insert data. Invalid values.");
            // set the return value to 0 to indicate that the update failed
            return 0;
        }
        // Sanitize data
        values = sanitizeData(values);

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int count = db.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // Notify all listeners that the data has changed for the pet content URI
        if (count != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows that were affected
        return count;
    }

    // A helper class which ensures that the values being saved or updated are fatal errors
    private int badDataCheck(ContentValues values) {

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return DATA_BAD;
        }

        // If the {@link productEntry#COLUMN_PRODUCT_NAME} key is present,
        // check that the name value is not null.
        int result = DATA_OK;
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(ProductEntry.COLUMN_PRODUCT_NAME);
            if (TextUtils.getTrimmedLength(name) == 0) {
                result = DATA_BAD;
            }
        }
        // If the {@link productEntry#COLUMN_PRODUCT_SUPPLIER_ID} key is present,
        // check that the id value is not 0.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID)) {
            Integer supplierId = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID);
            if (supplierId == 0) {
                result = DATA_BAD;
            }
        }
        return result;
    }
    // End of Helper Classes to Sanitize data *********************************************


    // A helper class which ensures that the non-fatal data values being saved are cleaned up
    private ContentValues sanitizeData(ContentValues values) {

        // If the {@link ProductEntry#COLUMN_PRODUCT_QUANTITY} key is present,
        // check that the quantity value is valid and set to zero if not.
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            Integer quantity = values.getAsInteger(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity == null || quantity < 0) {
                // set quantity to 0
                values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, 0);
            }
        }
        return values;

    }

}