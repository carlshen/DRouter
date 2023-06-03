package com.didi.drouter.router;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class ActivityCallback implements RouterCallback {
    public @Override void onResult(@NonNull Result result) {}
    public abstract void onActivityResult(int resultCode, @Nullable Intent data);
}
