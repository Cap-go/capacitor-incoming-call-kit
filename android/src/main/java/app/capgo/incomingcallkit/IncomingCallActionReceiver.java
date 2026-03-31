package app.capgo.incomingcallkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IncomingCallActionReceiver extends BroadcastReceiver {

    public static final String ACTION_ACCEPT_CALL = "app.capgo.incomingcallkit.ACCEPT_CALL";
    public static final String ACTION_DECLINE_CALL = "app.capgo.incomingcallkit.DECLINE_CALL";
    public static final String EXTRA_CALL_ID = "incomingCallId";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        final String callId = intent.getStringExtra(EXTRA_CALL_ID);
        if (callId == null || action == null) {
            return;
        }

        if (ACTION_ACCEPT_CALL.equals(action)) {
            IncomingCallController.acceptCall(context, callId, true);
        } else if (ACTION_DECLINE_CALL.equals(action)) {
            IncomingCallController.declineCall(context, callId, false);
        }
    }
}
