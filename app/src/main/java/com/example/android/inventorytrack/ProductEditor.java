package com.example.android.inventorytrack;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.LoaderManager;
import android.content.Loader;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventorytrack.data.InventoryContract;
import com.example.android.inventorytrack.data.InventoryContract.ProductEntry;
import com.example.android.inventorytrack.data.InventoryContract.SupplierEntry;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;



/**
 * Created by paulvermette on 2016-12-20.
 * Adding butter knife 2017-01-22.
 */

public class ProductEditor extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, ProductQuantityDialog.OrderProductDialogListener {
    public static final int REQUEST_READ_STORAGE_PERMISSION = 1;
    public static final int ORDERING_DIALOG_FRAGMENT = 1;
    public static final int RECEIVING_DIALOG_FRAGMENT = 2;
    public static final int SELLING_DIALOG_FRAGMENT = 3;
    private static final String LOG_TAG = ProductEditor.class.getSimpleName();
    // Create a unique loader ID for the supplier and product
    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int SUPPLIER_LOADER = 1;
    private static int RESULT_LOAD_IMAGE = 1;
    // Create a URI which represents the current product being edited
    private Uri mCurrentProductUri;
    private Uri mSupplierUri = SupplierEntry.CONTENT_URI;
    private Bitmap imageBitmap;
    // Create objects to handle the data fields on screen
    @BindView(R.id.photo) ImageView imageView;
    @BindView(R.id.edit_product_name) EditText mNameEditText;
    @BindView(R.id.qty_on_hand) TextView mQtyText;
    @BindView(R.id.edit_price) EditText mPriceEditText;
    @BindView(R.id.spinner_supplier) Spinner mSupplierSpinner;

    @OnClick(R.id.order_button)
    public void order(View view)  {
        ProductQuantityDialog productDialog = new ProductQuantityDialog();
        // pass in arguments to indicate the purpose of the dialog
        // which determines prompts and button labels that should be used
        Bundle args = new Bundle();
        args.putInt("dialogPurpose", ORDERING_DIALOG_FRAGMENT);
        productDialog.setArguments(args);
        productDialog.show(fragmentManager, "Order Dialog");
    }
    @OnClick(R.id.receive_button)
    public void receive(View view) {
        ProductQuantityDialog productDialog = new ProductQuantityDialog();

        // pass in arguments to indicate the purpose of the dialog
        // which determines prompts and button labels that should be used
        Bundle args = new Bundle();
        args.putInt("dialogPurpose", RECEIVING_DIALOG_FRAGMENT);
        productDialog.setArguments(args);
        productDialog.show(fragmentManager, "Receiving Dialog");
    }

    @OnClick(R.id.sale_button)
    public void sale(View view) {
        ProductQuantityDialog productDialog = new ProductQuantityDialog();
        // pass in arguments to indicate the purpose of the dialog
        // which determines prompts and button labels that should be used
        Bundle args = new Bundle();
        args.putInt("dialogPurpose", SELLING_DIALOG_FRAGMENT);
        productDialog.setArguments(args);
        productDialog.show(fragmentManager, "Sale Dialog");
    }
    @OnClick(R.id.photo_button)
    public void onClick(View arg0) {

        // Depending on the API suppoted by the device, deal with permissions to access
        // EXTERNAL STORAGE
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
            if (!ExternalStoragePermissionGranted(activity)) {
                // No permission. Proceed without launching photo picker
                return;
            }
        }
        // Either permission was granted or API is less than M so proceed with file access
        Intent intentGetPhoto = new Intent(
                Intent.ACTION_PICK);
        intentGetPhoto.setType("image/*");

