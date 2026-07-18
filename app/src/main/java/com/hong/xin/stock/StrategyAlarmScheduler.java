package com.hong.xin.stock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class StrategyAlarmScheduler {

    private static final int ALARM_REQUEST_CODE_1 = 9001;
    private static final int ALARM_REQUEST_CODE_2 = 9002;

    public static void scheduleAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, StrategyCheckReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi1 = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE_1, intent, flags);
        PendingIntent pi2 = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE_2, intent, flags);

        Calendar cal1 = Calendar.getInstance();
        cal1.set(Calendar.HOUR_OF_DAY, 9);
        cal1.set(Calendar.MINUTE, 50);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        if (cal1.before(Calendar.getInstance())) {
            cal1.add(Calendar.DAY_OF_MONTH, 1);
        }

        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.HOUR_OF_DAY, 14);
        cal2.set(Calendar.MINUTE, 45);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        if (cal2.before(Calendar.getInstance())) {
            cal2.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal1.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pi1);
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pi2);
            }
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal1.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi1);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal2.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi2);
        }
    }

    public static void cancelAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, StrategyCheckReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi1 = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE_1, intent, flags);
        PendingIntent pi2 = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE_2, intent, flags);

        alarmManager.cancel(pi1);
        alarmManager.cancel(pi2);
    }
}
