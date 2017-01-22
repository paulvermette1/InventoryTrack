package com.example.android.inventorytrack;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;

import static com.example.android.inventorytrack.ProductEditor.ORDERING_DIALOG_FRAGMENT;
import static com.example.android.inventorytrack.ProductEditor.RECEIVING_DIALOG_FRAGMENT;
import static com.example.android.inventorytrack.ProductEditor.SELLING_DIALOG_FRAGMENT;

/**
 * Created by paulvermette on 2016-12-28.
 */

public class ProductQuantityDialog extends DialogFragment {

    private static final String LOG_TAG = ProductQuantityDialog.class.getSimpleName();
    // Use this instance of the interface to deliver action events
    OrderProductDialogListener mListener;
    private String dialogPrompt;
    private String positiveButtonLabel;
    private EditText editQuantity;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (OrderProductDialogListener) context;

        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Determine the purpose of the dialog by getting the arguments
        Bundle argsBundle = this.getArguments();
        int dialogPurpose = argsBundle.getInt("dialogPurpose");

        switch (dialogPurpose) {
            case ORDERING_DIALOG_FRAGMENT:
                dialogPrompt = getString(R.string.order_dialog_prompt);
                positiveButtonLabel = getString(R.string.order_button);
                break;
            case RECEIVING_DIALOG_FRAGMENT:
                dialogPrompt = getString(R.string.receive_dialog_prompt);
                positiveButtonLabel = getString(R.string.receive);
                break;
            case SELLING_DIALOG_FRAGMENT:
                dialogPrompt = getString(R.string.sale_dialog_prompt);
                positiveButtonLabel = getString(R.string.sale);
                break;
            default:
                Log.e(LOG_TAG, getString(R.string.dialog_purpose_not_found));
        }


        // Inflate and set the layout for the dialog
        builder.setView(inflater.inflate(R.layout.product_dialog, null))
                // Add action buttons
                .setMessage(dialogPrompt)
                .setPositiveButton(positiveButtonLabel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        mListener.onDialogPositiveClick(ProductQuantityDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(ProductQuantityDialog.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface OrderProductDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);

        public void onDialogNegativeClick(DialogFragment dialog);
    }
}
