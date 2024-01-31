package com.epicshaggy.biometric;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricPrompt;

import android.os.Handler;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;

import com.epicshaggy.biometric.capacitornativebiometric.R;

import org.spongycastle.jcajce.provider.digest.Blake2b;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AuthActivity extends AppCompatActivity {

    private Executor executor;
    private int maxAttempts;
    private int counter = 0;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_acitivy);

        maxAttempts = getIntent().getIntExtra("maxAttempts", 1);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            executor = this.getMainExecutor();
        }else{
            executor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    new Handler().post(command);
                }
            };
        }

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getIntent().hasExtra("title") ? getIntent().getStringExtra("title") : "Authenticate")
                .setSubtitle(getIntent().hasExtra("subtitle") ? getIntent().getStringExtra("subtitle") : null)
                .setDescription(getIntent().hasExtra("description") ? getIntent().getStringExtra("description") : null);

        boolean useFallback = getIntent().getBooleanExtra("useFallback", false);

        if(useFallback) {
            // TODO: Deprecated function, probably want to migrate to `setAllowedAuthenticators`
            builder.setDeviceCredentialAllowed(true);
        } else {
            // Note that this option is incompatible with device credential authentication and must NOT be set if the latter is enabled via `setAllowedAuthenticators` or `setDeviceCredentialAllowed`.
            // @see https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setNegativeButtonText(java.lang.CharSequence)
            builder.setNegativeButtonText(getIntent().hasExtra("negativeButtonText") ? getIntent().getStringExtra("negativeButtonText") : "Cancel");
        }

        BiometricPrompt.PromptInfo promptInfo = builder.build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {

                Log.w("onAuthenticationError",errorCode+" - "+errString);

                super.onAuthenticationError(errorCode, errString);
                int pluginErrorCode = AuthActivity.convertToPluginErrorCode(errorCode);
                finishActivity("error", pluginErrorCode, errString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {

                Log.w("onAuthenticationSucceed",result.getCryptoObject()+"");


                String payload = getIntent().getStringExtra("payload");
                Log.i("PAYLOAD",payload);

                //BLAKE2B HASH the payload ...
                Blake2b.Blake2b256 b2b = new Blake2b.Blake2b256();
                b2b.update(payload.getBytes());

                byte[] hashed = b2b.digest();
                Log.i("HASHED",hashed.toString());


                //encrypt the hash
                byte[] signatureBytes = new byte[0];
                try {
                    signatureBytes = result.getCryptoObject().getCipher().doFinal(


                            hashed);
                } catch (BadPaddingException e) {
                    throw new RuntimeException(e);
                } catch (IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }

                //get signature
                String signature = Arrays.toString(signatureBytes);

                Log.d("signature", signature
                        );




                super.onAuthenticationSucceeded(result);



                finishActivity("success",0,signature);
            }

            @Override
            public void onAuthenticationFailed() {


                Log.w("onAuthenticationFailed","");


                super.onAuthenticationFailed();
                counter++;
                if(counter == maxAttempts)
                    finishActivity("failed", 10, "Authentication failed.");
            }
        });

        KeyStore ks = null;
        Cipher cipher = null;

        try {

            ks = KeyStore.getInstance(ANDROID_KEY_STORE);
            ks.load(null);

            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_EC,ANDROID_KEY_STORE);
            Key key = ks.getKey("TEZOS",null);
            cipher.init(Cipher.ENCRYPT_MODE, key);

        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableEntryException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        biometricPrompt.authenticate(promptInfo,new BiometricPrompt.CryptoObject(cipher));

    }




    void finishActivity(String result,String signature) {
        finishActivity(result, null, null, signature);
    }

    void finishActivity(String result, Integer errorCode, String errorDetails) {
        finishActivity(result, errorCode, errorDetails, null);
    }

    void finishActivity(String result, Integer errorCode, String errorDetails,String signature) {


        Log.w("finishActivity",result);

        Intent intent = new Intent();
        intent.putExtra("result", result);
        if (errorCode != null) {
            intent.putExtra("errorCode", String.valueOf(errorCode));
        }
        if (errorDetails != null) {
            intent.putExtra("errorDetails", errorDetails);
        }
        if (signature != null) {
            intent.putExtra("signature", signature);
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Convert Auth Error Codes to plugin expected Biometric Auth Errors (in README.md)
     * This way both iOS and Android return the same error codes for the same authentication failure reasons.
     * !!IMPORTANT!!: Whenever this is modified, check if similar function in iOS Plugin.swift needs to be modified as well
     * @see https://developer.android.com/reference/androidx/biometric/BiometricPrompt#constants
     * @return BiometricAuthError
     */
    public static int convertToPluginErrorCode(int errorCode) {
        switch (errorCode) {
            case BiometricConstants.ERROR_HW_UNAVAILABLE:
            case BiometricConstants.ERROR_HW_NOT_PRESENT:
                return 1;
            case BiometricConstants.ERROR_LOCKOUT_PERMANENT:
                return 2;
            case BiometricConstants.ERROR_NO_BIOMETRICS:
                return 3;
            case BiometricConstants.ERROR_LOCKOUT:
                return 4;
            // Authentication Failure (10) Handled by `onAuthenticationFailed`.
            // App Cancel (11), Invalid Context (12), and Not Interactive (13) are not valid error codes for Android.
            case BiometricConstants.ERROR_NO_DEVICE_CREDENTIAL:
                return 14;
            case BiometricConstants.ERROR_TIMEOUT:
            case BiometricConstants.ERROR_CANCELED:
                return 15;
            case BiometricConstants.ERROR_USER_CANCELED:
            case BiometricConstants.ERROR_NEGATIVE_BUTTON:
                return 16;
            default:
                return 0;
        }
    }

}
