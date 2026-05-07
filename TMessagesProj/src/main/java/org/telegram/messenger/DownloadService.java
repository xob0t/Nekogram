/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.ui.LaunchActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import tw.nekomimi.nekogram.NekoConfig;

public class DownloadService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private static final int NOTIFICATION_ID = 7;
    private static DownloadService instance;

    private NotificationCompat.Builder builder;
    private final HashMap<String, long[]> progressByFile = new HashMap<>();

    public static void start() {
        if (instance != null) {
            instance.updateNotification();
            return;
        }
        try {
            Intent intent = new Intent(ApplicationLoader.applicationContext, DownloadService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ApplicationLoader.applicationContext.startForegroundService(intent);
            } else {
                ApplicationLoader.applicationContext.startService(intent);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void stop() {
        if (instance != null) {
            instance.stopSelf();
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.onDownloadingFilesChanged);
            NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.fileLoadFailed);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            stopForeground(true);
        } catch (Throwable ignore) {

        }
        try {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(NOTIFICATION_ID);
        } catch (Throwable ignore) {

        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.onDownloadingFilesChanged);
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.fileLoadFailed);
        }
        progressByFile.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureBuilder();
        updateBuilderContent();
        try {
            startForegroundCompat();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        if (!hasDownloads()) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoadProgressChanged) {
            String fileName = (String) args[0];
            if (isDownloadingFile(fileName)) {
                progressByFile.put(fileName, new long[]{(Long) args[1], (Long) args[2]});
                updateNotification();
            }
        } else if (id == NotificationCenter.fileLoaded || id == NotificationCenter.fileLoadFailed) {
            progressByFile.remove((String) args[0]);
            updateNotification();
        } else if (id == NotificationCenter.onDownloadingFilesChanged) {
            updateNotification();
        }
    }

    private boolean hasDownloads() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && !DownloadController.getInstance(a).downloadingFiles.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isDownloadingFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            for (MessageObject messageObject : DownloadController.getInstance(a).downloadingFiles) {
                if (fileName.equals(messageObject.getFileName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getDownloadsCount() {
        int count = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                count += DownloadController.getInstance(a).downloadingFiles.size();
            }
        }
        return count;
    }

    private HashSet<String> getDownloadingFileNames() {
        HashSet<String> fileNames = new HashSet<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                continue;
            }
            for (MessageObject messageObject : DownloadController.getInstance(a).downloadingFiles) {
                String fileName = messageObject.getFileName();
                if (fileName != null) {
                    fileNames.add(fileName);
                }
            }
        }
        return fileNames;
    }

    private int getProgress(HashSet<String> fileNames) {
        long loaded = 0;
        long total = 0;
        for (String fileName : fileNames) {
            long[] progress = progressByFile.get(fileName);
            if (progress != null && progress[1] > 0) {
                loaded += progress[0];
                total += progress[1];
            }
        }
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) (loaded * 100 / total)));
    }

    private void ensureBuilder() {
        if (builder != null) {
            return;
        }
        NotificationsController.checkOtherNotificationsChannel();
        Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ApplicationLoader.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        builder.setSmallIcon(android.R.drawable.stat_sys_download);
        builder.setWhen(System.currentTimeMillis());
        builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        builder.setContentTitle(LocaleController.getString(R.string.AppName));
        builder.setColor(NekoConfig.getNotificationColor());
        builder.setCategory(NotificationCompat.CATEGORY_PROGRESS);
        builder.setOnlyAlertOnce(true);
        builder.setOngoing(true);
        builder.setContentIntent(pendingIntent);
    }

    private void startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private void updateBuilderContent() {
        int count = getDownloadsCount();
        HashSet<String> fileNames = getDownloadingFileNames();
        Iterator<String> iterator = progressByFile.keySet().iterator();
        while (iterator.hasNext()) {
            if (!fileNames.contains(iterator.next())) {
                iterator.remove();
            }
        }
        int progress = getProgress(fileNames);
        builder.setContentText(count == 1 ? LocaleController.formatString(R.string.AppUpdateDownloading, progress) : LocaleController.getString(R.string.SaveToDownloads));
        builder.setProgress(100, progress, progress == 0);
    }

    private void updateNotification() {
        if (!hasDownloads()) {
            try {
                stopForeground(true);
            } catch (Throwable ignore) {

            }
            try {
                NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(NOTIFICATION_ID);
            } catch (Throwable ignore) {

            }
            stopSelf();
            return;
        }
        ensureBuilder();
        updateBuilderContent();
        try {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(NOTIFICATION_ID, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
