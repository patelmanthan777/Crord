package me.s1rius.noone;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.NonNull;

import java.io.File;

import me.s1rius.ffmpeglib.FFmpegHelper;
import timber.log.Timber;

import static android.app.Notification.PRIORITY_MIN;

public class CaptureService extends Service {

    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_CROP_RECT = "crop_rect";
    private static final String EXTRA_OUTPUT_NAME = "ouput_name";
    private static final String EXTRA_HAS_AUDIO = "has_audio";
    private static final int NOTIFICATION_ID = 99118822;
    public static final int EXTRA_CODE_CROP = 2;

    private boolean running;
    private RecordingSession recordingSession;

    public static Intent newIntent(Context context, int resultCode, Intent data, String outputName, boolean isRecordAudio) {
        Intent intent = new Intent(context, CaptureService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        intent.putExtra(EXTRA_OUTPUT_NAME, outputName);
        intent.putExtra(EXTRA_HAS_AUDIO, isRecordAudio);
        return intent;
    }

    public static Intent newIntent(Context context, Rect rect) {
        Intent intent = new Intent(context, CaptureService.class);
        intent.putExtra(EXTRA_RESULT_CODE, EXTRA_CODE_CROP);
        intent.putExtra(EXTRA_CROP_RECT, rect);
        return intent;
    }

    private final RecordingSession.Listener listener = new RecordingSession.Listener() {
        @Override
        public void onStart() {

            Context context = getApplicationContext();
            String title = context.getString(R.string.notification_recording_title);
            String subtitle = context.getString(R.string.notification_recording_subtitle);

            Notification notification = new Notification.Builder(context) //
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setSmallIcon(R.drawable.ic_laucher_notify)
                    .setColor(context.getResources().getColor(R.color.colorPrimary))
                    .setAutoCancel(true)
                    .setPriority(PRIORITY_MIN)
                    .build();

            Timber.d("Moving service into the foreground with recording notification.");
            startForeground(NOTIFICATION_ID, notification);
        }

        @Override
        public void onStop() {

        }

        @Override
        public void onCrop() {
            new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    publishProgress(null);
                    File temp = new File(params[0]);
                    File outputFile = new File(params[1]);

                    Rect cropRect = recordingSession.getCropRect();

                    String cropCommand = String.format("crop=%d:%d:%d:%d",
                            cropRect.width(),
                            cropRect.height(),
                            cropRect.left,
                            cropRect.top);

                    String[] commands = new StringBuilder()
                            .append("ffmpeg").append(" ")
                            .append("-i").append(" ")
                            .append(temp.getAbsolutePath()).append(" ")
                            .append("-filter:v").append(" ")
                            .append(cropCommand).append(" ")
                            .append("-vcodec libx264 -acodec aac").append(" ")
                            .append(outputFile.getAbsolutePath())
                            .toString()
                            .split(" ");
                    FFmpegHelper.runCommand(commands);

                    if (outputFile.exists() && temp.exists()) {
                        temp.delete();
                        outputFile.renameTo(temp);
                    }
                    return temp.getAbsolutePath();
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);

                    Context context = getApplicationContext();
                    String title = context.getString(R.string.notification_please_wait);
                    String subtitle = context.getString(R.string.notification_crop_processing);
                    Notification notification = new Notification.Builder(context)
                            .setContentTitle(title)
                            .setContentText(subtitle)
                            .setSmallIcon(R.drawable.ic_laucher_notify)
                            .setColor(context.getResources().getColor(R.color.colorPrimary))
                            .setAutoCancel(false)
                            .setProgress(100, 100, true)
                            .build();
                    startForeground(NOTIFICATION_ID, notification);

                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    recordingSession.scanfileAndNotify(s);
                }
            }.execute(recordingSession.getRecordFile(), recordingSession.getTempFile());
        }

        @Override
        public void onEnd() {
            Timber.d("Shutting down.");
            stopSelf();
        }
    };

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        int extraCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        if (running) {
            if (extraCode == EXTRA_CODE_CROP) {
                recordingSession.setCropRect((Rect) intent.getParcelableExtra(EXTRA_CROP_RECT));
            }
            Timber.d("Already running! ");
            return START_NOT_STICKY;
        }
        Timber.d("Starting up!");
        running = true;

        Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (extraCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }
        String ouputName = intent.getStringExtra(EXTRA_OUTPUT_NAME);
        boolean hasAudio = intent.getBooleanExtra(EXTRA_HAS_AUDIO, false);
        recordingSession =
                new RecordingSession(this, listener, extraCode, data, ouputName, hasAudio);
        recordingSession.showOverlay();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        recordingSession.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        throw new AssertionError("Not supported.");
    }
}
