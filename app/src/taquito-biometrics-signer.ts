/**
 * @packageDocumentation
 * @module @taquito/remote-signer
 */
import {
  InvalidPublicKeyError,
  InvalidSignatureError,
  ProhibitedActionError,
  PublicKeyNotFoundError,
} from "@taquito/core";
import { HttpResponseError, STATUS_CODE } from "@taquito/http-utils";
import { Signer } from "@taquito/taquito";
import {
  ValidationResult,
  b58cdecode,
  b58cencode,
  buf2hex,
  getPkhfromPk,
  hex2buf,
  invalidDetail,
  isValidPrefix,
  mergebuf,
  prefix,
  validatePublicKey,
  verifySignature,
} from "@taquito/utils";
import { NativeBiometric } from "capacitor-native-biometric";
import toBuffer from "typedarray-to-buffer";
import {
  BadSigningDataError,
  OperationNotAuthorizedError,
  SignatureVerificationError,
} from "./errors";

interface PublicKeyResponse {
  public_key: string;
}

interface SignResponse {
  signature: string;
}

type curves = "ed" | "p2" | "sp";

const pref = {
  ed: {
    pk: prefix["edpk"],
    sk: prefix["edsk"],
    pkh: prefix.tz1,
    sig: prefix.edsig,
  },
  p2: {
    pk: prefix["p2pk"],
    sk: prefix["p2sk"],
    pkh: prefix.tz3,
    sig: prefix.p2sig,
  },
  sp: {
    pk: prefix["sppk"],
    sk: prefix["spsk"],
    pkh: prefix.tz2,
    sig: prefix.spsig,
  },
};

export class BiometricsSigner implements Signer {
  constructor(private pk: string) {
    const pkValidation = validatePublicKey(this.pk);
    if (pkValidation !== ValidationResult.VALID) {
      throw new InvalidPublicKeyError(this.pk, invalidDetail(pkValidation));
    }
  }

  async publicKeyHash(): Promise<string> {
    return getPkhfromPk(this.pk);
  }

  async publicKey(): Promise<string> {
    if (this.pk) return this.pk;
    else return (await NativeBiometric.getPublicKey()).publicKey;
  }

  async secretKey(): Promise<string> {
    throw new ProhibitedActionError("Secret key cannot be exposed");
  }

  async sign(bytes: string, watermark?: Uint8Array) {
    try {
      let bb = hex2buf(bytes);
      if (typeof watermark !== "undefined") {
        bb = mergebuf(watermark, bb);
      }
      const watermarkedBytes = buf2hex(toBuffer(bb));

      const { signature } = await NativeBiometric.sign({
        payload: watermarkedBytes,
      });

      const pref = signature.startsWith("sig")
        ? signature.substring(0, 3)
        : signature.substring(0, 5);

      if (!isValidPrefix(pref)) {
        throw new InvalidSignatureError(
          signature,
          invalidDetail(ValidationResult.NO_PREFIX_MATCHED) +
            ` from a remote signer.`
        );
      }

      const decoded = b58cdecode(signature, prefix[pref]);

      const pk = await this.publicKey();

      const signatureVerified = verifySignature(
        watermarkedBytes,
        pk,
        signature
      );
      if (!signatureVerified) {
        throw new SignatureVerificationError(watermarkedBytes, signature);
      }

      return {
        bytes,
        sig: b58cencode(decoded, prefix.sig),
        prefixSig: signature,
        sbytes: bytes + buf2hex(toBuffer(decoded)),
      };
    } catch (ex) {
      if (ex instanceof HttpResponseError) {
        if (ex.status === STATUS_CODE.NOT_FOUND) {
          throw new PublicKeyNotFoundError(this.pk, ex);
        } else if (ex.status === STATUS_CODE.FORBIDDEN) {
          throw new OperationNotAuthorizedError(
            "Signing Operation not authorized",
            ex
          );
        } else if (ex.status === STATUS_CODE.BAD_REQUEST) {
          throw new BadSigningDataError(ex, bytes, watermark);
        }
      }
      throw ex;
    }
  }
}
