package me.s1rius.noone;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import timber.log.Timber;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

public final class CaptureHelper {
    public static final int CREATE_SCREEN_CAPTURE = 4242;

    private CaptureHelper() {
        throw new AssertionError("No instances.");
    }

    public static void fireScreenCaptureIntent(Activity activity) {
        MediaProjectionManager manager =
                (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = manager.createScreenCaptureIntent();
        activity.startActivityForResult(intent, CREATE_SCREEN_CAPTURE);
    }

    public static boolean handleActivityResult(Activity activity,
                                        int requestCode,
                                        int resultCode,
                                        Intent data,
                                        String ouputName,
                                        boolean hasAudio) {
        if (requestCode != CREATE_SCREEN_CAPTURE) {
            return false;
        }

        if (resultCode == Activity.RESULT_OK) {
            Timber.d("Acquired permission to screen capture. Starting service.");
            activity.startService(CaptureService.newIntent(activity.getApplicationContext(),
                    resultCode,
                    data,
                    ouputName,
                    hasAudio));
        } else {
            Timber.d("Failed to acquire permission to screen capture.");
            return false;
        }
        return true;
    }
}
