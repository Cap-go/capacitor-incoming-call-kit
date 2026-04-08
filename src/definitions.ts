import type { PermissionState, PluginListenerHandle } from '@capacitor/core';

/**
 * Supported incoming call states.
 */
export type IncomingCallState = 'ringing' | 'accepted' | 'ended';

/**
 * Platform-normalized listener names.
 */
export type IncomingCallEventName =
  | 'incomingCallDisplayed'
  | 'callAccepted'
  | 'callDeclined'
  | 'callEnded'
  | 'callTimedOut';

/**
 * Plugin version payload.
 */
export interface PluginVersionResult {
  /**
   * Version identifier returned by the platform implementation.
   */
  version: string;
}

/**
 * Represents a currently tracked call.
 */
export interface IncomingCallRecord {
  /**
   * Stable call identifier provided by your app.
   */
  callId: string;

  /**
   * Name shown in the native UI.
   */
  callerName: string;

  /**
   * Secondary handle shown by the platform when available.
   */
  handle?: string;

  /**
   * Whether this call should be treated as a video call.
   */
  hasVideo: boolean;

  /**
   * Current platform-reported call state.
   */
  state: IncomingCallState;

  /**
   * Platform that produced the record.
   */
  platform: 'android' | 'ios' | 'web';

  /**
   * Arbitrary metadata passed in at call creation time.
   */
  extra?: Record<string, any>;
}

/**
 * Result payload for `showIncomingCall()`.
 */
export interface ShowIncomingCallResult {
  /**
   * The call record that was created or reused.
   */
  call: IncomingCallRecord;
}

/**
 * Result payload for `getActiveCalls()`.
 */
export interface ActiveCallsResult {
  /**
   * Calls still tracked by the native implementation.
   */
  calls: IncomingCallRecord[];
}

/**
 * Result payload for permission checks.
 */
export interface IncomingCallPermissionStatus {
  /**
   * Notification permission state.
   *
   * iOS CallKit itself does not require runtime notification permission, so iOS returns `notApplicable`.
   */
  notifications: PermissionState | 'notApplicable';

  /**
   * Full-screen intent permission state.
   *
   * iOS returns `notApplicable`. Android 13 and below resolve this as `granted`.
   */
  fullScreenIntent: PermissionState | 'notApplicable';
}

/**
 * Common options used to present an incoming call.
 */
export interface ShowIncomingCallOptions {
  /**
   * Stable identifier for the call.
   *
   * Reuse the same value when ending the call later.
   */
  callId: string;

  /**
   * Primary name shown to the user.
   */
  callerName: string;

  /**
   * Optional secondary handle such as a phone number, SIP URI, or user ID.
   */
  handle?: string;

  /**
   * Label shown by Android in notifications.
   *
   * iOS uses the app display name configured in the host app bundle.
   */
  appName?: string;

  /**
   * Whether the incoming session should be marked as video-capable.
   */
  hasVideo?: boolean;

  /**
   * Best-effort timeout in milliseconds.
   *
   * Defaults to `60000`.
   */
  timeoutMs?: number;

  /**
   * Custom label for the accept action.
   *
   * Android only.
   */
  acceptText?: string;

  /**
   * Custom label for the decline action.
   *
   * Android only.
   */
  declineText?: string;

  /**
   * Arbitrary JSON metadata echoed back in all events.
   */
  extra?: Record<string, any>;

  /**
   * Android-specific behavior overrides.
   */
  android?: AndroidIncomingCallOptions;

  /**
   * iOS-specific behavior overrides.
   */
  ios?: IOSIncomingCallOptions;
}

/**
 * Android-specific incoming call presentation options.
 */
export interface AndroidIncomingCallOptions {
  /**
   * Notification channel identifier.
   *
   * Defaults to `incoming_call_kit`.
   */
  channelId?: string;

  /**
   * Notification channel display name.
   *
   * Defaults to `Incoming Calls`.
   */
  channelName?: string;

  /**
   * Whether Android should request full-screen presentation when possible.
   *
   * Defaults to `true`.
   */
  showFullScreen?: boolean;

  /**
   * Optional accent color in `#RRGGBB` or `#AARRGGBB` form.
   */
  accentColor?: string;

