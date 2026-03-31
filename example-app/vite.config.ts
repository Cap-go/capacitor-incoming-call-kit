import { resolve } from 'node:path';

import { defineConfig } from 'vite';

export default defineConfig({
  resolve: {
    alias: {
      '@capgo/capacitor-incoming-call-kit': resolve(__dirname, '../dist/esm/index.js'),
    },
  },
  server: {
    open: true,
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
