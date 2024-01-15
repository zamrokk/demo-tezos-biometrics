import {
  IonButton,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from "@ionic/react";
import { BiometryType, NativeBiometric } from "capacitor-native-biometric";
import "./Home.css";

const Home: React.FC = () => {
  const performBiometricVerification = async () => {
    const result = await NativeBiometric.isAvailable();

    if (!result.isAvailable) return;

    const isFaceID = result.biometryType == BiometryType.FACE_ID;

    try {
      await NativeBiometric.verifyIdentity({
        reason: "For easy log in",
        title: "Log in",
        subtitle: "Maybe add subtitle here?",
        description: "Maybe a description too?",
      });

      NativeBiometric.setCredentials({
        username: "username",
        password: "password",
        server: "www.example.com",
      });

      let credentials = await NativeBiometric.getCredentials({
        server: "www.example.com",
      });

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
            let credentials = await NativeBiometric.getCredentials({
              server: "www.example.com",
            });

            console.log("credentials", JSON.stringify(credentials));

            await NativeBiometric.deleteCredentials({
              server: "www.example.com",
            });

            credentials = await NativeBiometric.getCredentials({
              server: "www.example.com",
            });

            console.log("credentials AFTER", JSON.stringify(credentials));
          }}
        >
          deleteCredentials
        </IonButton>
      </IonContent>
    </IonPage>
  );
};

export default Home;
