import { WebPlugin } from '@capacitor/core';

import type {
  ActiveCallsResult,
  EndAllCallsOptions,
  EndCallOptions,
  IncomingCallEvent,
  IncomingCallEventName,
  IncomingCallKitPlugin,
  IncomingCallPermissionStatus,
  IncomingCallRecord,
  PluginVersionResult,
  ShowIncomingCallOptions,
  ShowIncomingCallResult,
} from './definitions';

export class IncomingCallKitWeb extends WebPlugin implements IncomingCallKitPlugin {
  private readonly calls = new Map<string, IncomingCallRecord>();

  async showIncomingCall(options: ShowIncomingCallOptions): Promise<ShowIncomingCallResult> {
    const call: IncomingCallRecord = {
      callId: options.callId,
      callerName: options.callerName,
      handle: options.handle,
      hasVideo: options.hasVideo ?? false,
      state: 'ringing',
      platform: 'web',
      extra: options.extra,
    };

    this.calls.set(call.callId, call);
    await this.emit('incomingCallDisplayed', call, undefined, 'api');

    if ((options.timeoutMs ?? 60_000) > 0) {
      window.setTimeout(() => {
        const currentCall = this.calls.get(call.callId);
        if (currentCall?.state === 'ringing') {
          this.calls.delete(call.callId);
          void this.emit('callTimedOut', { ...currentCall, state: 'ended' }, 'timeout', 'system');
        }
      }, options.timeoutMs ?? 60_000);
    }

    return { call };
  }

  async endCall(options: EndCallOptions): Promise<ActiveCallsResult> {
    const call = this.calls.get(options.callId);
    if (call) {
      this.calls.delete(options.callId);
      await this.emit('callEnded', { ...call, state: 'ended' }, options.reason ?? 'ended', 'api');
    }

    return this.getActiveCalls();
  }

  async endAllCalls(options?: EndAllCallsOptions): Promise<ActiveCallsResult> {
    const calls = [...this.calls.values()];
    this.calls.clear();
    await Promise.all(
      calls.map((call) => this.emit('callEnded', { ...call, state: 'ended' }, options?.reason ?? 'ended', 'api')),
    );
    return this.getActiveCalls();
  }

  async getActiveCalls(): Promise<ActiveCallsResult> {
    return {
      calls: [...this.calls.values()],
    };
  }

  async checkPermissions(): Promise<IncomingCallPermissionStatus> {
    return {
      notifications: 'notApplicable',
      fullScreenIntent: 'notApplicable',
    };
  }

  async requestPermissions(): Promise<IncomingCallPermissionStatus> {
    return this.checkPermissions();
  }

  async requestFullScreenIntentPermission(): Promise<IncomingCallPermissionStatus> {
    return this.checkPermissions();
  }

  async getPluginVersion(): Promise<PluginVersionResult> {
    return {
      version: 'web',
    };
  }

  private async emit(
    eventName: IncomingCallEventName,
    call: IncomingCallRecord,
    reason: string | undefined,
    source: IncomingCallEvent['source'],
  ): Promise<void> {
    const payload: IncomingCallEvent = {
      call,
      source,
      ...(reason ? { reason } : {}),
    };

    await this.notifyListeners(eventName, payload);
  }
}
