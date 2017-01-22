package com.example.android.inventorytrack;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventorytrack.data.InventoryContract;
import com.example.android.inventorytrack.data.ProductProvider;

/**
 * Created by paulvermette on 2016-12-19.
 */

public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new ProductCursorAdapter.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {


        // Find fields to populate in inflated template
        TextView tvName = (TextView) view.findViewById(R.id.name);
        TextView tvSummary = (TextView) view.findViewById(R.id.summary);
        // Extract properties from cursor
        String name = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME));
        int qty = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY));
        int price = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE));

        // Populate fields with extracted properties
        String summary = context.getString(R.string.summary_part1_prefix) + " " + qty + "    " + context.getString(R.string.summary_part2_prefix) + InventoryHelpers.IntPriceToString(price);
        tvName.setText(name);
        tvSummary.setText(summary);


        // Bind / set the Tag of the clickable view objects with the _ID of the current row.
        // This will alow the on Click Listener to reference this data element from the row clicked on
        // and perform the appropriate action on the Product record referenced.
        int productId = cursor.getInt(cursor.getColumnIndexOrThrow((InventoryContract.ProductEntry._ID)));

        ImageView cartIcon = (ImageView) view.findViewById(R.id.sale);
        cartIcon.setTag(productId);

        RelativeLayout clickZone = (RelativeLayout) view.findViewById(R.id.product_click_zone);
        clickZone.setTag(productId);

        // Set up the two click listeners
        clickZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Form the content URI which represents the specific product clicked on
                // using the Product _ID set in the tag
                int thisProductId = (int) view.getTag();
                Uri productUri = ContentUris.withAppendedId(InventoryContract.ProductEntry.CONTENT_URI, thisProductId);

                // Create intent to launch the new activity
                Intent editProductIntent = new Intent(context, ProductEditor.class);
                editProductIntent.setData(productUri);
                context.startActivity(editProductIntent);
            }
        });


        cartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int thisProductId = (int) v.getTag();
                // Call the ProductSale method to take care of the request to sell one item
                int saleResult = ProductProvider.ProductSale(context, thisProductId, 1);

                if (saleResult == ProductProvider.SALE_SUCCESS) {
                    Toast.makeText(context, context.getString(R.string.sale_completed), Toast.LENGTH_SHORT).show();
                } else if (saleResult == ProductProvider.SALE_ERROR_NO_STOCK) {
                    Toast.makeText(context, context.getString(R.string.sale_warning_not_enough_stock), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.sale_general_failure), Toast.LENGTH_SHORT).show();
                }
            }

        });
    }

}
