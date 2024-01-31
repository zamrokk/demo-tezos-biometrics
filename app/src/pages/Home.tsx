import {
  IonButton,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
} from "@ionic/react";

import { TezosToolkit } from "@taquito/taquito";
import { AvailableResult, NativeBiometric } from "capacitor-native-biometric";
import { useEffect, useState } from "react";
import { BiometricsSigner } from "../taquito-biometrics-signer";
import "./Home.css";

const Home: React.FC = () => {
  const [Tezos, setTezos] = useState<TezosToolkit>(
    new TezosToolkit("https://ghostnet.tezos.marigold.dev")
  );

  const [publicKey, setPublicKey] = useState<string | undefined>();
  const [publicKeyHash, setPublicKeyHash] = useState<string | undefined>();

  const [userBalance, setUserBalance] = useState<number>(0);

  const [signer, setSigner] = useState<BiometricsSigner>();

  useEffect(() => {
    (async () => {
      if (publicKey) setPublicKeyHash(await signer?.publicKeyHash());
    })();
  }, [publicKey]);

  useEffect(() => {
    if (!signer)
      (async () => {
        const result: AvailableResult = await NativeBiometric.isAvailable();

        console.log("NativeBiometric.isAvailable", result);

        if (!result.isAvailable) {
          console.error("Biometrics are not available on this phone");
          return;
        }

        try {
          if (!publicKey) {
            try {
              let { publicKey } = await NativeBiometric.getPublicKey();
              setPublicKey(publicKey);
              console.log("Public key : ", publicKey);
            } catch (error) {
              console.error(
                "Public key is not initialized, need to initialize the account on the device the first time",
                error
              );
              let { publicKey } = await NativeBiometric.init();
              setPublicKey(publicKey);
              console.log("Public key : ", publicKey);
            }
          }
          const signer = new BiometricsSigner(publicKey!);
          setSigner(signer);
        } catch (error) {
          console.error("Error on initializing the signer", error);
        }
      })();
  }, []);

  const transfer = async () => {
    try {
      console.log("Tezos.signer", Tezos.signer);

      const op = await Tezos.contract.transfer({
        to: "tz1VSUr8wwNhLAzempoch5d6hLRiTh8Cjcjb",
        amount: 1,
        mutez: true,
      });
      console.log("Transfer sent to alice");
      const opHash = await op.confirmation(1);
      console.log("Confirmed go to https://ghostnet.tzkt.io/" + opHash);
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

        <div>Keypair :</div>
        <div>PK : {publicKeyHash}</div>
        <div>PK : {publicKey}</div>
        <div>PrivKey : INSIDE OF YOUR PHONE SECURE ENCLAVE ...</div>
        <div>
          Balance : {userBalance}
          {"mutez "}
          <IonButton
            onClick={async () =>
              publicKeyHash
                ? setUserBalance(
                    (await Tezos.tz.getBalance(publicKeyHash)).toNumber()
                  )
                : () => {
                    console.warn("Initialize the public key first ...");
                  }
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
