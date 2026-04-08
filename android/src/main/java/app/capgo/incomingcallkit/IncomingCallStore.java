package app.capgo.incomingcallkit;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class IncomingCallStore {

    private static final String PREFS_NAME = "capgo_incoming_call_kit";
    private static final String KEY_ACTIVE_CALLS = "active_calls";
    private static final String KEY_PENDING_EVENTS = "pending_events";

    private IncomingCallStore() {}

    public static final class QueuedEvent {

        public final String eventName;
        public final JSObject payload;

        public QueuedEvent(final String eventName, final JSObject payload) {
            this.eventName = eventName;
            this.payload = payload;
        }
    }

    public static synchronized List<IncomingCallRecord> getActiveCalls(final Context context) {
        final JSONArray array = readArray(context, KEY_ACTIVE_CALLS);
        final List<IncomingCallRecord> calls = new ArrayList<>();

        for (int index = 0; index < array.length(); index++) {
            final JSONObject object = array.optJSONObject(index);
            if (object == null) {
                continue;
            }

            try {
                calls.add(IncomingCallRecord.fromJson(object));
            } catch (JSONException ignored) {}
        }

        return calls;
    }

    public static synchronized IncomingCallRecord getActiveCall(final Context context, final String callId) {
        for (IncomingCallRecord call : getActiveCalls(context)) {
            if (call.getCallId().equals(callId)) {
                return call;
            }
        }
        return null;
    }

    public static synchronized void upsertActiveCall(final Context context, final IncomingCallRecord call) {
        final JSONArray array = new JSONArray();
        boolean inserted = false;

        for (IncomingCallRecord existing : getActiveCalls(context)) {
            if (existing.getCallId().equals(call.getCallId())) {
                array.put(call.toJson());
                inserted = true;
            } else {
                array.put(existing.toJson());
            }
        }

        if (!inserted) {
            array.put(call.toJson());
        }

        writeArray(context, KEY_ACTIVE_CALLS, array);
    }

    public static synchronized void removeActiveCall(final Context context, final String callId) {
        final JSONArray array = new JSONArray();

        for (IncomingCallRecord existing : getActiveCalls(context)) {
            if (!existing.getCallId().equals(callId)) {
                array.put(existing.toJson());
            }
        }

        writeArray(context, KEY_ACTIVE_CALLS, array);
    }

    public static synchronized void queueEvent(final Context context, final String eventName, final JSObject payload) {
        final JSONArray events = readArray(context, KEY_PENDING_EVENTS);
        final JSONObject object = new JSONObject();
        try {
            object.put("eventName", eventName);
            object.put("payload", new JSONObject(payload.toString()));
            events.put(object);
            writeArray(context, KEY_PENDING_EVENTS, events);
        } catch (JSONException ignored) {}
    }

    public static synchronized List<QueuedEvent> drainPendingEvents(final Context context) {
        final JSONArray array = readArray(context, KEY_PENDING_EVENTS);
        final List<QueuedEvent> events = new ArrayList<>();

        writeArray(context, KEY_PENDING_EVENTS, new JSONArray());

        for (int index = 0; index < array.length(); index++) {
            final JSONObject object = array.optJSONObject(index);
            if (object == null) {
                continue;
            }

            final String eventName = object.optString("eventName", null);
            final JSONObject payload = object.optJSONObject("payload");
            if (eventName == null || payload == null) {
                continue;
            }

            events.add(new QueuedEvent(eventName, toJSObject(payload)));
        }

        return events;
    }

    private static SharedPreferences prefs(final Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static JSONArray readArray(final Context context, final String key) {
        final String raw = prefs(context).getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private static void writeArray(final Context context, final String key, final JSONArray array) {
        prefs(context).edit().putString(key, array.toString()).apply();
    }

    private static JSObject toJSObject(final JSONObject jsonObject) {
        final JSObject object = new JSObject();
        final Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            object.put(key, jsonObject.opt(key));
        }
        return object;
    }
}
