package com.epicshaggy.biometric;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.annotation.SuppressLint;
import android.util.Log;

import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.digests.Blake2bDigest;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jcajce.provider.digest.Blake2b;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveGenParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.math.ec.ECPoint;


@CapacitorPlugin(name = "NativeBiometric")
public class NativeBiometric extends Plugin {

    private static BouncyCastleProvider bouncyCastleProvider;
    public static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
    static {
        bouncyCastleProvider = BOUNCY_CASTLE_PROVIDER;
    }

    //protected final static int AUTH_CODE = 0102;

    private static final int NONE = 0;
    private static final int FINGERPRINT = 3;
    private static final int FACE_AUTHENTICATION = 4;
    private static final int IRIS_AUTHENTICATION = 5;
    private static final int MULTIPLE = 6;


    private KeyStore keyStore;
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String AES_MODE = "AES/ECB/PKCS7Padding";
    private static final byte[] FIXED_IV = new byte[12];
    private static final String ENCRYPTED_KEY = "NativeBiometricKey";
    private static final String NATIVE_BIOMETRIC_SHARED_PREFERENCES = "NativeBiometricSharedPreferences";

    private SharedPreferences encryptedSharedPreferences;

    private int getAvailableFeature() {
        // default to none
        int type = NONE;

        // if has fingerprint
        if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            type = FINGERPRINT;
        }

        // if has face auth
        if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            // if also has fingerprint
            if (type != NONE)
                return MULTIPLE;

