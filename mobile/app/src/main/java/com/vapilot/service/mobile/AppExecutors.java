package com.vapilot.service.mobile;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final ExecutorService IO = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static ExecutorService io() {
        return IO;
    }

    public static void runOnMain(Runnable runnable) {
        MAIN.post(runnable);
    }
}
