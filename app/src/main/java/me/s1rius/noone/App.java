package me.s1rius.noone;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class App extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Answers(), new Crashlytics());
        Timber.plant(new Timber.DebugTree());
    }
}
