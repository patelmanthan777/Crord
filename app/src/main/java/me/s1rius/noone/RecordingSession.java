package me.s1rius.noone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import me.s1rius.noone.ui.widget.OverlayView;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_VIEW;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.media.MediaRecorder.OutputFormat.MPEG_4;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoSource.SURFACE;
import static android.os.Environment.DIRECTORY_MOVIES;
import static android.widget.Toast.LENGTH_SHORT;

final class RecordingSession {

    private String TAG = RecordingSession.class.getSimpleName();
    static final int NOTIFICATION_ID = 522592;

    public static final String DISPLAY_NAME = "crord";
    private static final String MIME_TYPE = "video/mp4";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMddHHmmss");


    public interface Listener {
        /**
         * Invoked immediately prior to the start of recording.
         */
        void onStart();

        /**
         * Invoked immediately after the end of recording.
         */
        void onStop();

        /**
         * invoded when crop process on
         */
        void onCrop();

        /**
         * Invoked after all work for this session has completed.
         */
        void onEnd();
    }

    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private final Context context;
    private final Listener listener;
    private final int resultCode;
    private final Intent data;
    private final File outputRoot;

    private final NotificationManager notificationManager;
    private final WindowManager windowManager;
    private final MediaProjectionManager projectionManager;

    private OverlayView overlayView;
    private MediaRecorder recorder;
    private MediaProjection projection;
    private VirtualDisplay display;
    private String outputFile;
    private boolean running;
    private boolean hasAudio;
    private Rect mCropRect;
    private String outputName;

    private long startTime;

    RecordingSession(Context context, Listener listener, int resultCode, Intent data, String name, boolean hasAudio) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.resultCode = resultCode;
        this.data = data;
        this.outputName = name;
        this.hasAudio = hasAudio;

        File picturesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        outputRoot = new File(picturesDir, DISPLAY_NAME);

        notificationManager = (NotificationManager) this.context.getSystemService(NOTIFICATION_SERVICE);
        windowManager = (WindowManager) this.context.getSystemService(WINDOW_SERVICE);
        projectionManager = (MediaProjectionManager) this.context.getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    void showOverlay() {

        OverlayView.Listener overlayListener = new OverlayView.Listener() {
            @Override
            public void onCancel() {
                cancelOverlay();
            }

            @Override
            public void onStart() {
                startRecording();
            }

            @Override
            public void onStop() {
                stopRecording();
            }

            @Override
            public void onResize() {
                windowManager.updateViewLayout(overlayView, overlayView.getLayoutParams());
            }

            @Override
            public void onCropSelect(boolean isSelected) {

            }
        };
        overlayView = OverlayView.create(context, overlayListener, true);
        windowManager.addView(overlayView, OverlayView.createLayoutParams(context));
    }

    private void hideOverlay() {
        if (overlayView != null) {
            Timber.d("Removing overlay view from window.");
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private void cancelOverlay() {
        hideOverlay();
        listener.onEnd();
    }

    private RecordingInfo getRecordingInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayDensity = displayMetrics.densityDpi;


        Configuration configuration = context.getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;

        // Get the best camera profile available. We assume MediaRecorder supports the highest.
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = camcorderProfile != null ? camcorderProfile.videoFrameWidth : -1;
        int cameraHeight = camcorderProfile != null ? camcorderProfile.videoFrameHeight : -1;
        int cameraFrameRate = camcorderProfile != null ? camcorderProfile.videoFrameRate : 30;

        int sizePercentage = 100;

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape,
                cameraWidth, cameraHeight, cameraFrameRate, sizePercentage);
    }

    private void startRecording() {

        if (!outputRoot.exists() && !outputRoot.mkdirs()) {
            Toast.makeText(context, "Unable to create output directory.\nCannot record screen.",
                    LENGTH_SHORT).show();
            return;
        }

        RecordingInfo recordingInfo = getRecordingInfo();

        recorder = new MediaRecorder();
        recorder.setVideoSource(SURFACE);
        if (hasAudio) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        recorder.setOutputFormat(MPEG_4);
        recorder.setVideoFrameRate(recordingInfo.frameRate);
        recorder.setVideoEncoder(H264);
        if (hasAudio) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        recorder.setVideoSize(recordingInfo.width, recordingInfo.height);
        recorder.setVideoEncodingBitRate(8 * 1000 * 1000);


        outputFile = new File(outputRoot, this.outputName).getAbsolutePath();

        recorder.setOutputFile(outputFile);

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare MediaRecorder.", e);
        }

        projection = projectionManager.getMediaProjection(resultCode, data);

        Surface surface = recorder.getSurface();
        display =
                projection.createVirtualDisplay(DISPLAY_NAME, recordingInfo.width, recordingInfo.height,
                        recordingInfo.density, VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null);

        recorder.start();
        running = true;
        listener.onStart();
        startTime = System.nanoTime();

