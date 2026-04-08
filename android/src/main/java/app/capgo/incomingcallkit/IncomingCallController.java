package app.capgo.incomingcallkit;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSObject;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IncomingCallController {

    private static final String TAG = "IncomingCallKit";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<String, Runnable> TIMEOUTS = new ConcurrentHashMap<>();
    private static WeakReference<IncomingCallKitPlugin> pluginRef = new WeakReference<>(null);

    private IncomingCallController() {}

    public static void attachPlugin(final IncomingCallKitPlugin plugin) {
        pluginRef = new WeakReference<>(plugin);
        flushPendingEvents(plugin.getContext());
    }

    public static void flushPendingEvents(final Context context) {
        for (IncomingCallStore.QueuedEvent event : IncomingCallStore.drainPendingEvents(context)) {
            dispatchOrQueue(context, event.eventName, event.payload);
        }
    }

    public static IncomingCallRecord showIncomingCall(
        final Context context,
        final IncomingCallRecord call,
        final boolean launchImmediately
    ) {
        IncomingCallStore.upsertActiveCall(context, call);
        postNotification(context, call);
        scheduleTimeout(context, call);
        dispatchOrQueue(context, "incomingCallDisplayed", makeEventPayload(call, null, "api"));

        if (launchImmediately && call.isShowFullScreen()) {
            launchIncomingCallActivity(context, call.getCallId());
        }

        return call;
    }

    public static void acceptCall(final Context context, final String callId, final boolean launchApp) {
        final IncomingCallRecord call = IncomingCallStore.getActiveCall(context, callId);
        if (call == null) {
            return;
        }

        cancelTimeout(callId);
        cancelNotification(context, call);
        call.setState("accepted");
        IncomingCallStore.upsertActiveCall(context, call);
        dispatchOrQueue(context, "callAccepted", makeEventPayload(call, "accepted", "user"));

        if (launchApp) {
            launchHostApp(context, callId);
        }
    }

    public static void declineCall(final Context context, final String callId, final boolean launchApp) {
        final IncomingCallRecord call = IncomingCallStore.getActiveCall(context, callId);
        if (call == null) {
            return;
        }

        cancelTimeout(callId);
        cancelNotification(context, call);
        IncomingCallStore.removeActiveCall(context, callId);
        call.setState("ended");
        dispatchOrQueue(context, "callDeclined", makeEventPayload(call, "declined", "user"));

        if (launchApp) {
            launchHostApp(context, callId);
        }
    }

    public static void endCall(final Context context, final String callId, final String reason) {
        final IncomingCallRecord call = IncomingCallStore.getActiveCall(context, callId);
        if (call == null) {
            return;
        }

        cancelTimeout(callId);
        cancelNotification(context, call);
        IncomingCallStore.removeActiveCall(context, callId);
        call.setState("ended");
        dispatchOrQueue(context, "callEnded", makeEventPayload(call, TextUtils.isEmpty(reason) ? "ended" : reason, "api"));
    }

    public static List<IncomingCallRecord> endAllCalls(final Context context, final String reason) {
        for (IncomingCallRecord call : IncomingCallStore.getActiveCalls(context)) {
            endCall(context, call.getCallId(), reason);
        }

        return IncomingCallStore.getActiveCalls(context);
    }

    public static void timeoutCall(final Context context, final String callId) {
        final IncomingCallRecord call = IncomingCallStore.getActiveCall(context, callId);
        if (call == null || !"ringing".equals(call.getState())) {
            return;
        }

        cancelTimeout(callId);
        cancelNotification(context, call);
        IncomingCallStore.removeActiveCall(context, callId);
        call.setState("ended");
        dispatchOrQueue(context, "callTimedOut", makeEventPayload(call, "timeout", "system"));
    }

    private static void scheduleTimeout(final Context context, final IncomingCallRecord call) {
        cancelTimeout(call.getCallId());

        if (call.getTimeoutMs() <= 0) {
            return;
        }

        final Runnable timeout = () -> timeoutCall(context, call.getCallId());
        TIMEOUTS.put(call.getCallId(), timeout);
        MAIN_HANDLER.postDelayed(timeout, call.getTimeoutMs());
    }

    private static void cancelTimeout(final String callId) {
        final Runnable timeout = TIMEOUTS.remove(callId);
        if (timeout != null) {
            MAIN_HANDLER.removeCallbacks(timeout);
        }
    }

    private static void postNotification(final Context context, final IncomingCallRecord call) {
        createNotificationChannel(context, call);

        final PendingIntent contentIntent = createContentIntent(context, call.getCallId());
        final PendingIntent acceptIntent = createActionIntent(context, IncomingCallActionReceiver.ACTION_ACCEPT_CALL, call.getCallId());
        final PendingIntent declineIntent = createActionIntent(context, IncomingCallActionReceiver.ACTION_DECLINE_CALL, call.getCallId());

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, call.getChannelId())
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(call.getCallerName())
            .setContentText(TextUtils.isEmpty(call.getHandle()) ? "Incoming call" : call.getHandle())
            .setSubText(call.getAppName())
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(call.isHighPriority() ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.sym_action_call, call.getAcceptText(), acceptIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, call.getDeclineText(), declineIntent);

        if (call.getTimeoutMs() > 0) {
            builder.setTimeoutAfter(call.getTimeoutMs());
        }

        if (!TextUtils.isEmpty(call.getAccentColor())) {
            try {
                builder.setColor(Color.parseColor(call.getAccentColor()));
            } catch (IllegalArgumentException ignored) {}
        }

        if (call.isShowFullScreen()) {
            builder.setFullScreenIntent(contentIntent, true);
        }

        NotificationManagerCompat.from(context).notify(call.getNotificationId(), builder.build());
    }

    private static PendingIntent createContentIntent(final Context context, final String callId) {
        final Intent intent = IncomingCallActivity.createIntent(context, callId);
        return PendingIntent.getActivity(
            context,
            requestCode(callId, 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent createActionIntent(final Context context, final String action, final String callId) {
        final Intent intent = new Intent(context, IncomingCallActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(IncomingCallActionReceiver.EXTRA_CALL_ID, callId);

        return PendingIntent.getBroadcast(
            context,
            requestCode(callId, action.hashCode()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(final String callId, final int salt) {
        return Math.abs((callId + ":" + salt).hashCode());
    }

    private static void createNotificationChannel(final Context context, final IncomingCallRecord call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        final int importance = call.isHighPriority() ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;
        final NotificationChannel channel = new NotificationChannel(call.getChannelId(), call.getChannelName(), importance);
        channel.setDescription("Incoming call alerts");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        final Uri sound = resolveRingtone(call);
        final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        channel.setSound(sound, attributes);

        final NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private static Uri resolveRingtone(final IncomingCallRecord call) {
        if (!TextUtils.isEmpty(call.getRingtoneUri())) {
            return Uri.parse(call.getRingtoneUri());
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    private static void launchIncomingCallActivity(final Context context, final String callId) {
        try {
            context.startActivity(IncomingCallActivity.createIntent(context, callId));
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Failed to launch incoming call activity", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected failure while launching incoming call activity", e);
        }
    }

    private static void launchHostApp(final Context context, final String callId) {
        try {
            final Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launchIntent == null) {
                return;
            }

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.putExtra(IncomingCallActionReceiver.EXTRA_CALL_ID, callId);
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Failed to launch host app", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected failure while launching host app", e);
        }
    }

    private static void cancelNotification(final Context context, final IncomingCallRecord call) {
        NotificationManagerCompat.from(context).cancel(call.getNotificationId());
    }

    private static JSObject makeEventPayload(final IncomingCallRecord call, final String reason, final String source) {
        final JSObject payload = new JSObject();
        payload.put("call", call.toJSObject());
        payload.put("source", source);
        if (!TextUtils.isEmpty(reason)) {
            payload.put("reason", reason);
        }
        return payload;
    }

    private static void dispatchOrQueue(final Context context, final String eventName, final JSObject payload) {
        final IncomingCallKitPlugin plugin = pluginRef.get();
        if (plugin != null) {
            plugin.dispatchEvent(eventName, payload);
        } else {
            IncomingCallStore.queueEvent(context, eventName, payload);
        }
    }
}
