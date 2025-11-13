import js from "@eslint/js";
import globals from "globals";
import pluginReact from "eslint-plugin-react";
import reactHooks from 'eslint-plugin-react-hooks';
import { defineConfig } from "eslint/config";

export default defineConfig([
  {
    files: ["**/*.{js,mjs,cjs,jsx}"],
    plugins: {
      js ,
      'react-hooks': reactHooks
    },
    extends: ["js/recommended"],
    languageOptions: { globals: globals.browser }
  },
  pluginReact.configs.flat.recommended, // React config first
  reactHooks.configs.flat.recommended,
  {
    rules: {
      "react/prop-types": "off",
      "react/no-children-prop": "off",
      'react-hooks/exhaustive-deps': 'warn',
      "react/react-in-jsx-scope" : "off",
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/immutability":"off"
    },
  },

]);
