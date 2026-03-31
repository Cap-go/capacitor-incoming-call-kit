import './style.css';
import { IncomingCallKit } from '@capgo/capacitor-incoming-call-kit';

const output = document.getElementById('plugin-output');
const statusBadge = document.getElementById('status-badge');
const permissionsButton = document.getElementById('request-permissions');
const fullscreenButton = document.getElementById('request-fullscreen');
const showButton = document.getElementById('show-call');
const endAllButton = document.getElementById('end-all-calls');
const refreshButton = document.getElementById('refresh-calls');
const versionButton = document.getElementById('get-version');
const randomCallId = () => `demo-${Date.now()}`;

const listeners = [
  'incomingCallDisplayed',
  'callAccepted',
  'callDeclined',
  'callEnded',
  'callTimedOut',
];

const setOutput = (value) => {
  output.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
};

const setStatus = (count) => {
  statusBadge.textContent = `${count} active`;
  statusBadge.dataset.active = String(count > 0);
};

const refreshCalls = async () => {
  try {
    const result = await IncomingCallKit.getActiveCalls();
    setStatus(result.calls.length);
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
};

permissionsButton.addEventListener('click', async () => {
  try {
    const result = await IncomingCallKit.requestPermissions();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

fullscreenButton.addEventListener('click', async () => {
  try {
    const result = await IncomingCallKit.requestFullScreenIntentPermission();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

showButton.addEventListener('click', async () => {
  try {
    const result = await IncomingCallKit.showIncomingCall({
      callId: randomCallId(),
      callerName: 'Ada Lovelace',
      handle: '+39 555 010 020',
      appName: 'Capgo Phone',
      hasVideo: true,
      timeoutMs: 45_000,
      extra: {
        roomId: 'room-42',
        callerUserId: 'user_ada',
      },
      android: {
        channelId: 'calls',
        channelName: 'Incoming Calls',
        showFullScreen: true,
      },
      ios: {
        handleType: 'phoneNumber',
      },
    });
    await refreshCalls();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

endAllButton.addEventListener('click', async () => {
  try {
    const result = await IncomingCallKit.endAllCalls({ reason: 'demo-reset' });
    await refreshCalls();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

refreshButton.addEventListener('click', refreshCalls);

versionButton.addEventListener('click', async () => {
  try {
    const result = await IncomingCallKit.getPluginVersion();
    setOutput(result);
  } catch (error) {
    setOutput(`Error: ${error?.message ?? error}`);
  }
});

listeners.forEach((eventName) => {
  void IncomingCallKit.addListener(eventName, async (event) => {
    await refreshCalls();
    setOutput({ eventName, event });
  });
});

void refreshCalls();
