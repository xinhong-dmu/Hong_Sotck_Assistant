package com.hong.xin.stock.util;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebugLogger {

    private static final String TAG = "HongStock";
    private static File logFile;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static void init(Context context) {
        File cacheDir = context.getCacheDir();
        logFile = new File(cacheDir, "debug_log.txt");
        i(TAG, "Logger initialized, log file: " + logFile.getAbsolutePath());
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
        writeToFile("D", tag, message);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("I", tag, message);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        writeToFile("W", tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        writeToFile("E", tag, message + " - " + Log.getStackTraceString(throwable));
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("E", tag, message);
    }

    private static synchronized void writeToFile(String level, String tag, String message) {
        if (logFile == null) return;
        try (FileWriter fw = new FileWriter(logFile, true)) {
            String line = String.format("[%s] %s/%s: %s%n", DATE_FMT.format(new Date()), level, tag, message);
            fw.write(line);
        } catch (IOException ignored) {
        }
    }
}
