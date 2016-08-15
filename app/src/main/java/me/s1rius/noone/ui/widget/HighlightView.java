package me.s1rius.noone.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

public class HighlightView extends View {
    private static final String TAG = HighlightView.class.getSimpleName();
    private static final boolean DBG = false;
    private HighlightViewImp mHighlightViewimp;
    private Rect mViewRect;
    private RectF mCropRect;
    private float lastX;
    private float lastY;
    private int motionEdge;
    private int validPointerId;


    public HighlightView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public HighlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public HighlightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mHighlightViewimp = new HighlightViewImp(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewRect = new Rect(0, 0, getWidth(), getHeight());

                int cropWidth = Math.min(getWidth(), getHeight()) * 4 / 5;
                @SuppressWarnings("SuspiciousNameCombination")
                int cropHeight = cropWidth;

                int x = (getWidth() - cropWidth) / 2;
                int y = (getHeight() - cropHeight) / 2;

                mCropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
                mHighlightViewimp.setup(mViewRect, mCropRect);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }


    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int edge = mHighlightViewimp.getHit(event.getX(), event.getY());
                if (DBG) Log.i(TAG, edge + "" + "x " + event.getX() + " y " + event.getY());

                if (edge != HighlightViewImp.GROW_NONE) {
                    motionEdge = edge;
                    lastX = event.getX();
                    lastY = event.getY();
                    // Prevent multiple touches from interfering with crop area re-sizing
                    validPointerId = event.getPointerId(event.getActionIndex());
                    mHighlightViewimp.setMode((edge == HighlightViewImp.MOVE)
                            ? HighlightViewImp.ModifyMode.Move
                            : HighlightViewImp.ModifyMode.Grow);
                    break;
                } else {
                    return false;
                }
            case MotionEvent.ACTION_UP:
                if (mHighlightViewimp != null) {
                    mHighlightViewimp.setMode(HighlightViewImp.ModifyMode.None);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mHighlightViewimp != null && event.getPointerId(event.getActionIndex()) == validPointerId) {
                    mHighlightViewimp.handleMotion(motionEdge, event.getX()
                            - lastX, event.getY() - lastY);
                    lastX = event.getX();
                    lastY = event.getY();
                }
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mHighlightViewimp.draw(canvas);
    }


    public Rect getCropedRect() {
        return mHighlightViewimp.getWindowCropRect();
    }
}
