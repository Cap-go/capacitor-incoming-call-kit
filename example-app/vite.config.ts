import { existsSync } from 'node:fs';
import { resolve } from 'node:path';

import { defineConfig } from 'vite';

const localPluginEntry = resolve(__dirname, '../dist/esm/index.js');

export default defineConfig({
  resolve: {
    alias: existsSync(localPluginEntry)
      ? {
          '@capgo/capacitor-incoming-call-kit': localPluginEntry,
        }
      : {},
  },
  server: {
    open: true,
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
