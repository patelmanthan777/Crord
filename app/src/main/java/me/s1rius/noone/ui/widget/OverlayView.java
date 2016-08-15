package me.s1rius.noone.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.s1rius.noone.ui.CropActivity;
import me.s1rius.noone.R;
import me.s1rius.noone.event.CropCancelEvent;
import me.s1rius.noone.event.RecordCancelEvent;
import me.s1rius.noone.event.RecordStartEvent;

import static android.graphics.PixelFormat.TRANSLUCENT;
import static android.text.TextUtils.getLayoutDirectionFromLocale;
import static android.view.ViewAnimationUtils.createCircularReveal;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

@SuppressLint("ViewConstructor") // Lint, in this case, I am smarter than you.
public final class OverlayView extends FrameLayout {
    private static final int COUNTDOWN_DELAY = 1000;
    private static final int NON_COUNTDOWN_DELAY = 500;
    private static final int DURATION_ENTER_EXIT = 300;

    public static OverlayView create(Context context, Listener listener, boolean showCountDown) {
        return new OverlayView(context, listener, showCountDown);
    }

    public static WindowManager.LayoutParams createLayoutParams(Context context) {
        int width = context.getResources().getDimensionPixelSize(R.dimen.overlay_width);
        final WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(width, WRAP_CONTENT, TYPE_SYSTEM_ERROR, FLAG_NOT_FOCUSABLE
                        | FLAG_NOT_TOUCH_MODAL
                        | FLAG_LAYOUT_NO_LIMITS
                        | FLAG_LAYOUT_INSET_DECOR
                        | FLAG_LAYOUT_IN_SCREEN, TRANSLUCENT);
        params.gravity = Gravity.TOP | gravityEndLocaleHack();

        return params;
    }

    @SuppressLint("RtlHardcoded") // Gravity.END is not honored by WindowManager for added views.
    private static int gravityEndLocaleHack() {
        int direction = getLayoutDirectionFromLocale(Locale.getDefault());
        return direction == LAYOUT_DIRECTION_RTL ? Gravity.LEFT : Gravity.RIGHT;
    }

    public interface Listener {
        /**
         * Called when cancel is clicked. This view is unusable once this callback is invoked.
         */
        void onCancel();

        /**
         * Called when start is clicked and it is appropriate to start recording. This view will hide
         * itself completely before invoking this callback.
         */
        void onStart();

        /**
         * Called when stop is clicked. This view is unusable once this callback is invoked.
         */
        void onStop();

        /**
         * Called when the size or layout params of this view have changed and require a relayout.
         */
        void onResize();

        void onCropSelect(boolean isSelected);
    }

    @BindView(R.id.record_overlay_buttons)
    View buttonsView;
    @BindView(R.id.record_overlay_cancel)
    View cancelView;
    @BindView(R.id.record_overlay_start)
    View startView;
    @BindView(R.id.record_crop)
    ToggleImageView cropView;
    @BindView(R.id.record_overlay_stop)
    View stopView;
    @BindView(R.id.record_overlay_recording)
    TextView recordingView;

    @BindDimen(R.dimen.overlay_width)
    int animationWidth;

    private final Listener listener;
    private final boolean showCountDown;

    private OverlayView(Context context, Listener listener, boolean showCountDown) {
        super(context);
        this.listener = listener;
        this.showCountDown = showCountDown;

        inflate(context, R.layout.overlay_view, this);
        ButterKnife.bind(this);

        cropView.setOnCheckedChangeListener(new ToggleImageView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View view, boolean isChecked) {
                if (isChecked) {
                    Intent intent = new Intent(getContext(), CropActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                } else {
                    EventBus.getDefault().post(new CropCancelEvent());
                }
            }
        });

        if (getLayoutDirectionFromLocale(Locale.getDefault()) == LAYOUT_DIRECTION_RTL) {
            animationWidth = -animationWidth; // Account for animating in from the other side of screen.
        }

    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = insets.getSystemWindowInsetTop();

        listener.onResize();

        return insets.consumeSystemWindowInsets();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setTranslationX(animationWidth);
        animate().translationX(0)
                .setDuration(DURATION_ENTER_EXIT)
                .setInterpolator(new DecelerateInterpolator());
    }

    @OnClick(R.id.record_overlay_cancel)
    void onCancelClicked() {
        animate().translationX(animationWidth)
                .setDuration(DURATION_ENTER_EXIT)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post(new RecordCancelEvent());
                        listener.onCancel();
                    }
                });
    }

    @OnClick(R.id.record_overlay_start)
    void onStartClicked() {
        recordingView.setVisibility(VISIBLE);
        int centerX = (int) (startView.getX() + (startView.getWidth() / 2));
        int centerY = (int) (startView.getY() + (startView.getHeight() / 2));
        Animator reveal = createCircularReveal(recordingView, centerX, centerY, 0, getWidth() / 2f);
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                buttonsView.setVisibility(GONE);
            }
        });
        reveal.start();

        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (showCountDown) {
                    showCountDown();
                } else {
                    countdownComplete();
                }
            }
        }, showCountDown ? COUNTDOWN_DELAY : NON_COUNTDOWN_DELAY);
    }


    private void startRecording() {
        recordingView.setVisibility(INVISIBLE);
        stopView.setVisibility(VISIBLE);
        stopView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(@NonNull View v) {
                listener.onStop();
            }
        });
        listener.onStart();
    }

    private void showCountDown() {
        String[] countdown = getResources().getStringArray(R.array.countdown);
        countdown(countdown, 0); // array resource must not be empty
        EventBus.getDefault().post(new RecordStartEvent());
    }

    private void countdownComplete() {
        recordingView.animate()
                .alpha(0)
                .setDuration(COUNTDOWN_DELAY)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        startRecording();
                    }
                });
    }

    private void countdown(final String[] countdownArr, final int index) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                recordingView.setText(countdownArr[index]);
                if (index < countdownArr.length - 1) {
                    countdown(countdownArr, index + 1);
                } else {
                    countdownComplete();
                }
            }
        }, COUNTDOWN_DELAY);
    }
}
