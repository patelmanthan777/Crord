package me.s1rius.noone.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.s1rius.noone.BuildConfig;
import me.s1rius.noone.CaptureHelper;
import me.s1rius.noone.R;
import me.s1rius.noone.Util;
import me.s1rius.noone.ui.widget.CheckableRelativeLayout;

public class MainActivity extends AppCompatActivity {

    private static final short REQUEST_EXTERNAL_STORAGE = 0x1111;
    private static final short REQUEST_AUDIO_RECORD = 0x1100;

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyMMddHHmmss");

    @BindView(R.id.start_capture)
    Button startButton;
    @BindView(R.id.output)
    EditText ouputEditText;
    @BindView(R.id.container)
    ViewGroup container;
    @BindView(R.id.mic)
    CheckableRelativeLayout mic;
    @BindView(R.id.root_dir)
    TextView dir;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.ad_container)
    FrameLayout adContainer;

    String outputName;
    private boolean hasAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        container.requestFocus();
        mic.setOnCheckedChangeListener(new CheckableRelativeLayout.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View view, boolean isChecked) {
                if (isChecked) {
                    int audioStatus = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);
                    if (audioStatus == PackageManager.PERMISSION_GRANTED) {
                    } else {
                        mic.setChecked(false);
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_RECORD);
                    }
                }
                hasAudio = isChecked;

            }
        });
        dir.setText("/sdcard/Movies/crord/");
        AdView mAdView = new AdView(this);
        mAdView.setAdUnitId(BuildConfig.ADID);
        mAdView.setAdSize(AdSize.SMART_BANNER);
        adContainer.addView(mAdView);
        mAdView.setVisibility(BuildConfig.DEBUG ? View.INVISIBLE : View.VISIBLE);
        if (!BuildConfig.DEBUG) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            String versionName = "";
            try {
                versionName = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            WebView webView = new WebView(this);
            webView .loadUrl("file:///android_asset/about.html");
            webView.setWebViewClient(new WebViewClient(){
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name) + " " + versionName)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ouputEditText.setHint(mDateFormat.format(new Date()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @OnClick(R.id.start_capture)
    void startCapture() {

        if (!Util.isAlerWindowPermissionGanted(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.please_enable_draw_over_app_permission)
                    .setMessage(R.string.we_will_use_it_to_build_a_record_control_view)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                Intent settings = new Intent(Settings.ACTION_SETTINGS);
                                startActivity(settings);
                            }
                        }
                    }).create().show();
            return;
        }

        int status = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (status == PackageManager.PERMISSION_GRANTED) {
            CaptureHelper.fireScreenCaptureIntent(this);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        outputName = ouputEditText.getText().toString();
        if (TextUtils.isEmpty(outputName)) {
            outputName = ouputEditText.getHint().toString();
        }
        outputName = outputName + ".mp4";
        if (CaptureHelper.handleActivityResult(this, requestCode, resultCode, data, outputName, hasAudio)) {
            finish();
            return;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CaptureHelper.fireScreenCaptureIntent(this);
            } else {
                Toast.makeText(this, R.string.please_access_storage_permission, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AUDIO_RECORD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mic.setChecked(true);
            } else {
                Toast.makeText(this, R.string.please_enable_mic_permission, Toast.LENGTH_SHORT).show();
            }
        }

    }
}
