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

Refresh lib

```
npm i capacitor-native-biometric@git+https:github.com/zamrokk/capacitor-native-biometric.git
```

Compile

```
ionic capacitor update android
ionic capacitor sync android
git add .
git commit -m "."
git push
```
