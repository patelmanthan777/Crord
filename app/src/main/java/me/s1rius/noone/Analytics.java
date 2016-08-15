package me.s1rius.noone;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.HashMap;
import java.util.Map;

interface Analytics {

    String CATEGORY_RECORDING = "Recording";
    String CATEGORY_CONFIG = "Record Config";

    String VARIBALE_RECORD_CROP = "Record Crop";
    String VARIBAL_RECORD_AUDIO = "Recrod Audio";
    String VARIABLE_RECORDING_LENGTH = "Recording Length";

    void send(String key);
    void send(String key, Map<String, String> params);

    class FabricAnalytics implements Analytics {
        static FabricAnalytics instance;

        private Answers mAnswers;

        public FabricAnalytics(Answers answers) {
            mAnswers = answers;
        }

        public static Analytics getInstance() {
            if (instance == null) {
                instance = new FabricAnalytics(Answers.getInstance());
            }
            return instance;
        }

        @Override
        public void send(String key, Map<String, String> params) {
            CustomEvent customEvent = new CustomEvent(key);
            if (params != null && params.size() > 0) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    customEvent.putCustomAttribute(entry.getKey(), entry.getValue());
                }
            }
            mAnswers.logCustom(customEvent);
        }

        public void send(String key) {
            send(key, null);
        }
    }

    public static class AttrBuilder {
        private Map<String, String> map = new HashMap<>();

        public AttrBuilder set(String key, String value) {
            map.put(key, value);
            return this;
        }

        public Map<String, String> create() {
            return map;
        }
    }
}
