package com.example.android.inventorytrack;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.example.android.inventorytrack.data.InventoryContract;
import com.example.android.inventorytrack.data.ProductProvider;
import com.example.android.inventorytrack.data.SupplierProvider;

import java.text.NumberFormat;

import static com.example.android.inventorytrack.ProductEditor.REQUEST_READ_STORAGE_PERMISSION;
import static com.example.android.inventorytrack.R.string.price;

/**
 * Created by paulvermette on 2017-01-01.
 */

public class InventoryHelpers {

    // this is a static class that is only used to centralize some application logic
    // there should be no need to instantiate it - making it private.
    private static void InventoryHelpers() {
    }

    // This method takes price as an integer (likely from the DB), and returns a formatted
    // string, with the price represented in dollars (not cents as it is stored in the DB)
    public static String IntPriceToString(int intPrice) {

        double doublePrice = (double) intPrice / 100;
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        return nf.format(doublePrice);
    }

    // This method takes a string input from the UI and converts it into an integer
    // for saving to the DB. It also takes care of changing the price to cents not dollars
    public static int StringPriceToInt(String stringPrice) {
        int intPrice = 0;

        // Find the location of the decimal
        int decimalPos = stringPrice.indexOf(".");
        if (decimalPos >= 0) {
            // Pad the appropriate number with zeros depending where / if the the decimal exists in the string
            switch (stringPrice.length() - decimalPos) {
                case 1:
                    // The string entered by the user looks something like "12."
                    stringPrice = stringPrice.substring(0, decimalPos) + "00";
                    break;
                case 2:
                    // The string entered by the user looks something like "12.1"
                    stringPrice = stringPrice.substring(0, decimalPos) + stringPrice.substring(decimalPos + 1, stringPrice.length()) + "0";
                    break;
                default:
                    stringPrice = stringPrice.substring(0, decimalPos) + stringPrice.substring(decimalPos + 1, decimalPos + 3);
            }
        } else {
            // no decimal was found, so just add the 00 to convert from dollars to cents
            stringPrice = stringPrice + "00";
        }
        return Integer.parseInt(stringPrice);
    }

    // The following "seed" methods are to seed an empty database with data.
    // Since this project's scope does not include maintaining
    // suppliers the seedSuppliers method needs to be able to be called before allowing a user to
    // manually add new products.
    // The seed suppliers method can be run separately if the user chooses not to start with
    // with the sample data in its entirety.
    public static void seedDatabase(Context context) {
        seedProducts(context);
        seedSuppliers(context);
    }

    private static void seedProducts(Context context) {
        // Create multiple rows at once by creating
        // an array for each column in the table then use a for loop to add each row
        // with the array elements per their array index
        String[] productName = new String[10];
        int[] productQty = new int[10];
        int[] productSupplier = new int[10];
        int[] productPrice = new int[10];

        productName[0] = "VOLKL - MANTRA SKIS 2016";
        productName[1] = "ROSSIGNOL - SAVORY 7 SKIS - WOMEN'S 2016";
        productName[2] = "HEAD - STRONG INSTINCT TI SKIS 2016";
        productName[3] = "ROSSIGNOL - SAFFRON 7 SKIS - WOMEN'S 2016";
        productName[4] = "DPS - NINA 99 HYBRID SKIS - WOMEN'S 2016";
        productName[5] = "VOLKL - AURA SKIS - WOMEN'S 2016";
        productName[6] = "SALOMON - X-DRIVE 8.0 SKIS 2016";
        productName[7] = "K2 - MBELUVED 78 TI SKIS";
        productName[8] = "BLIZZARD - LATIGO SKIS 2016";
        productName[9] = "BLIZZARD - POWER S6 SKIS 2016";

        productQty[0] = 3;
        productQty[1] = 2;
        productQty[2] = 3;
        productQty[3] = 7;
        productQty[4] = 4;
        productQty[5] = 7;
        productQty[6] = 13;
        productQty[7] = 3;
        productQty[8] = 6;
        productQty[9] = 1;

        productSupplier[0] = 1;
        productSupplier[1] = 1;
        productSupplier[2] = 2;
        productSupplier[3] = 1;
        productSupplier[4] = 2;
        productSupplier[5] = 1;
        productSupplier[6] = 1;
        productSupplier[7] = 2;
        productSupplier[8] = 2;
        productSupplier[9] = 1;

        productPrice[0] = 59999;
        productPrice[1] = 79995;
        productPrice[2] = 79995;
        productPrice[3] = 34995;
        productPrice[4] = 62949;
        productPrice[5] = 59500;
        productPrice[6] = 39995;
        productPrice[7] = 55995;
        productPrice[8] = 89000;
        productPrice[9] = 71995;

        // Create a counter that can be passed out of the for loop to record the number of rows inserted
        int rowsInserted = 0;

        ContentValues values = new ContentValues();

        for (int i = 0; i < 10; i++) {
            values.clear();
            values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME, productName[i]);
            values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, productQty[i]);
            values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID, productSupplier[i]);
            values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE, productPrice[i]);
            Uri result = context.getContentResolver().insert(InventoryContract.ProductEntry.CONTENT_URI,values);
            long newRowId = ContentUris.parseId(result);
            if (newRowId == -1) {
                break;
            }
            rowsInserted++;
        }
        if (rowsInserted == 0) {
            Toast.makeText(context.getApplicationContext(), context.getString(R.string.save_operation_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context.getApplicationContext(), rowsInserted + " " + context.getString(R.string.multi_save_operation_succeeded),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Seed the Suppliers table
    public static void seedSuppliers (Context context) {
        // Create multiple rows at once by creating
        // an array for each column in the table then use a for loop to add each row
        // with the array elements per their array index
        String[] suppliertName = new String[2];
        String[] supplierEmail = new String[2];

        suppliertName[0] = "Skis-R-Us";
        suppliertName[1] = "Ski Ski Ski";

        supplierEmail[0] = "paulvermette1@gmail.com";
        supplierEmail[1] = "pvermette10@shaw.ca";

        // Create a counter that can be passed out of the for loop to record the number of rows inserted
        int rowsInserted = 0;
        ContentValues values = new ContentValues();

        for(int i=0 ; i < 2 ; i++) {
            values.clear();
            values.put(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME, suppliertName[i]);
            values.put(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL, supplierEmail[i]);
            Uri supplierResult = context.getContentResolver().insert(
                    InventoryContract.SupplierEntry.CONTENT_URI,
                    values);
            long newRowId = ContentUris.parseId(supplierResult);
            if (newRowId==-1) {
                break;
            }
            rowsInserted ++;
        }
    }

    public static boolean suppliersExist (Context context) {

        SupplierProvider supplierProvider = new SupplierProvider();
        Uri supplierUri = InventoryContract.SupplierEntry.CONTENT_URI;

        // This loader will execute the ContentProvider's query method on a background thread
        // with all selection parameters set to null, all columns and rows will be returned
        Cursor cursor = context.getContentResolver().query(supplierUri, null, null, null, null);

        if (cursor.moveToFirst()) {
            // there is at least one row, so return that suppliers exist --> true
            return true;
        } else {
            return false;
        }

    }
}