        Analytics.FabricAnalytics.getInstance().send(
                Analytics.CATEGORY_CONFIG,
                new Analytics.AttrBuilder()
                    .set(Analytics.VARIBAL_RECORD_AUDIO, String.valueOf(hasAudio))
                    .set(Analytics.VARIBALE_RECORD_CROP, mCropRect == null ?
                            "" : mCropRect.toShortString())
                    .create()
                );
    }

    public void setCropRect(Rect cropRect) {
        this.mCropRect = cropRect;
    }

    public Rect getCropRect() {
        return this.mCropRect;
    }

    public String getRecordFile() {
        return outputFile;
    }

    public String getTempFile() {
        return outputFile.replace(".mp4", "_temp.mp4");
    }

    private void stopRecording() {

        if (!running) {
            throw new IllegalStateException("Not running.");
        }
        running = false;

        hideOverlay();

        boolean propagate = false;
        try {
            // Stop the projection in order to flush everything to the recorder.
            projection.stop();
            // set listener null to prevent crash
            recorder.setOnErrorListener(null);
            recorder.setOnInfoListener(null);
            recorder.setPreviewDisplay(null);
            // Stop the recorder which writes the contents to the file.
            recorder.stop();

            propagate = true;
        } catch (RuntimeException e) {
            listener.onEnd();
        } finally {
            try {
                // Ensure the listener can tear down its resources regardless if stopping crashes.
                listener.onStop();
            } catch (RuntimeException e) {
                if (propagate) {
                    //noinspection ThrowFromFinallyBlock
                    throw e; // Only allow listener exceptions to propagate if stopped successfully.
                }
            }
        }

        recorder.release();
        display.release();

        if (mCropRect == null || mCropRect.isEmpty()) {
            scanfileAndNotify(outputFile);
        } else {
            listener.onCrop();
        }

        long duration = System.nanoTime() - startTime;
        Analytics.FabricAnalytics.getInstance().send(
                Analytics.CATEGORY_RECORDING,
                new Analytics.AttrBuilder()
                        .set(Analytics.VARIABLE_RECORDING_LENGTH,
                                TimeUnit.NANOSECONDS.toSeconds(duration)+ "seconds")
                        .create());
    }

    public void scanfileAndNotify(String fileName) {
        MediaScannerConnection.scanFile(context, new String[]{fileName}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, final Uri uri) {
                        if (uri == null) throw new NullPointerException("uri == null");
                        mainThread.post(new Runnable() {
                            @Override
                            public void run() {
                                showNotification(uri, null);
                            }
                        });
                    }
                });
    }

    private void showNotification(final Uri uri, Bitmap bitmap) {
        Intent viewIntent = new Intent(ACTION_VIEW, uri);
        PendingIntent pendingViewIntent =
                PendingIntent.getActivity(context, 0, viewIntent, FLAG_CANCEL_CURRENT);

        Intent shareIntent = new Intent(ACTION_SEND);
        shareIntent.setType(MIME_TYPE);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent = Intent.createChooser(shareIntent, null);
        PendingIntent pendingShareIntent =
                PendingIntent.getActivity(context, 0, shareIntent, FLAG_CANCEL_CURRENT);

        Intent deleteIntent = new Intent(context, DeleteRecordingBroadcastReceiver.class);
        deleteIntent.setData(uri);
        PendingIntent pendingDeleteIntent =
                PendingIntent.getBroadcast(context, 0, deleteIntent, FLAG_CANCEL_CURRENT);

        CharSequence title = context.getText(R.string.notification_captured_title);
        CharSequence subtitle = context.getText(R.string.notification_captured_subtitle);
        CharSequence share = context.getText(R.string.notification_captured_share);
        CharSequence delete = context.getText(R.string.notification_captured_delete);
        Notification.Builder builder = new Notification.Builder(context) //
                .setContentTitle(title)
                .setContentText(subtitle)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_laucher_notify)
                .setContentIntent(pendingViewIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, share, pendingShareIntent)
                .addAction(R.drawable.ic_delete_white_24dp, delete, pendingDeleteIntent);

        if (bitmap != null) {
            builder.setLargeIcon(createSquareBitmap(bitmap))
                    .setStyle(new Notification.BigPictureStyle() //
                            .setBigContentTitle(title) //
                            .setSummaryText(subtitle) //
                            .bigPicture(bitmap));
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());

        if (bitmap != null) {
            listener.onEnd();
            return;
        }

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(@NonNull Void... none) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, uri);
                return retriever.getFrameAtTime();
            }

            @Override
            protected void onPostExecute(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    showNotification(uri, bitmap);
                } else {
                    listener.onEnd();
                }
            }
        }.execute();
    }

    static RecordingInfo calculateRecordingInfo(int displayWidth, int displayHeight,
                                                int displayDensity, boolean isLandscapeDevice, int cameraWidth, int cameraHeight,
                                                int cameraFrameRate, int sizePercentage) {
        // Scale the display size before any maximum size calculations.
        displayWidth = displayWidth * sizePercentage / 100;
        displayHeight = displayHeight * sizePercentage / 100;

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the display size.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        int frameWidth = isLandscapeDevice ? cameraWidth : cameraHeight;
        int frameHeight = isLandscapeDevice ? cameraHeight : cameraWidth;
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire display. Use exact values.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight;
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth;
        }
        return new RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity);
    }

    static final class RecordingInfo {
        final int width;
        final int height;
        final int frameRate;
        final int density;

        RecordingInfo(int width, int height, int frameRate, int density) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.density = density;
        }
    }

    private static Bitmap createSquareBitmap(Bitmap bitmap) {
        int x = 0;
        int y = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > height) {
            x = (width - height) / 2;
            //noinspection SuspiciousNameCombination
            width = height;
        } else {
            y = (height - width) / 2;
            //noinspection SuspiciousNameCombination
            height = width;
        }
        return Bitmap.createBitmap(bitmap, x, y, width, height, null, true);
    }

    void destroy() {
        Log.i(TAG, "destroy");
        if (running) {
            stopRecording();
        }
    }
}
