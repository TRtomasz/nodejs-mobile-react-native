package com.janeasystems.rn_nodejs_mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

public class NodeService extends Service {

    private PowerManager.WakeLock mWakeLock = null;
    private boolean mIsServiceStarted = false;
    private Thread mNodeThread = null;

    static {
        System.loadLibrary("nodejs-mobile-react-native-native-lib");
        System.loadLibrary("node");
    }

    public native Integer startNodeWithArguments(String[] arguments, String modulesPath, boolean option_redirectOutputToLogcat);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NODEJS", "stcommand");
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case "START":
                    startService();
                    break;
                case "STOP":
                    stopService();
                    break;
                default:
                    break;
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("NODEJS", "create");
        startForeground((int)(System.currentTimeMillis()%10000), createNotification());
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("NODEJS", "removed");
        super.onTaskRemoved(rootIntent);
        Intent intent = new Intent(this, NodeService.class);
        intent.setAction("STOP");
        startService(intent);
        System.exit(0);
    }



    private void startService() {
        Log.i("NODEJS", "start");
        if (mIsServiceStarted) return;
        final String nodeDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-project";
        final String modulesDir = getApplicationContext().getFilesDir().getAbsolutePath() + "/nodejs-builtin_modules";
        mIsServiceStarted = true;
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NodeService::lock");
        mWakeLock.acquire();
        mNodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startNodeWithArguments(new String[]{"node",
                                nodeDir + "/streamingServer.js"
                        },
                        nodeDir + ":" + modulesDir,
                        true
                );
            }
        });
        mNodeThread.start();
    }

    private void stopService() {
        try {
            if (mWakeLock != null) {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            //something went wrong;
        }
        mIsServiceStarted = false;
    }


    public Notification createNotification() {
        String notificationChannelId = "Streaming Server Channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(notificationChannelId, "Streaming server notifications channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Streaming Server Channel");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, notificationChannelId) : new Notification.Builder(this);
        return builder
                .setSmallIcon(this.getApplicationInfo().icon)
                .setContentTitle("Stremio Server")
                .setContentText("Stremio server is running")
                .setStyle(new Notification.BigTextStyle()
                        .bigText("Stremio Server is running in the background"))
                .build();

    }

}
