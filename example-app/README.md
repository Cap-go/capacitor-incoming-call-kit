# Example App for `@capgo/capacitor-incoming-call-kit`

This Vite project links directly to the local plugin source so you can validate incoming-call behavior on web, iOS, and Android while developing.

## Getting started

```bash
bun install
bun run start
```

To test on native shells:

```bash
bunx cap add ios
bunx cap add android
bunx cap sync
```

Use the example UI to:

- request Android notification permission,
- open the Android 14+ full-screen intent settings page,
- trigger a demo incoming call payload,
- inspect active calls,
- and watch emitted listener payloads.