            type = FACE_AUTHENTICATION;
        }

        // if has iris auth
        if (getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_IRIS)) {
            // if also has fingerprint or face auth
            if (type != NONE)
                return MULTIPLE;

            type = IRIS_AUTHENTICATION;
        }

        return type;
    }

    @PluginMethod()
    public void isAvailable(PluginCall call) {
        JSObject ret = new JSObject();

        boolean useFallback = Boolean.TRUE.equals(call.getBoolean("useFallback", false));

        BiometricManager biometricManager = BiometricManager.from(getContext());
        int canAuthenticateResult = biometricManager.canAuthenticate();
        // Using deviceHasCredentials instead of canAuthenticate(DEVICE_CREDENTIAL)
        // > "Developers that wish to check for the presence of a PIN, pattern, or password on these versions should instead use isDeviceSecure."
        // @see https://developer.android.com/reference/androidx/biometric/BiometricManager#canAuthenticate(int)

        Log.w("canAuthenticateResult",canAuthenticateResult+"");
        Log.w("deviceHasCredentials",this.deviceHasCredentials()+"");
        Log.w("useFallback",useFallback+"");

        boolean fallbackAvailable = useFallback && this.deviceHasCredentials();
        if (useFallback && !fallbackAvailable) {
            canAuthenticateResult = BiometricConstants.ERROR_NO_DEVICE_CREDENTIAL;
        }

        boolean isAvailable = (canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS || fallbackAvailable);
        ret.put("isAvailable", isAvailable);

        Log.w("isAvailable",isAvailable+"");

        if (!isAvailable) {
            // BiometricManager Error Constants use the same values as BiometricPrompt's Constants. So we can reuse our
            int pluginErrorCode = AuthActivity.convertToPluginErrorCode(canAuthenticateResult);
            ret.put("errorCode", pluginErrorCode);
            Log.w("errorCode",pluginErrorCode+"");

        }

        ret.put("biometryType", getAvailableFeature());
        call.resolve(ret);
    }




    @PluginMethod()
    public void sign(final PluginCall call) {
        Intent intent = new Intent(getContext(), AuthActivity.class);

        intent.putExtra("title", call.getString("title", "Authenticate"));

        if (call.hasOption("subtitle")) {
            intent.putExtra("subtitle", call.getString("subtitle"));
        }

        if (call.hasOption("description")) {
            intent.putExtra("description", call.getString("description"));
        }

        if (call.hasOption("negativeButtonText")) {
            intent.putExtra("negativeButtonText", call.getString("negativeButtonText"));
        }

        if (call.hasOption("maxAttempts")) {
            intent.putExtra("maxAttempts", call.getInt("maxAttempts"));
        }

        boolean useFallback = Boolean.TRUE.equals(call.getBoolean("useFallback", false));
        if (useFallback) {
            useFallback = this.deviceHasCredentials();
        }

        intent.putExtra("useFallback", useFallback);

        startActivityForResult(call, intent, "verifyResult");
    }

    /*
    @PluginMethod()
    public void setCredentials(final PluginCall call) {
        String username = call.getString("username", null);
        String password = call.getString("password", null);
        String KEY_ALIAS = call.getString("server", null);

        if (username != null && password != null && KEY_ALIAS != null) {
            try {
                SharedPreferences.Editor editor = getContext().getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE).edit();
                editor.putString(KEY_ALIAS + "-username", encryptString(username, KEY_ALIAS));
                editor.putString(KEY_ALIAS + "-password", encryptString(password, KEY_ALIAS));
                editor.apply();
                call.resolve();
            } catch (GeneralSecurityException | IOException e) {
                call.reject("Failed to save credentials", e);
                e.printStackTrace();
            }
        } else {
            call.reject("Missing properties");
        }
    }*/

    @PluginMethod()
    public void getPublicKey(final PluginCall call) {

        SharedPreferences sharedPreferences = getContext().getSharedPreferences(NATIVE_BIOMETRIC_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        String publicKey = sharedPreferences.getString( "TEZOS-publickey", null);
            if (publicKey != null) {
                try {
                    JSObject jsObject = new JSObject();
                    jsObject.put("publicKey", publicKey);
                    call.resolve(jsObject);
                } catch (Exception e) {
                    // Can get here if not authenticated.
                    String errorMessage = "Failed to get publicKey";
                    call.reject(errorMessage);
                }
            } else {
                call.reject("No publicKey found");
            }
    }

    @ActivityCallback
    private void verifyResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.hasExtra("result")) {
                switch (data.getStringExtra("result")) {
                    case "success":
                        call.resolve();
                        break;
                    case "failed":
                    case "error":
                        call.reject(data.getStringExtra("errorDetails"), data.getStringExtra("errorCode"));
                        break;
                    default:
                        // Should not get to here unless AuthActivity starts returning different Activity Results.
                        call.reject("Something went wrong.");
                        break;
                }
            }
        } else {
            call.reject("Something went wrong.");
        }
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }

    private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;


    @PluginMethod()
    public void init(final PluginCall call) {
        KeyPair key = null;
        try {
            key = init( );
        } catch (
         GeneralSecurityException | IOException e){
                call.reject("INIT : Cannot generate the keys : "+e.toString());
        }



        if(key != null) {
            JSObject ret = new JSObject();


            try {
                ECPublicKeyParameters bcecPublicKey =(ECPublicKeyParameters) ECUtil.generatePublicKeyParameter(key.getPublic());
                Log.i("PUBLIC COMP KEY LENGHT",  bcecPublicKey.getQ().getEncoded(true).length + "");
                Log.i("PUBLIC COMP HEX",  hex(bcecPublicKey.getQ().getEncoded(true)) + "");

                ret.put("value", hex(bcecPublicKey.getQ().getEncoded(true)));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Blake2b.Blake2b256 b2b = new Blake2b.Blake2b256();
            b2b.update(hex(key.getPublic().getEncoded()).getBytes());

            Log.i("HASH of the key",hex(b2b.digest()));


            KeyFactory keyFactory = null;
            try {
                keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (NoSuchProviderException e) {
                throw new RuntimeException(e);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    KeyInfo keyInfo = (KeyInfo) keyFactory.getKeySpec(key.getPrivate(), KeyInfo.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Log.i("KEY getSecurityLevel ?", keyInfo.getSecurityLevel()+"");
                    }

                    Log.i("KEY isInsideSecureHardware ?", keyInfo.isInsideSecureHardware()+"");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Log.i("KEY isInvalidatedByBiometricEnrollment ?", keyInfo.isInvalidatedByBiometricEnrollment()+"");
                    }

                    Log.i("KEY isUserAuthenticationRequired ?", keyInfo.isUserAuthenticationRequired()+"");

                    Log.i("KEY isUserAuthenticationRequirementEnforcedBySecureHardware ?", keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware()+"");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Log.i("KEY isUserConfirmationRequired ?", keyInfo.isUserConfirmationRequired()+"");
                    }


                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
            call.resolve(ret);
        }else {
            call.reject("INIT : Cannot generate the keys ");
        }
    }


    private KeyPair init() throws GeneralSecurityException, StrongBoxUnavailableException, IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i("Build.VERSION", "Build.VERSION.SDK_INT >= Build.VERSION_CODES.M");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC
                    , ANDROID_KEY_STORE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                kpg.initialize(

                        new KeyGenParameterSpec.Builder(
                        "TEZOS",
                        KeyProperties.PURPOSE_ENCRYPT  )
                                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))

                                .setIsStrongBoxBacked(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE))

                                .setUnlockedDeviceRequired(true)
                                .setInvalidatedByBiometricEnrollment(true)
                                .setUserAuthenticationRequired(true)

                        .build());


            }

            return kpg.generateKeyPair();

        }else{
            Log.e("ANDROID_VERSION","Too old");
                    return null;
        }
    }






    private boolean deviceHasCredentials() {
        KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        // Can only use fallback if the device has a pin/pattern/password lockscreen.
        return keyguardManager.isDeviceSecure();
    }
}