        startActivityForResult(intentGetPhoto, RESULT_LOAD_IMAGE);
    }


    // Create a global supplier ID so the current product query can set the supplier ID
    // then once the list of suppliers is collected and the spinner is populated, the spinner
    // can be set to the proper value matching this ID.  Initialize to zero which will
    //be valid id this view is opened in Create/Insert NewProduct
    private int productSupplierId = 0;
    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean mProductHasChanged = false;
    // Create a variable which indicates whether the Activity is being used as a New Product add OR
    // an Edit
    private Boolean NewProduct;
    private SimpleCursorAdapter mSimpleCursorAdapter;
    // Create an arraylist inside an array list to store the list of suppliers with both the
    // supplier name and id for each.
    // This will be used to quickly translate a selection of an item in the suppliers spinner
    // to their proper ID.
    private List<List<String>> mSuppliersList = new ArrayList<List<String>>();
    private List<String> mSupplierData;
    // Create the variables & constants which manage the single dialog fragment used for
    // multiple purposes
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private Activity activity = this;
    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mPetHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    // This method decodes the image from the stream to the best size possible
    public static Bitmap decodeSampledBitmapFromStream(InputStream streamForSize, InputStream streamForImage,
                                                       int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(streamForSize, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(streamForImage, null, options);

    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // This funtion takes a bitmap and returns a byteArray
    private static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_detail);

        // Use butterknife
        ButterKnife.bind(this);

         // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));
            //set the NewProduct to Edit
            NewProduct = Boolean.TRUE;

            // Setup the Spinner by loading the list of valid suppliers
            getLoaderManager().initLoader(SUPPLIER_LOADER, null, this);

            // Invalidate the options menu, so the Delete, Order, Sell  menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));
            NewProduct = Boolean.FALSE;

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierSpinner.setOnTouchListener(mTouchListener);

/*
        // Create Click Listener for image load button
        Button buttonLoadImage = (Button) findViewById(R.id.photo_button);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                // Depending on the API suppoted by the device, deal with permissions to access
                // EXTERNAL STORAGE
                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
                if (currentapiVersion >= android.os.Build.VERSION_CODES.M) {
                    if (!ExternalStoragePermissionGranted(activity)) {
                        // No permission. Proceed without launching photo picker
                        return;
                    }
                }
                // Either permission was granted or API is less than M so proceed with file access
                Intent intentGetPhoto = new Intent(
                        Intent.ACTION_PICK);
                intentGetPhoto.setType("image*/
/*");

                startActivityForResult(intentGetPhoto, RESULT_LOAD_IMAGE);
            }
        });
*/

        // Create Click Listener for Order button
/*
        Button buttonOrder = (Button) findViewById(R.id.order_button);
        buttonOrder.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ProductQuantityDialog productDialog = new ProductQuantityDialog();
                // pass in arguments to indicate the purpose of the dialog
                // which determines prompts and button labels that should be used
                Bundle args = new Bundle();
                args.putInt("dialogPurpose", ORDERING_DIALOG_FRAGMENT);
                productDialog.setArguments(args);
                productDialog.show(fragmentManager, "Order Dialog");
            }
        });
*/

        // Create Click Listener for Receive button
/*
        Button buttonReceive = (Button) findViewById(R.id.receive_button);
        buttonReceive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ProductQuantityDialog productDialog = new ProductQuantityDialog();

                // pass in arguments to indicate the purpose of the dialog
                // which determines prompts and button labels that should be used
                Bundle args = new Bundle();
                args.putInt("dialogPurpose", RECEIVING_DIALOG_FRAGMENT);
                productDialog.setArguments(args);
                productDialog.show(fragmentManager, "Receiving Dialog");
            }
        });
*/

        // Create Click Listener for Sale button
/*
        Button buttonSale = (Button) findViewById(R.id.sale_button);
        buttonSale.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ProductQuantityDialog productDialog = new ProductQuantityDialog();
                // pass in arguments to indicate the purpose of the dialog
                // which determines prompts and button labels that should be used
                Bundle args = new Bundle();
                args.putInt("dialogPurpose", SELLING_DIALOG_FRAGMENT);
                productDialog.setArguments(args);
                productDialog.show(fragmentManager, "Sale Dialog");
            }
        });
*/

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // Execute a different set of code depending on which table is being queried
        // The loader ID holds the id passed in when the getLoadManager was called

        switch (id) {
            case EXISTING_PRODUCT_LOADER:
                // Since the editor shows all product attributes, define a projection that contains
                // all columns from the product table
                String[] pProjection = {

                        ProductEntry.COLUMN_PRODUCT_NAME,
                        ProductEntry.COLUMN_PRODUCT_QUANTITY,
                        ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID,
                        ProductEntry.COLUMN_PRODUCT_PRICE,
                        ProductEntry.COLUMN_PRODUCT_PHOTO};

                // This loader will execute the ContentProvider's query method on a background thread
                return new CursorLoader(this,   // Parent activity context
                        mCurrentProductUri,         // Query the content URI for the current product
                        pProjection,             // Columns to include in the resulting Cursor
                        null,                   // No selection clause
                        null,                   // No selection arguments
                        null);                  // Default sort order

            case SUPPLIER_LOADER:
                // This loader initialization pertains to a supplier table query
                // Create a projection to retrieve the Supplier ID and Supplier Name
                String[] sProjection = {
                        SupplierEntry._ID,
                        SupplierEntry.COLUMN_SUPPLIER_NAME,
                        SupplierEntry.COLUMN_SUPPLIER_EMAIL};

                // Now CursorLoad with all records from the suppliers table

                CursorLoader mCursorLoader = new CursorLoader(this,
                        mSupplierUri,
                        sProjection,
                        null,
                        null,
                        null);
                return mCursorLoader;
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Execute a different set of code depending on which table is being queried
        // The loader ID holds the id passed in when the getLoadManager was called
        switch (loader.getId()) {
            case EXISTING_PRODUCT_LOADER:
                // This call back is from a completed product table query
                // Proceed with moving to the first row of the cursor and reading data from it
                // (This should be the only row in the cursor)
                if (cursor.moveToFirst()) {
                    // Find the columns of product attributes that we're interested in
                    int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
                    int supplierIdColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID);
                    int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
                    int qtyColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
                    int phoColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO);

                    // Extract out the value from the Cursor for the given column index
                    String name = cursor.getString(nameColumnIndex);
                    int price = cursor.getInt(priceColumnIndex);
                    int supplierId = cursor.getInt(supplierIdColumnIndex);
                    int qty = cursor.getInt(qtyColumnIndex);
                    byte[] photoData = cursor.getBlob(phoColumnIndex);

                    // Update the views on the screen with the values from the database
                    mNameEditText.setText(name);
                    mQtyText.setText(Integer.toString(qty));

                    // Convert the price integer from cents to dollars and cents
                    String stringPrice = InventoryHelpers.IntPriceToString(price);
                    mPriceEditText.setText(stringPrice);

                    if (photoData != null) {
                        imageBitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
                        imageView.setImageBitmap(imageBitmap);
                    } else {
                        imageView.setImageResource(R.drawable.no_img);
                    }


                    // for the supplier spinner the value will be set in the other branch
                    // where the list of suppliers is queried and the spinner setup
                    productSupplierId = supplierId;

                    // Setup the Spinner by loading the list of valid suppliers
                    getLoaderManager().initLoader(SUPPLIER_LOADER, null, this);

                }

                break;

            case SUPPLIER_LOADER:
                // This call back is from a completed supplier table CursorLoad
                // Populate the spinner items with the SimpleCursorAdapter
                // Then if the screen is editing an existing product, set the position of the
                // spinner to the id value associated with that product
                String[] adapterCols = new String[]{SupplierEntry.COLUMN_SUPPLIER_NAME};
                int[] adapterRowViews = new int[]{android.R.id.text1};

                mSimpleCursorAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, cursor, adapterCols, adapterRowViews, 0);
                mSimpleCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSupplierSpinner.setAdapter(mSimpleCursorAdapter);

                // load the supplier table into the mSuppliersList arraylist
                // we will then have a two dimensional array which has the supplier names in the same
                // sequence as the spinner, making it simpler to find the spinner selections, corresponding
                // supplier ID (using the name)

                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    // Get the values for the current row
                    int supplierColIdIndex = cursor.getColumnIndex(SupplierEntry._ID);
                    Integer mCurrentSupplerId = cursor.getInt(supplierColIdIndex);
                    int supplierColNameIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_NAME);
                    String mCurrentSupplerName = cursor.getString(supplierColNameIndex);
                    int supplierColEmailIndex = cursor.getColumnIndex(SupplierEntry.COLUMN_SUPPLIER_EMAIL);
                    String mCurrentSupplierEmail = cursor.getString(supplierColEmailIndex);

                    // assign the elements to an arraylist
                    mSupplierData = new ArrayList<String>();
                    mSupplierData.add(mCurrentSupplerId.toString());
                    mSupplierData.add(mCurrentSupplerName);
                    mSupplierData.add(mCurrentSupplierEmail);

                    // Now add that new mCurrentSupplier ArrayList to the mSuppliersList ArrayList
                    mSuppliersList.add(mSupplierData);

                }

                // If product supplier ID is >0 then the view is being rendered for an existing
                // product and the previous loader that ran, populated that product's supplier ID
                // Now, iterate through the cursor and find the name of the supplier for the ID found
                // (since the number of rows is expected to be limited this will be faster than issuing another query)
                if (productSupplierId > 0) {

                    int spinnerPosIndex = 0;
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        int supplierColIdIndex = cursor.getColumnIndex(SupplierEntry._ID);
                        int mCurrentSupplerId = cursor.getInt(supplierColIdIndex);
                        if (mCurrentSupplerId == productSupplierId) {
                            break;
                        }
                        spinnerPosIndex++;
                    }
                    mSupplierSpinner.setSelection(spinnerPosIndex);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields and null the
        // cursor.
        mNameEditText.setText("");
        mQtyText.setText("");
        mPriceEditText.setText("");
        mSupplierSpinner.setSelection(0); // Select "Unknown" supplier
        imageView.setImageResource(R.drawable.no_img);

        mSimpleCursorAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    private void saveProduct() {
        // Gather the data from the screen
        if (TextUtils.isEmpty(mNameEditText.getText().toString().trim())) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_product_name), Toast.LENGTH_LONG).show();
            return;
        }

        String mNameString = mNameEditText.getText().toString().trim();
        int mQty;
        if (TextUtils.isEmpty(mQtyText.getText().toString().trim())) {
            // Set the qty to zero if, string is empty
            mQty = 0;
        } else {
            mQty = Integer.parseInt(mQtyText.getText().toString().trim());
        }

        int mPrice;
        // Check if price is empty
        if (TextUtils.isEmpty(mPriceEditText.getText().toString().trim())) {
            // Set the price to zero is string is empty
            mPrice = 0;
            //or a nonsense price too large for an integer to store
        } else if (mPriceEditText.getText().toString().trim().length() > 10) {
            mPrice = 0;
            Toast.makeText(getApplicationContext(), getString(R.string.price_invalid), Toast.LENGTH_LONG).show();
        } else {
            // else we have a valid price
            String stringPrice = mPriceEditText.getText().toString().trim();
            // Convert the String from the UI to an integer (representing cents, not dollars)
            // for storage to the DB
            mPrice = InventoryHelpers.StringPriceToInt(stringPrice);
        }

        byte[] mPhotoData;
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        if (drawable != null) {
            Bitmap bmap = drawable.getBitmap();
            mPhotoData = getBitmapAsByteArray(bmap);
        } else {
            mPhotoData = null;
        }

        // Get the mSupplierId by checking the position of the supplier spinner
        // Then go to that position in the mSuppliersList and extract the ID from that row
        int spinnerPos = mSupplierSpinner.getSelectedItemPosition();
        mSupplierData = mSuppliersList.get(spinnerPos);
        int mSupplierId = Integer.parseInt(mSupplierData.get(0));

        // / Populate a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, mNameString);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, mPrice);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, mQty);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID, mSupplierId);
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO, mPhotoData);


        // Check if in Edit or New NewProduct and save accordingly
        if (NewProduct) {
            // Insert the new row, returning the primary key value of the new row
            Uri uri = getContentResolver().insert(
                    InventoryContract.ProductEntry.CONTENT_URI,
                    values);
            long newRowId = ContentUris.parseId(uri);

            if (newRowId == -1) {
                Toast.makeText(getApplicationContext(), getString(R.string.save_operation_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.save_operation_succeeded),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            int result = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (result == 1) {
                Toast.makeText(getApplicationContext(), getString(R.string.save_operation_succeeded),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.save_operation_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                saveProduct();
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(ProductEditor.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(ProductEditor.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard_changes, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, null);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    // This gets called when the invalidateOptionsMenu() is called
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (NewProduct) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    // this code takes care of deleting (when the Delete option is selected from the options menu
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteProduct() {
        // call the ProductProvider delete function with the Uri
        int result = getContentResolver().delete(mCurrentProductUri, null, null);

        // if the return value (the number of rows deleted) is one, then all good, else errors
        if (result == 1) {
            Toast.makeText(getApplicationContext(), getString(R.string.delete_operation_succeeded),
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.delete_operation_failed),
                    Toast.LENGTH_SHORT).show();
        }

    }

    // this method handles an image selected by the image selection intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                Uri selectedImage = data.getData();
                InputStream imageInputStream = getContentResolver().openInputStream(selectedImage);
                InputStream imageInputStreamForSizeSample = getContentResolver().openInputStream(selectedImage);
                imageBitmap = decodeSampledBitmapFromStream(imageInputStreamForSizeSample, imageInputStream, 375, 175);
                imageView.setImageBitmap(imageBitmap);
                // Flag that the product information has changed
                mProductHasChanged = true;

            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, getString(R.string.photo_load_error));
            }

        }
    }

    // This method handles the positive response from the dialog fragment
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

        // get the dialogPurpose argument from the dialog object to determine what function the
        // dialog was serving when the user clicked the Positive dialog option (Order,Receive,Sale)
        Bundle argsBundle = dialog.getArguments();
        int dialogPurpose = argsBundle.getInt("dialogPurpose");
        // get the value entered in the EditText window
        EditText editTextView;
        String quantityEntered;
        editTextView = (EditText) dialog.getDialog().findViewById(R.id.quantity);
        quantityEntered = editTextView.getText().toString().trim();
        // if quantity entered is empty, default to zero
        if (quantityEntered.equals("")) {
            quantityEntered = "0";
        }

        switch (dialogPurpose) {
            case ORDERING_DIALOG_FRAGMENT:
                handleOrder(quantityEntered);
                break;
            case RECEIVING_DIALOG_FRAGMENT:
                handleReceiving(quantityEntered);
                break;
            case SELLING_DIALOG_FRAGMENT:
                handleSale(quantityEntered);
                break;
            default:
                Log.e(LOG_TAG, getString(R.string.dialog_purpose_not_found));
        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // the user pressed cancel in the dialog...do nothing
    }


    private void handleOrder(String quantityOrdered) {
        // Create an email to send to the supplier
        // Start by gathering the product information being ordered
        if (quantityOrdered.equals("0")) {
            Toast.makeText(getApplicationContext(), getString(R.string.order_less_than_one), Toast.LENGTH_LONG).show();
        } else {
            String emailBody = getString(R.string.order_email_header);
            emailBody += "\n" + getString(R.string.order_email_product_heading) + " " + mNameEditText.getText().toString().trim();
            emailBody += "\n" + getString(R.string.order_email_quantity_heading) + " " + quantityOrdered;

            // Get the mSupplierEmail by checking the position of the supplier spinner
            // Then go to that position in the mSuppliersList and extract the ID from that row
            int spinnerPos = mSupplierSpinner.getSelectedItemPosition();
            mSupplierData = mSuppliersList.get(spinnerPos);
            String[] mSupplierEmail = {mSupplierData.get(2)};

            // Create an email intent with the message recipient, subject and body
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_EMAIL, mSupplierEmail);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.order_email_subject));
            intent.putExtra(Intent.EXTRA_TEXT, emailBody);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }
    }

    private void handleReceiving(String quantityReceived) {
        // First handle the situation where the Qty on hand is blank
        if (mQtyText.getText().toString().equals("")) {
            mQtyText.setText("0");
        }
        // Sanity check on amount entered by user, then update Qty on hand.
        if (Integer.valueOf(quantityReceived) < 1) {
            Toast.makeText(getApplicationContext(), getString(R.string.receiving_warning_less_than_one), Toast.LENGTH_LONG).show();
        } else {
            Integer newQuantity = Integer.valueOf(quantityReceived) + Integer.valueOf(mQtyText.getText().toString());
            mQtyText.setText(newQuantity.toString());
            Toast.makeText(getApplicationContext(), getString(R.string.receiving_completed), Toast.LENGTH_LONG).show();
            // Flag that the product information has changed
            mProductHasChanged = true;
        }
    }

    private void handleSale(String quantitySold) {
        // First handle the situation where the Qty on hand is blank
        if (mQtyText.getText().toString().equals("")) {
            mQtyText.setText("0");
        }
        // Sanity Check on amount entered by user, then decrease quantity on hand
        if (Integer.valueOf(quantitySold) < 1) {
            Toast.makeText(getApplicationContext(), getString(R.string.sale_warning_less_than_one), Toast.LENGTH_LONG).show();
        } else if (Integer.valueOf(quantitySold) > Integer.valueOf(mQtyText.getText().toString())) {
            Toast.makeText(getApplicationContext(), getString(R.string.sale_warning_not_enough_stock), Toast.LENGTH_LONG).show();
        } else {
            Integer newQuantity = Integer.valueOf(mQtyText.getText().toString()) - Integer.valueOf(quantitySold);
            mQtyText.setText(newQuantity.toString());
            Toast.makeText(getApplicationContext(), getString(R.string.sale_completed), Toast.LENGTH_LONG).show();
            // Flag that the product information has changed
            mProductHasChanged = true;
        }
    }


    // The following methods deal with the permissions required to access the users EXTERNAL_STORAGE
    @TargetApi(Build.VERSION_CODES.M)
    private static boolean ExternalStoragePermissionGranted(Activity activity) {

        if (ContextCompat.checkSelfPermission(activity,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_STORAGE_PERMISSION);

            // Permission not immediately granted, return false and await the callback
            return false;
        } else {
            // Permission granted - return true
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_STORAGE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    Intent intentGetPhoto = new Intent(
                            Intent.ACTION_PICK);
                    intentGetPhoto.setType("image/*");

                    startActivityForResult(intentGetPhoto, RESULT_LOAD_IMAGE);


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }
}