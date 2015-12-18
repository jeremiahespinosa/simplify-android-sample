package com.simplify.android.sdk.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.BuyButtonAppearance;
import com.google.android.gms.wallet.fragment.BuyButtonText;
import com.google.android.gms.wallet.fragment.Dimension;
import com.google.android.gms.wallet.fragment.WalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.simplify.android.sdk.Card;
import com.simplify.android.sdk.CardEditor;
import com.simplify.android.sdk.CardToken;
import com.simplify.android.sdk.Simplify;

public class MainActivity extends AppCompatActivity implements Simplify.AndroidPayCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CURRENCY_CODE_USD = "USD";
    private static final String WALLET_FRAGMENT_ID = "wallet_fragment";

    private CardEditor mCardEditor;
    private Button mPayButton;
    private GoogleApiClient mGoogleApiClient;

    //---------------------------------------------
    // Life-Cycle
    //---------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register Android Pay callback
        Simplify.addAndroidPayCallback(this);
    }

    @Override
    protected void onStop() {
        // remove Android Pay callback
        Simplify.removeAndroidPayCallback(this);

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // to get back MaskedWallet using call back method.
        if (Simplify.handleAndroidPayResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    //---------------------------------------------
    // Android Pay callback methods
    //---------------------------------------------

    @Override
    public void onReceivedMaskedWallet(MaskedWallet maskedWallet) {
        // launch confirmation activity
        Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
        intent.putExtra(WalletConstants.EXTRA_MASKED_WALLET, maskedWallet);
        startActivity(intent);
    }

    @Override
    public void onReceivedFullWallet(FullWallet fullWallet) {

    }

    @Override
    public void onAndroidPayCancelled() {

    }

    @Override
    public void onAndroidPayError(int errorCode) {

    }


    //---------------------------------------------
    // Util
    //---------------------------------------------

    private void init() {

        mGoogleApiClient = ((SimplifyApplication)getApplication()).getGoogleApiClient(this);

        mPayButton = (Button) findViewById(R.id.btnPay);
        mPayButton.setEnabled(false);
        mPayButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCardToken();
            }
        });

        mCardEditor = (CardEditor) findViewById(R.id.card_editor);
        mCardEditor.addOnStateChangedListener(new CardEditor.OnStateChangedListener() {
            @Override
            public void onStateChange(CardEditor cardEditor) {
                mPayButton.setEnabled(cardEditor.isValid());
            }
        });

        //initializeAndroidPay();
        showGoogleBuyButton();
    }

    private void initializeAndroidPay() {

        System.out.println("initializeAndroidPay");
        Wallet.Payments.isReadyToPay(mGoogleApiClient).setResultCallback(
                new ResultCallback<BooleanResult>() {
                    @Override
                    public void onResult(@NonNull BooleanResult booleanResult) {
                        System.out.println("booleanResult.getValue() : + booleanResult.getValue()");
                        if (booleanResult.getStatus().isSuccess()) {
                            if (booleanResult.getValue()) {
                                showGoogleBuyButton();
                            } else {
                                hideGoogleBuyButton();
                            }
                        } else {
                            // Error making isReadyToPay call
                            Log.e(TAG, "isReadyToPay:" + booleanResult.getStatus());
                        }
                    }
                });
    }

    private void showGoogleBuyButton() {

        System.out.println("showGoogleBuyButton");
        findViewById(R.id.buy_button_layout).setVisibility(View.VISIBLE);

        // Define fragment style
        WalletFragmentStyle fragmentStyle = new WalletFragmentStyle()
                .setBuyButtonText(BuyButtonText.BUY_NOW)
                .setBuyButtonAppearance(BuyButtonAppearance.CLASSIC)
                .setBuyButtonWidth(Dimension.MATCH_PARENT);

        // Define fragment options
        WalletFragmentOptions fragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(WalletConstants.ENVIRONMENT_SANDBOX)
                .setFragmentStyle(fragmentStyle)
                .setTheme(WalletConstants.THEME_HOLO_LIGHT)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();

        // Create a new instance of WalletFragment
        WalletFragment walletFragment = WalletFragment.newInstance(fragmentOptions);

        // Initialize the fragment with start params
        // Note: If using the provided helper method Simplify.handleAndroidPayResult(int, int, Intent),
        //       you MUST set the request code to Simplify.REQUEST_CODE_MASKED_WALLET
        WalletFragmentInitParams startParams = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(getMaskedWalletRequest())
                .setMaskedWalletRequestCode(Simplify.REQUEST_CODE_MASKED_WALLET)
                .build();

        walletFragment.initialize(startParams);

        // Add Wallet fragment to the UI
        getFragmentManager().beginTransaction()
                .replace(R.id.buy_button_holder, walletFragment, WALLET_FRAGMENT_ID)
                .commit();

    }

    private void hideGoogleBuyButton() {
        System.out.println("hideGoogleBuyButton");
        findViewById(R.id.buy_button_layout).setVisibility(View.GONE);
    }

    private void requestCardToken() {

        mPayButton.setEnabled(false);

        Card card = mCardEditor.getCard();

        Simplify.createCardToken(card, new CardToken.Callback() {
            @Override
            public void onSuccess(CardToken cardToken) {
                mPayButton.setEnabled(true);

                Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
                i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_SUCCESS);
                startActivity(i);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();

                mPayButton.setEnabled(true);

                Intent i = new Intent(MainActivity.this, ThankYouActivity.class);
                i.putExtra(ThankYouActivity.EXTRA_PAGE, ThankYouActivity.PAGE_FAIL);
                startActivity(i);
            }
        });
    }


    private MaskedWalletRequest getMaskedWalletRequest() {

        return MaskedWalletRequest.newBuilder()
                .setMerchantName("Overpriced Coffee Shop")
                .setPhoneNumberRequired(true)
                .setShippingAddressRequired(true)
                .setCurrencyCode(CURRENCY_CODE_USD)
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(CURRENCY_CODE_USD)
                        .setTotalPrice("15.00")
                        .addLineItem(LineItem.newBuilder()
                                .setCurrencyCode(CURRENCY_CODE_USD)
                                .setDescription("Iced Coffee")
                                .setQuantity("1")
                                .setUnitPrice("15.00")
                                .setTotalPrice("15.00")
                                .build())
                        .build())
                .setEstimatedTotalPrice("5.00")
                .build();
    }

}
