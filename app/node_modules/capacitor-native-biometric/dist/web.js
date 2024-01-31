import { WebPlugin } from "@capacitor/core";
export class NativeBiometricWeb extends WebPlugin {
    constructor() {
        super();
    }
    isAvailable() {
        throw new Error("Method not implemented.");
    }
    init(_options) {
        throw new Error("Method not implemented.");
    }
    //call only if already initialized, otherwise it throws an error
    getPublicKey() {
        throw new Error("Method not implemented.");
    }
    //sign payload with optional magic bytes
    sign(options) {
        throw new Error("Method not implemented." + options);
    }
}
//# sourceMappingURL=web.js.map