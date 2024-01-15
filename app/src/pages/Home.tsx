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

    const verified = await NativeBiometric.verifyIdentity({
      reason: "For easy log in",
      title: "Log in",
      subtitle: "Maybe add subtitle here?",
      description: "Maybe a description too?",
    })
      .then(() => true)
      .catch(() => false);

    if (!verified) return;

    const credentials = await NativeBiometric.getCredentials({
      server: "www.example.com",
    });

    alert("credentials" + JSON.stringify(credentials));
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

        <IonButton
          onClick={() =>
            NativeBiometric.setCredentials({
              username: "username",
              password: "password",
              server: "www.example.com",
            })
          }
        >
          setCredentials
        </IonButton>

        <IonButton onClick={performBiometricVerification}>
          performBiometricVerification
        </IonButton>

        <IonButton
          onClick={() =>
            NativeBiometric.deleteCredentials({
              server: "www.example.com",
            })
          }
        >
          deleteCredentials
        </IonButton>
      </IonContent>
    </IonPage>
  );
};

export default Home;
