# demo-tezos-biometrics

demo showcasing webauthn signing using secure enclave on Tezos

Add to AndroidManifest.xml

```xml

<application
        android:enableOnBackInvokedCallback="true"
/>


    <uses-permission android:name="android.permission.USE_BIOMETRIC"/>
```

to build.gradle

```gradle
    implementation 'com.madgag.spongycastle:core:1.58.0.0'
    implementation 'com.madgag.spongycastle:prov:1.58.0.0'
```
