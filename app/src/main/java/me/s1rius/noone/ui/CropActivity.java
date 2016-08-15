package me.s1rius.noone.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.s1rius.noone.CaptureService;
import me.s1rius.noone.R;
import me.s1rius.noone.event.CropCancelEvent;
import me.s1rius.noone.event.RecordStartEvent;
import me.s1rius.noone.ui.widget.HighlightView;

public class CropActivity extends AppCompatActivity {


    @BindView(R.id.crop_highlight)
    HighlightView highlightView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Subscribe
    public void onCropCancelEvent(CropCancelEvent obj) {
        startService(CaptureService.newIntent(this, null));
        finish();
    }

    @Subscribe
    public void onRecordStartEvent(RecordStartEvent recordStartEvent) {
        startService(CaptureService.newIntent(this, highlightView.getCropedRect()));
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
