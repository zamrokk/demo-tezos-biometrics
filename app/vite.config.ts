import react from "@vitejs/plugin-react-swc";
import path from "path";
import { defineConfig } from "vite";
// https://vitejs.dev/config/
export default ({ command }) => {
  const isBuild = command === "build";

  return defineConfig({
    define: {
      "process.title": "browser",
      "process.browser": true,
      "process.env": {},
      "process.argv": [],
      "process.version": "", // empty string to avoid regexp issues
      "process.versions": {},
      //global: {}
    },
    plugins: [react()],
    build: {
      commonjsOptions: {
        transformMixedEsModules: true,
      },
    },
    resolve: {
      alias: {
        // dedupe @airgap/beacon-sdk
        // I almost have no idea why it needs `cjs` on dev and `esm` on build, but this is how it works 🤷‍♂️
        "@airgap/beacon-sdk": path.resolve(
          path.resolve(),
          `./node_modules/@airgap/beacon-sdk/dist/${
            isBuild ? "esm" : "cjs"
          }/index.js`
        ),
        stream: "stream-browserify",
        os: "os-browserify/browser",
        util: "util",
        process: "process/browser",
        buffer: "buffer",
        crypto: "crypto-browserify",
        assert: "assert",
        http: "stream-http",
        https: "https-browserify",
        url: "url",
        path: "path-browserify",
      },
    },
  });
};