  /**
   * Optional ringtone URI string.
   *
   * Example: `android.resource://com.example.app/raw/ringtone`
   */
  ringtoneUri?: string;

  /**
   * Whether to mark the notification as high priority.
   *
   * Defaults to `true`.
   */
  isHighPriority?: boolean;
}

/**
 * iOS-specific incoming call presentation options.
 */
export interface IOSIncomingCallOptions {
  /**
   * CallKit handle type.
   *
   * Defaults to `generic`.
   */
  handleType?: 'generic' | 'phoneNumber' | 'emailAddress';

  /**
   * Whether the call should support hold.
   *
   * Defaults to `true`.
   */
  supportsHolding?: boolean;

  /**
   * Whether the call should support DTMF.
   *
   * Defaults to `false`.
   */
  supportsDTMF?: boolean;

  /**
   * Whether the call should support grouping.
   *
   * Defaults to `false`.
   */
  supportsGrouping?: boolean;

  /**
   * Whether the call should support ungrouping.
   *
   * Defaults to `false`.
   */
  supportsUngrouping?: boolean;
}

/**
 * Options for ending a single tracked call.
 */
export interface EndCallOptions {
  /**
   * The call identifier originally passed to `showIncomingCall()`.
   */
  callId: string;

  /**
   * Optional application-defined reason string.
   */
  reason?: string;
}

/**
 * Options for ending all tracked calls.
 */
export interface EndAllCallsOptions {
  /**
   * Optional application-defined reason string.
   */
  reason?: string;
}

/**
 * Payload delivered by plugin listeners.
 */
export interface IncomingCallEvent {
  /**
   * The call that triggered the event.
   */
  call: IncomingCallRecord;

  /**
   * Optional reason for the state transition.
   */
  reason?: string;

  /**
   * Origin of the action.
   */
  source: 'api' | 'user' | 'system';
}

/**
 * Capacitor API for presenting a native incoming-call surface.
 */
export interface IncomingCallKitPlugin {
  /**
   * Displays the native incoming call UI.
   *
   * Android shows a high-priority notification and can raise a full-screen activity.
   * iOS reports the call to CallKit.
   */
  showIncomingCall(options: ShowIncomingCallOptions): Promise<ShowIncomingCallResult>;

  /**
   * Ends a specific tracked call.
   */
  endCall(options: EndCallOptions): Promise<ActiveCallsResult>;

  /**
   * Ends every tracked call.
   */
  endAllCalls(options?: EndAllCallsOptions): Promise<ActiveCallsResult>;

  /**
   * Returns the currently tracked calls.
   */
  getActiveCalls(): Promise<ActiveCallsResult>;

  /**
   * Returns the current permission state for notifications and full-screen intents.
   */
  checkPermissions(): Promise<IncomingCallPermissionStatus>;

  /**
   * Requests the notification permission when the platform supports it.
   *
   * iOS CallKit itself does not require a runtime prompt, so iOS resolves without prompting.
   */
  requestPermissions(): Promise<IncomingCallPermissionStatus>;

  /**
   * Opens the Android 14+ full-screen intent settings page when available.
   *
   * On other platforms this resolves with the current permission status.
   */
  requestFullScreenIntentPermission(): Promise<IncomingCallPermissionStatus>;

  /**
   * Returns the native implementation version marker.
   */
  getPluginVersion(): Promise<PluginVersionResult>;

  /**
   * Fired after the call has been handed to the native platform UI.
   */
  addListener(
    eventName: 'incomingCallDisplayed',
    listenerFunc: (event: IncomingCallEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Fired when the user accepts the call from native UI.
   */
  addListener(
    eventName: 'callAccepted',
    listenerFunc: (event: IncomingCallEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Fired when the user declines the call from native UI.
   */
  addListener(
    eventName: 'callDeclined',
    listenerFunc: (event: IncomingCallEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Fired when a call ends through the API or a platform action.
   */
  addListener(eventName: 'callEnded', listenerFunc: (event: IncomingCallEvent) => void): Promise<PluginListenerHandle>;

  /**
   * Fired when an unanswered call reaches its configured timeout.
   */
  addListener(
    eventName: 'callTimedOut',
    listenerFunc: (event: IncomingCallEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Removes every native listener registered by the plugin.
   */
  removeAllListeners(): Promise<void>;
}
