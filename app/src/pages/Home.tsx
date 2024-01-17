import {
  IonButton,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from "@ionic/react";
import { InMemorySigner } from "@taquito/signer";
import { TezosToolkit } from "@taquito/taquito";
import { Prefix, b58cencode, prefix } from "@taquito/utils";
import { Credentials, NativeBiometric } from "capacitor-native-biometric";
import * as crypto from "crypto";
import { useState } from "react";
import "./Home.css";

const Home: React.FC = () => {
  const Tezos = new TezosToolkit("https://ghostnet.tezos.marigold.dev");

  const [credentials, setCredentials] = useState<Credentials>({
    username: "",
    password: "",
  });

  const [userBalance, setUserBalance] = useState<number>(0);

  const [signer, setSigner] = useState<InMemorySigner>();

  const generateCredentials = async () => {
    const keyBytes = Buffer.alloc(32);
    crypto.randomFillSync(keyBytes);

    console.log("keyBytes", keyBytes.toString());

    console.log("prefix[Prefix.EDSK]", prefix[Prefix.EDSK].toString());

    const key =
      Prefix.EDSK + b58cencode(new Uint8Array(keyBytes), prefix[Prefix.EDSK]);

    console.log("key", key);

    const signer = new InMemorySigner(key);
    setSigner(signer);
    const pkh = await signer.publicKeyHash();

    const userBalance = await Tezos.tz.getBalance(pkh);
    setUserBalance(userBalance.toNumber());

    setCredentials({
      username: pkh,
      password: key,
    });
  };

  const saveEncryptedKeyPairWithBiometrics = async () => {
    const result = await NativeBiometric.isAvailable();

    console.log("NativeBiometric.isAvailable", result);

    if (!result.isAvailable) return;

    try {
      await NativeBiometric.verifyIdentity({
        reason: "It is required to access to encrypted data on Keystore",
        title: "Log in",
        subtitle: "(required)",
        description:
          "Biometric step to be able to store encrypted keypair on Keystore and decrypt it",
      });

      NativeBiometric.setCredentials({
        username: credentials.username,
        password: credentials.password,
        server: "TEZOS",
      });

      console.log("Successfully store encrypted Keypair");
    } catch (error) {
      console.log("Biometrics failed");
      return;
    }
  };

  const transfer = async () => {
    try {
      const op = await Tezos.contract.transfer({
        to: "tz1VSUr8wwNhLAzempoch5d6hLRiTh8Cjcjb",
        amount: 1,
        mutez: true,
      });
      console.log("Transfer sent to alice");
      const opHash = await op.confirmation(1);
      console.log("Confirmed go to https://ghostnet.tzkt.io/", opHash);
    } catch (error) {
      console.error("Error", error);
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
        <IonButton onClick={generateCredentials}>
          Generate TEZOS keypair
        </IonButton>
        <IonButton onClick={saveEncryptedKeyPairWithBiometrics}>
          Save keypair with Biometrics
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
          Remove keypair
        </IonButton>
        <div>Keypair :</div>
        <div>PKH : {credentials.username}</div>
        <div>PrivKey : {credentials.password}</div>
        <div>
          Balance : {userBalance}
          {"mutez "}
          <IonButton
            onClick={async () =>
              setUserBalance(
                (await Tezos.tz.getBalance(credentials.username)).toNumber()
              )
            }
          >
            Refresh balance
          </IonButton>
        </div>
        <IonButton onClick={transfer}>Send money to alice</IonButton>
      </IonContent>
    </IonPage>
  );
};

export default Home;
