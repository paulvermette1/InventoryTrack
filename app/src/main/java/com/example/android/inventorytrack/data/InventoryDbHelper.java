package com.example.android.inventorytrack.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventorytrack.data.InventoryContract.ProductEntry;
import com.example.android.inventorytrack.data.InventoryContract.SupplierEntry;

/**
 * Created by paulvermette on 2016-12-19.
 */

/**
 * Database helper for  app. Manages database creation and version management.
 */
public class InventoryDbHelper extends SQLiteOpenHelper {

    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "inventory_tracker.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link InventoryDbHelper}.
     *
     * @param context of the app
     */
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the products table
        String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductEntry.TABLE_NAME + " ("
                + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER DEFAULT 0, "
                + ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER DEFAULT 0, "
                + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID + " INTEGER NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_PHOTO + " BLOB);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);

        // Create a String that contains the SQL statement to create the suppliers table
        String SQL_CREATE_SUPPLIERS_TABLE = "CREATE TABLE " + SupplierEntry.TABLE_NAME + " ("
                + SupplierEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SupplierEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL, "
                + SupplierEntry.COLUMN_SUPPLIER_EMAIL + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_SUPPLIERS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.
    }
}