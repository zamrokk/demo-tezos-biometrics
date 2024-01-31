import { WebPlugin } from "@capacitor/core";
import { AvailableResult, BiometricOptions, NativeBiometricPlugin } from "./definitions";
export declare class NativeBiometricWeb extends WebPlugin implements NativeBiometricPlugin {
    constructor();
    isAvailable(): Promise<AvailableResult>;
    init(_options?: BiometricOptions): Promise<{
        publicKey: string;
    }>;
    getPublicKey(): Promise<{
        publicKey: string;
    }>;
    sign(watermarkedBytes: string): Promise<{
        signature: string;
    }>;
}
