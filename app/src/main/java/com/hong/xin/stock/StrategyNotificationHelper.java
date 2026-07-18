package com.hong.xin.stock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.hong.xin.stock.data.model.Strategy;

public class StrategyNotificationHelper {

    private static final String CHANNEL_ID = "strategy_alert_channel";
    private static final String CHANNEL_NAME = "策略提醒";
    private static int notifyId = 1000;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("策略条件触发提醒");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void notifyStrategyMatched(Context context, Strategy strategy) {
        createChannel(context);

        Intent intent = new Intent(context, StockDetailActivity.class);
        intent.putExtra("code", strategy.getStockCode());
        intent.putExtra("name", strategy.getStockName());
        intent.putExtra("type", "");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, strategy.getStockCode().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = strategy.getStockName() + "(" + strategy.getStockCode() + ")";
        String content = "策略【" + strategy.getName() + "】已触发";
        if (strategy.getConditionText() != null && !strategy.getConditionText().isEmpty()) {
            content += "\n" + strategy.getConditionText();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(notifyId++, builder.build());
        }
    }
}
