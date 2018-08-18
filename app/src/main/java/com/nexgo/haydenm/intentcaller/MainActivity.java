package com.nexgo.haydenm.intentcaller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    // Buttons and text inputs from the layout
    private Button intent_sale_go_button;
    private Button intent_generate_trans_key;
    private EditText intent_sale_amount_input;
    private EditText intent_tip_amount_input;

    // JSON Objects that will be packed into an Intent & sent to the Integrator
    private JSONObject Input;
    private JSONObject Action;
    private JSONObject Payment;
    private JSONObject Host;

    // Merchant parameters
    private String pMerchantID        = "";
    private String pDeveloperID       = "";
    private String pDeviceID          = "";
    private String pUserID            = "";
    private String pPassword          = "";
    private String pTransactionKey    = "";
    private String pSaleType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent_sale_go_button       =   (Button)    findViewById(R.id.intent_sale_go_button);

        // Set the button DISABLED so that a Credit Sale cannot be run before getting the TransactionKey!
        intent_sale_go_button.setEnabled(false);
        intent_sale_go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("GoButtonClicked", "GO pressed...Building intent..");
                if (buildCreditSaleJSONMessage())
                {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.Integrator");   //The listener name of Integrator
                    intent.putExtra("Input", Input.toString());      //Pack the Input JSONObject into the intent
                    startActivityForResult(intent, 1); //todo list requestcodes
                }
                else
                {
                    Log.e("GoButtonClicked", "buildCreditSaleJSONMessage() failed!");
                }
            }
        });
        intent_sale_amount_input    =   (EditText) findViewById(R.id.intent_sale_amount_input);
        intent_tip_amount_input     =   (EditText)  findViewById(R.id.intent_tip_amount_input);
        intent_generate_trans_key   =   (Button)    findViewById(R.id.intent_generate_trans_key_button);
        intent_generate_trans_key.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("GenTransButtonClicked", "GO pressed...Building intent..");

                //Check if TransKey is already generated...
                if (pTransactionKey.compareTo("") != 0) //if TransKey isn't empty
                {
                    //If TransactionKey variable is not "", then a key is already available,
                    // and we should prompt the user if they want to regenerate it anyways.
                    new AlertDialog.Builder(MainActivity.this)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    System.out.println("YES --> Clicked GEN AGAIN");

                                    if (buildGenerateKeyJSONMessage())
                                    {
                                        Intent intent = new Intent();
                                        intent.setAction("android.intent.action.Integrator");
                                        intent.putExtra("Input", Input.toString());
                                        startActivityForResult(intent, 1);
                                    }
                                    else
                                    {
                                        Log.e("GenTransButtonClicked", "buildGenerateKeyJSONMessage() failed!");
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    System.out.println("NO --> Clicked CANCEL / NO GEN AGAIN");
                                }
                            })
                            .setTitle("Key Already Generated!")
                            .setMessage("Warning! \nYou have already generated the TransactionKey. \n" +
                                    "\nDo you want to Generate it again?")
                            .show();
                }
                else
                {
                    if (buildGenerateKeyJSONMessage())
                    {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.Integrator");
                        intent.putExtra("Input", Input.toString());
                        startActivityForResult(intent, 1);
                    }
                    else
                    {
                        Log.e("GenTransButtonClicked", "buildGenerateKeyJSONMessage() failed!");
                    }
                }


            }
        });
    }

    /**
     * Builds the JSON Message that will be put in an Intent and sent to the Exadigm Integrator to
     * initiate a Credit Sale with our desired amount. If the JSON message is able to be built
     * without any issues, it will return true, otherwise false.
     * @return true if json message is able to be built correctly, otherwise returns false.
     */
    public boolean buildCreditSaleJSONMessage()
    {
        //Initialiate JSONObjects that will be packed into the Intent sent to Integrator.
        Input   = new JSONObject();
        Action  = new JSONObject();
        Payment = new JSONObject();
        Host    = new JSONObject();


        //Action Parameters
        boolean pSignature        =  false; //Require on-screen signature
        boolean pReceipt          =  true; //Pring a receipt
        boolean pProcessorReceipt =  false; //Print a processor receipt
        boolean pCamera           =  false;
        boolean pManual           =  true;  //Allow manual/keyed entry (as opposed to SWIPE/EMV/TAP)
        boolean pQuickChip        =  true;  //Enable QuickChip (EMV)
        String pProcessor         =  "TSYS";

        try {
            //Put the required additional parameters for a 'Credit Sale' into the Action JSONObject
            Action.put("signature", pSignature);
            Action.put("receipt", pReceipt);
            Action.put("processor_receipt", pProcessorReceipt);
            Action.put("camera", pCamera);
            Action.put("manual", pManual);
            Action.put("quick_chip", pQuickChip);
            Action.put("processor", pProcessor);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [ACTION] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Payment Parameters
        pSaleType         =  "Sale"; //'Sale' = Credit Sale
        if (intent_sale_amount_input.getText().toString().equalsIgnoreCase(""))
        {
            Log.e("buildSaleJSONMessage()", "Amount input was blank..");
            return false;
        }
        //Get the Sale parameters (amounts $$) entered by the user
        String  pSaleAmount       =  intent_sale_amount_input.getText().toString();
        String  pTipAmount        =  intent_tip_amount_input.getText().toString();
        String  pCashbackAmount   =  "0.00";

        try {
            //Put the required payment fields for a 'Credit Sale' into the Payment JSONObject
            Payment.put("type", pSaleType);
            Payment.put("amount", pSaleAmount);
            Payment.put("tip_amount", pTipAmount);
            Payment.put("cash_back", pCashbackAmount);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [PAYMENT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }


        try {
            //Put the required host parameters for a 'Credit Sale' into the Host JSON Object
            Host.put("ts_developerid", pDeveloperID);
            Host.put("ts_deviceid", pDeviceID);
            Host.put("ts_transactionkey", pTransactionKey);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [HOST] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Input Parameters (sent with Intent)
        try {
            //Pack the various JSONObject (Action, Payment, Host) Strings into the Intent to send to Integrator app
            Input.put("action", Action);
            Input.put("payment", Payment);
            Input.put("host", Host);
            System.out.println("Params:" + Input.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [INPUT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Builds the JSON Message that will be put in an Intent and sent to the Exadigm Integrator to
     * generate a TransactionKey. This function does not actually make the call to get the TransactionKey,
     * but rather builds the request message that will be put in the intent.
     * If the JSON message is able to be built without any issues, it will return true, otherwise false.
     * @return true if json message is able to be built correctly, otherwise returns false.
     */
    public boolean buildGenerateKeyJSONMessage()
    {
        Input   = new JSONObject();
        Action  = new JSONObject();
        Payment = new JSONObject();
        Host    = new JSONObject();

        try {
            Action.put("signature", "false");
            Action.put("receipt", "false");
            Action.put("manual", "false");
            Action.put("processor", "TSYS");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [ACTION] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            pSaleType = "Generate Key";
            Payment.put("type", pSaleType);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [PAYMENT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Host.put("ts_developerid", pDeveloperID);
            Host.put("ts_deviceid", pDeviceID);
            Host.put("ts_transactionkey", pTransactionKey);
            Host.put("ts_mid", pMerchantID);
            Host.put("ts_userid", pUserID);
            Host.put("ts_password", pPassword);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [HOST] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }
        //Input Parameters (sent with Intent)
        try {
            Input.put("action", Action);
            Input.put("payment", Payment);
            Input.put("host", Host);
            System.out.println("Params:  " + Input.toString());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error building [INPUT] JSON..", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * The onActivityResult(X,X,X) function is called after the Integrator Intent has run
     * and the result is returned to the IntentCaller application.
     *
     * This is the function where we can handle operations after an Integrator transaction has
     * finished, like getting the 'Signature' image, parsing a TransactionKey, or just for checking
     * the raw response from the Integrator (including the raw HOST RESPONSE)
     * @param requestCode
     * @param resultCode
     * @param data the data returned from Integrator, containing results and other messages/objs
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("leo", data.getStringExtra("transdata"));
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {

                String response = data.getStringExtra("transdata");
                System.out.println("RESPONSE ====> " + response);

                if (pSaleType.equalsIgnoreCase("Generate Key")) {
                    try {
                        System.out.println("*****************************************************");
                        JSONObject transkeyjson = new JSONObject(response);
                        String trans_data = transkeyjson.getString("packetData");
                        System.out.println("PACKET DATA =====>  " + trans_data);
                        System.out.println("*****************************************************");

                        System.out.println("*****************************************************");
                        JSONObject host_response = new JSONObject(trans_data);
                        String host_data = host_response.getString("hostResponse");
                        System.out.println("Host Response ====> " + host_response);
                        System.out.println("*****************************************************");

                        System.out.println("*****************************************************");
                        JSONObject genkey_repsonse = new JSONObject(host_data);
                        String genkey_data = genkey_repsonse.getString("GenerateKeyResponse");
                        System.out.println("GenerateKeyResponse ====> " + genkey_repsonse.getString("GenerateKeyResponse"));
                        System.out.println("*****************************************************");

                        System.out.println("*****************************************************");
                        JSONObject transkey_repsonse = new JSONObject(genkey_data);
                        String transkey_data = transkey_repsonse.getString("transactionKey");
                        System.out.println("Transaction Key ====> " + transkey_data);
                        pTransactionKey = transkey_repsonse.getString("transactionKey");
                        intent_sale_go_button.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "TRANS_KEY: " + pTransactionKey, Toast.LENGTH_LONG).show();
                        System.out.println("*****************************************************");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                new AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setTitle("Info")
                        .setMessage(data.getStringExtra("transdata"))
                        .show();

            }
        }
    }
}
