package app.capgo.incomingcallkit;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class IncomingCallActivity extends AppCompatActivity {

    public static Intent createIntent(final Context context, final String callId) {
        final Intent intent = new Intent(context, IncomingCallActivity.class);
        intent.putExtra(IncomingCallActionReceiver.EXTRA_CALL_ID, callId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowOverLockScreen();
        setContentView(R.layout.activity_incoming_call);

        final String callId = getIntent().getStringExtra(IncomingCallActionReceiver.EXTRA_CALL_ID);
        final IncomingCallRecord call = IncomingCallStore.getActiveCall(this, callId);

        if (call == null) {
            finish();
            return;
        }

        final TextView titleView = findViewById(R.id.incoming_call_title);
        final TextView handleView = findViewById(R.id.incoming_call_handle);
        final TextView appNameView = findViewById(R.id.incoming_call_app_name);
        final Button acceptButton = findViewById(R.id.incoming_call_accept);
        final Button declineButton = findViewById(R.id.incoming_call_decline);

        titleView.setText(call.getCallerName());
        handleView.setText(call.getHandle() == null ? "Incoming call" : call.getHandle());
        appNameView.setText(call.getAppName() == null ? getApplicationInfo().loadLabel(getPackageManager()) : call.getAppName());
        acceptButton.setText(call.getAcceptText());
        declineButton.setText(call.getDeclineText());

        acceptButton.setOnClickListener((view) -> {
            IncomingCallController.acceptCall(this, call.getCallId(), true);
            finish();
        });

        declineButton.setOnClickListener((view) -> {
            IncomingCallController.declineCall(this, call.getCallId(), false);
            finish();
        });
    }

    private void setShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
    }
}
