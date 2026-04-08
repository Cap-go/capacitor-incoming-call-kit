package app.capgo.incomingcallkit;

import android.text.TextUtils;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.json.JSONException;
import org.json.JSONObject;

public class IncomingCallRecord {

    private static final String DEFAULT_CHANNEL_ID = "incoming_call_kit";
    private static final String DEFAULT_CHANNEL_NAME = "Incoming Calls";
    private static final String DEFAULT_ACCEPT_TEXT = "Accept";
    private static final String DEFAULT_DECLINE_TEXT = "Decline";

    private final String callId;
    private final String callerName;
    private final String handle;
    private final String appName;
    private final boolean hasVideo;
    private final long timeoutMs;
    private final String acceptText;
    private final String declineText;
    private final String channelId;
    private final String channelName;
    private final boolean showFullScreen;
    private final String accentColor;
    private final String ringtoneUri;
    private final boolean highPriority;
    private final JSONObject extra;
    private String state;

    public IncomingCallRecord(
        final String callId,
        final String callerName,
        final String handle,
        final String appName,
        final boolean hasVideo,
        final long timeoutMs,
        final String acceptText,
        final String declineText,
        final String channelId,
        final String channelName,
        final boolean showFullScreen,
        final String accentColor,
        final String ringtoneUri,
        final boolean highPriority,
        final JSONObject extra,
        final String state
    ) {
        this.callId = callId;
        this.callerName = callerName;
        this.handle = handle;
        this.appName = appName;
        this.hasVideo = hasVideo;
        this.timeoutMs = timeoutMs;
        this.acceptText = acceptText;
        this.declineText = declineText;
        this.channelId = channelId;
        this.channelName = channelName;
        this.showFullScreen = showFullScreen;
        this.accentColor = accentColor;
        this.ringtoneUri = ringtoneUri;
        this.highPriority = highPriority;
        this.extra = extra == null ? new JSObject() : extra;
        this.state = state;
    }

    public static IncomingCallRecord fromCall(final PluginCall call) {
        final String callId = call.getString("callId");
        final String callerName = call.getString("callerName");

        if (TextUtils.isEmpty(callId)) {
            throw new IllegalArgumentException("Missing required field 'callId'.");
        }

        if (TextUtils.isEmpty(callerName)) {
            throw new IllegalArgumentException("Missing required field 'callerName'.");
        }

        final JSObject androidOptions = call.getObject("android");
        final JSONObject extra = call.getObject("extra") == null ? new JSObject() : call.getObject("extra");
        final Double timeoutValue = call.getDouble("timeoutMs");
        final long timeoutMs = timeoutValue == null ? 60_000L : Math.max(0L, timeoutValue.longValue());

        return new IncomingCallRecord(
            callId,
            callerName,
            call.getString("handle"),
            call.getString("appName"),
            Boolean.TRUE.equals(call.getBoolean("hasVideo")),
            timeoutMs,
            call.getString("acceptText", DEFAULT_ACCEPT_TEXT),
            call.getString("declineText", DEFAULT_DECLINE_TEXT),
            androidOptions == null ? DEFAULT_CHANNEL_ID : androidOptions.optString("channelId", DEFAULT_CHANNEL_ID),
            androidOptions == null ? DEFAULT_CHANNEL_NAME : androidOptions.optString("channelName", DEFAULT_CHANNEL_NAME),
            androidOptions == null || androidOptions.optBoolean("showFullScreen", true),
            androidOptions == null ? null : androidOptions.optString("accentColor", null),
            androidOptions == null ? null : androidOptions.optString("ringtoneUri", null),
            androidOptions == null || androidOptions.optBoolean("isHighPriority", true),
            extra,
            "ringing"
        );
    }

    public static IncomingCallRecord fromJson(final JSONObject json) throws JSONException {
        return new IncomingCallRecord(
            json.getString("callId"),
            json.getString("callerName"),
            json.optString("handle", null),
            json.optString("appName", null),
            json.optBoolean("hasVideo", false),
            json.optLong("timeoutMs", 60_000L),
            json.optString("acceptText", DEFAULT_ACCEPT_TEXT),
            json.optString("declineText", DEFAULT_DECLINE_TEXT),
            json.optString("channelId", DEFAULT_CHANNEL_ID),
            json.optString("channelName", DEFAULT_CHANNEL_NAME),
            json.optBoolean("showFullScreen", true),
            json.optString("accentColor", null),
            json.optString("ringtoneUri", null),
            json.optBoolean("highPriority", true),
            json.optJSONObject("extra") == null ? new JSObject() : json.optJSONObject("extra"),
            json.optString("state", "ringing")
        );
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        try {
            json.put("callId", callId);
            json.put("callerName", callerName);
            json.put("handle", handle);
            json.put("appName", appName);
            json.put("hasVideo", hasVideo);
            json.put("timeoutMs", timeoutMs);
            json.put("acceptText", acceptText);
            json.put("declineText", declineText);
            json.put("channelId", channelId);
            json.put("channelName", channelName);
            json.put("showFullScreen", showFullScreen);
            json.put("accentColor", accentColor);
            json.put("ringtoneUri", ringtoneUri);
            json.put("highPriority", highPriority);
            json.put("extra", extra);
            json.put("state", state);
        } catch (JSONException ignored) {}
        return json;
    }

    public JSObject toJSObject() {
        final JSObject object = new JSObject();
        object.put("callId", callId);
        object.put("callerName", callerName);
        object.put("handle", handle);
        object.put("hasVideo", hasVideo);
        object.put("state", state);
        object.put("platform", "android");
        if (extra != null && extra.length() > 0) {
            object.put("extra", extra);
        }
        return object;
    }

    public String getCallId() {
        return callId;
    }

    public String getCallerName() {
        return callerName;
    }

    public String getHandle() {
        return handle;
    }

    public String getAppName() {
        return appName;
    }

    public boolean hasVideo() {
        return hasVideo;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public String getAcceptText() {
        return acceptText;
    }

    public String getDeclineText() {
        return declineText;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public boolean isShowFullScreen() {
        return showFullScreen;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getRingtoneUri() {
        return ringtoneUri;
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public JSONObject getExtra() {
        return extra;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public int getNotificationId() {
        return 40_000 + Math.abs(callId.hashCode() % 10_000);
    }
}
