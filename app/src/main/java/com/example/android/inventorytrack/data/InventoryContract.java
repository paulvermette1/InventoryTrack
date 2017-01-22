package com.example.android.inventorytrack.data;

/**
 * Created by paulvermette on 2016-12-19.
 */

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the Pets app.
 */
public final class InventoryContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private InventoryContract() {
    }

    /**
     * Inner class that defines constant values for the products database table.
     * Each entry in the table represents a single product.
     */

    public static final class ProductEntry implements BaseColumns {

        /**
         * The "Content authority" is a name for the entire content provider, similar to the
         * relationship between a domain name and its website.
         */
        public static final String CONTENT_AUTHORITY = "com.example.android.inventorytrack.products";

        /**
         * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
         * the content provider.
         */
        public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

        /**
         * Possible path (appended to base content URI for possible URI's)
         */
        public static final String PATH_PRODUCTS = "products";

        /**
         * The content URI to access the product data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * Name of database table for products
         */
        public final static String TABLE_NAME = "products";

        /**
         * Name of the product.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_NAME = "name";

        /**
         * Supplier ID of the Product.
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_SUPPLIER_ID = "supplier_id";

        /**
         * Price of the Product.
         * <p>
         * Type: INTEGER (converting dollars to cents)
         */
        public final static String COLUMN_PRODUCT_PRICE = "price";


        /**
         * Quantity of the product on hand.
         * <p>
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";

        /**
         * Photo of the product.
         * <p>
         * Type: BLOB
         */
        public final static String COLUMN_PRODUCT_PHOTO = "photo";

    }

    /**
     * Inner class that defines constant values for the suppliers database table.
     * Each entry in the table represents a single product.
     */
    public static final class SupplierEntry implements BaseColumns {

        /**
         * The "Content authority" is a name for the entire content provider, similar to the
         * relationship between a domain name and its website.  A convenient string to use for the
         * content authority is the package name for the app, which is guaranteed to be unique on the
         * device.
         */
        public static final String CONTENT_AUTHORITY = "com.example.android.inventorytrack.suppliers";

        /**
         * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
         * the content provider.
         */
        public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

        /**
         * Possible path (appended to base content URI for possible URI's)
         */
        public static final String PATH_SUPPLIERS = "suppliers";


        /**
         * The content URI to access the supplier data in the provider
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_SUPPLIERS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of suppliers.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUPPLIERS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single supplier.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SUPPLIERS;

        /**
         * Name of database table for suppliers
         */
        public final static String TABLE_NAME = "suppliers";

        /**
         * Name of the supplier.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_SUPPLIER_NAME = "name";

        /**
         * Supplier email address.
         * <p>
         * Type: TEXT
         */
        public final static String COLUMN_SUPPLIER_EMAIL = "email";


    }
}
