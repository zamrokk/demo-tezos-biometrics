import { WebPlugin } from "@capacitor/core";
import { AvailableResult, BiometricOptions, NativeBiometricPlugin } from "./definitions";
export declare class NativeBiometricWeb extends WebPlugin implements NativeBiometricPlugin {
    constructor();
    isAvailable(): Promise<AvailableResult>;
    init(_options?: BiometricOptions): Promise<string>;
    getPublicKey(): Promise<string>;
    sign(watermarkedBytes: string): Promise<{
        signature: string;
    }>;
}
