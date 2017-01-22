package com.example.android.inventorytrack;

import android.app.LoaderManager;
import android.content.ContentUris;

import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.android.inventorytrack.data.InventoryContract;
import com.example.android.inventorytrack.data.InventoryContract.ProductEntry;
import com.example.android.inventorytrack.data.InventoryContract.SupplierEntry;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int PRODUCT_LOADER = 0;

    /** Adapter for the list of products */
    private ProductCursorAdapter mAdapter;

    /** TextView that is displayed when the list is empty */
    private RelativeLayout mEmptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup On Click Listener on the Seed Database button
        Button seedButton = (Button) findViewById(R.id.seed_database_button);
        seedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InventoryHelpers.seedDatabase(getApplicationContext());
            }
        });

        // Find a reference to the {@link ListView} in the layout
        ListView productListView = (ListView) findViewById(R.id.list);

        mEmptyStateTextView = (RelativeLayout) findViewById(R.id.empty_view);
        productListView.setEmptyView(mEmptyStateTextView);

        mAdapter = new ProductCursorAdapter(this,null);

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        productListView.setAdapter(mAdapter);

        // Kickoff the loader
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        // Define a projection that specifies which columns from the database
        // that will actually be used after this query.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID,
                ProductEntry.COLUMN_PRODUCT_PRICE };

        /*
        * Takes action based on the ID of the Loader that's being created
        */
        switch (loaderID) {
            case PRODUCT_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,   // Parent activity context
                        ProductEntry.CONTENT_URI,        // Table to query
                        projection,     // Projection to return
                        null,            // No selection clause
                        null,            // No selection arguments
                        null             // Default sort order
                );
            default:
                // An invalid id was passed in
                Log.e(LOG_TAG,getString(R.string.invalid_loader_id));
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        // Update the instance of Cursor Adapter with this new cursor containing updated data
        mAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    /*
     * Callback Called when the data needs to be deleted
     */
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.main_catalog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Add" button
            case R.id.action_add:

                // If there are less than 1 item in the list view, then this is a new database
                // Since the ProductEditor activity can not run without the suppliers table being
                // populated, first check if there are records in that table. If not, run the
                // seedSuppliers method first.
                if (mAdapter.getCount()<1) {
                    // New database. Check if Suppliers initialized yet.
                    if (!InventoryHelpers.suppliersExist(getApplicationContext())) {
                        // no suppliers in suppliers table. Seed suppliers.
                        InventoryHelpers.seedSuppliers(getApplicationContext());
                    }
                }
                Intent intent = new Intent(MainActivity.this, ProductEditor.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

