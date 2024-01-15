import {
  IonButton,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from "@ionic/react";
import {
  BiometryType,
  Credentials,
  NativeBiometric,
} from "capacitor-native-biometric";
import { useState } from "react";
import "./Home.css";

const Home: React.FC = () => {
  const [credentials, setCredentials] = useState<Credentials>({
    username: "",
    password: "",
  });

  const performBiometricVerification = async () => {
    const result = await NativeBiometric.isAvailable();

    console.log("isAvailable", result);

    if (!result.isAvailable) return;

    const isFaceID = result.biometryType == BiometryType.FACE_ID;

    try {
      await NativeBiometric.verifyIdentity({
        reason: "For easy log in",
        title: "Log in",
        subtitle: "Maybe add subtitle here?",
        description: "Maybe a description too?",
        useFallback: true,
      });

      NativeBiometric.setCredentials({
        username: "username",
        password: "password",
        server: "www.example.com",
      });

      setCredentials(
        await NativeBiometric.getCredentials({
          server: "www.example.com",
        })
      );

      console.log("credentials AFTER", JSON.stringify(credentials));
    } catch (error) {
      console.log("Biometrics failed");
      return;
    }
  };

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Blank</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent fullscreen>
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">Blank</IonTitle>
          </IonToolbar>
        </IonHeader>

        <IonButton onClick={performBiometricVerification}>
          performBiometricVerification
        </IonButton>

        <IonButton
          onClick={async () => {
            await NativeBiometric.deleteCredentials({
              server: "www.example.com",
            });

            try {
              await NativeBiometric.getCredentials({
                server: "www.example.com",
              });
            } catch (error) {
              setCredentials({
                password: "",
                username: "",
              });
            }
          }}
        >
          deleteCredentials
        </IonButton>

        <div>Credentials :</div>
        <div>username : {credentials.username}</div>
        <div>password : {credentials.password}</div>
      </IonContent>
    </IonPage>
  );
};

export default Home;
