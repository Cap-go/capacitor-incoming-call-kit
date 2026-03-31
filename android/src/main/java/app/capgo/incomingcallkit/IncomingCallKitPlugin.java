package app.capgo.incomingcallkit;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "IncomingCallKit",
    permissions = {
        @Permission(
            alias = IncomingCallKitPlugin.NOTIFICATIONS,
            strings = { Manifest.permission.POST_NOTIFICATIONS }
        )
    }
)
public class IncomingCallKitPlugin extends Plugin {

    static final String NOTIFICATIONS = "notifications";

    @Override
    public void load() {
        IncomingCallController.attachPlugin(this);
    }

    void dispatchEvent(final String eventName, final JSObject payload) {
        notifyListeners(eventName, payload, true);
    }

    @PluginMethod
    public void showIncomingCall(final PluginCall call) {
        try {
            final IncomingCallRecord record = IncomingCallRecord.fromCall(call);
            final boolean launchImmediately = getActivity() instanceof android.app.Activity;
            final IncomingCallRecord shown = IncomingCallController.showIncomingCall(getContext(), record, launchImmediately);

            final JSObject result = new JSObject();
            result.put("call", shown.toJSObject());
            call.resolve(result);
        } catch (IllegalArgumentException exception) {
            call.reject(exception.getMessage());
        }
    }

    @PluginMethod
    public void endCall(final PluginCall call) {
        final String callId = call.getString("callId");
        if (callId == null || callId.isEmpty()) {
            call.reject("Missing required field 'callId'.");
            return;
        }

        IncomingCallController.endCall(getContext(), callId, call.getString("reason"));
        resolveActiveCalls(call);
    }

    @PluginMethod
    public void endAllCalls(final PluginCall call) {
        IncomingCallController.endAllCalls(getContext(), call.getString("reason"));
        resolveActiveCalls(call);
    }

    @PluginMethod
    public void getActiveCalls(final PluginCall call) {
        resolveActiveCalls(call);
    }

    @PluginMethod
    @Override
    public void checkPermissions(final PluginCall call) {
        call.resolve(permissionStatus());
    }

    @PluginMethod
    @Override
    public void requestPermissions(final PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || getPermissionState(NOTIFICATIONS) == PermissionState.GRANTED) {
            call.resolve(permissionStatus());
            return;
        }

        requestPermissionForAlias(NOTIFICATIONS, call, "permissionsCallback");
    }

    @PermissionCallback
    private void permissionsCallback(final PluginCall call) {
        call.resolve(permissionStatus());
    }

    @PluginMethod
    public void requestFullScreenIntentPermission(final PluginCall call) {
        if (Build.VERSION.SDK_INT >= 34) {
            final Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }

        call.resolve(permissionStatus());
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        final JSObject result = new JSObject();
        result.put("version", "android");
        call.resolve(result);
    }

    private void resolveActiveCalls(final PluginCall call) {
        final JSArray calls = new JSArray();
        for (IncomingCallRecord record : IncomingCallStore.getActiveCalls(getContext())) {
            calls.put(record.toJSObject());
        }

        final JSObject result = new JSObject();
        result.put("calls", calls);
        call.resolve(result);
    }

    private JSObject permissionStatus() {
        final JSObject result = new JSObject();
        result.put("notifications", notificationPermissionState());
        result.put("fullScreenIntent", fullScreenIntentPermissionState());
        return result;
    }

    private String notificationPermissionState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "granted";
        }
        return permissionStateToString(getPermissionState(NOTIFICATIONS));
    }

    private String fullScreenIntentPermissionState() {
        if (Build.VERSION.SDK_INT < 34) {
            return "granted";
        }

        final NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return "prompt";
        }

        return notificationManager.canUseFullScreenIntent() ? "granted" : "denied";
    }

    private String permissionStateToString(final PermissionState state) {
        switch (state) {
        case GRANTED:
            return "granted";
        case DENIED:
            return "denied";
        case PROMPT_WITH_RATIONALE:
            return "prompt-with-rationale";
        case PROMPT:
        default:
            return "prompt";
        }
    }
}
