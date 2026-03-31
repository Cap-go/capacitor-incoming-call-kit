import { registerPlugin } from '@capacitor/core';

import type { IncomingCallKitPlugin } from './definitions';

const IncomingCallKit = registerPlugin<IncomingCallKitPlugin>('IncomingCallKit', {
  web: () => import('./web').then((m) => new m.IncomingCallKitWeb()),
});

export * from './definitions';
export { IncomingCallKit };
