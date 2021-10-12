package gl.cs.observer;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.time.Instant;

public class ForegroundService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "SensorChannelB";
    private SensorManager mSensorManager;

    /*
     * Parameters for Pixel 4A
     */
    private final long NORMAL_DELAY_MIN = 110000;
    private final long NORMAL_DELAY_MAX = 200000;
    private final long UI_DELAY_MAX = 90000;
    private final long UI_DELAY_MIN = 39000;
    private final long GAME_DELAY_MAX = 30000;
    private final long GAME_DELAY_MIN = 18000;
    private final long FASTEST_DELAY_MIN = 1000;
    private final long FASTEST_DELAY_MAX = 15000;
    private final int LONG_SAMPLING_RATE = 10000000;

    final int[] sensors = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_ROTATION_VECTOR
    };

    private long acPreviousTimestamp = 0;
    private long grPreviousTimestamp = 0;
    private long gyPreviousTimestamp = 0;
    private long laPreviousTimestamp = 0;
    private long mfPreviousTimestamp = 0;
    private long rvPreviousTimestamp = 0;

    private long acCurrentFrequency = 0;
    private long grCurrentFrequency = 0;
    private long gyCurrentFrequency = 0;
    private long laCurrentFrequency = 0;
    private long mfCurrentFrequency = 0;
    private long rvCurrentFrequency = 0;

    private long uptime;
    private boolean ready = false;

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, gl.cs.observer.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Listener Service")
                .setContentText("Listener Service Text")
                .setSmallIcon(R.drawable.receivericon)
                .setContentIntent(pendingIntent)
                .build();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Log.d("DATARECEIVER", mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());

        for (int s : sensors) {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(s),
                    this.LONG_SAMPLING_RATE);
        }

        uptime = System.currentTimeMillis();

        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSensorManager != null) {
            for (int s : sensors) {
                mSensorManager.unregisterListener(this,
                        mSensorManager.getDefaultSensor(s));
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SensorChannelB",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private void broadcastReceivedData(String sensor, String delay, String timestamp) {
        for (int s : sensors) {
            mSensorManager.unregisterListener(this,
                    mSensorManager.getDefaultSensor(s));
        }
        Log.d("DATARECEIVER", "Broadcasting...");
        Intent intent = new Intent("receiver-app");
        intent.putExtra("sensor", sensor);
        intent.putExtra("delay", delay);
        intent.putExtra("timestamp", timestamp);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void checkSamplingPeriod(long period, String sensorName) {
        if(ready && period > FASTEST_DELAY_MIN && period <= FASTEST_DELAY_MAX) {
            Log.d("DATARECEIVER", "SENSOR_DELAY_FASTEST!");
            broadcastReceivedData(sensorName, "FASTEST", Instant.now().toString());
        }
        else if (ready && period > GAME_DELAY_MIN && period <= GAME_DELAY_MAX) {
            Log.d("DATARECEIVER", "SENSOR_DELAY_GAME!");
            broadcastReceivedData(sensorName, "GAME", Instant.now().toString());
        }
        else if(ready && period > UI_DELAY_MIN && period <= UI_DELAY_MAX) {
            Log.d("DATARECEIVER", "SENSOR_DELAY_UI!");
            broadcastReceivedData(sensorName, "UI", Instant.now().toString());
        }
        /* Pixel 4A: The 'normal' rates of the
         * gravity, linear acceleration, and rotation vector
         * sensors are actually the maximum supported period;
         * they do not work using our method, so they must
         * be skipped.
         */
        else if(!(sensorName.equals("Gravity") ||
                sensorName.equals("Linear Acceleration") ||
                sensorName.equals("Rotation Vector")) &&
                ready && period > NORMAL_DELAY_MIN && period <= NORMAL_DELAY_MAX) {
            Log.d("DATARECEIVER", "SENSOR_DELAY_NORMAL!");
            Log.d("DATARECEIVER", String.valueOf(period));
            broadcastReceivedData(sensorName, "NORMAL", Instant.now().toString());
        }


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*
         * Compute time between new and previous sensor events.
         *
         * Event timestamps are given in nanoseconds since boot, so
         * we need to convert this into our sampling frequency (in
         * microseconds).
         */
        Log.d("DATARECEIVER", "AC sampling period: " + acCurrentFrequency);
        Log.d("DATARECEIVER", "GR sampling period: " + grCurrentFrequency);
        Log.d("DATARECEIVER", "GY sampling period: " + gyCurrentFrequency);
        Log.d("DATARECEIVER", "LA sampling period: " + laCurrentFrequency);
        Log.d("DATARECEIVER", "MF sampling period: " + mfCurrentFrequency);
        Log.d("DATARECEIVER", "RV sampling period: " + rvCurrentFrequency);
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acCurrentFrequency = (event.timestamp - acPreviousTimestamp) / 1000;
                break;
            case Sensor.TYPE_GRAVITY:
                grCurrentFrequency = (event.timestamp - grPreviousTimestamp) / 1000;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyCurrentFrequency = (event.timestamp - gyPreviousTimestamp) / 1000;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                laCurrentFrequency = (event.timestamp - laPreviousTimestamp) / 1000;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mfCurrentFrequency = (event.timestamp - mfPreviousTimestamp) / 1000;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                rvCurrentFrequency = (event.timestamp - rvPreviousTimestamp) / 1000;
                break;
        }

        checkSamplingPeriod(acCurrentFrequency, "Accelerometer");
        checkSamplingPeriod(grCurrentFrequency, "Gravity");
        checkSamplingPeriod(gyCurrentFrequency, "Gyroscope");
        checkSamplingPeriod(laCurrentFrequency, "Linear Acceleration");
        checkSamplingPeriod(mfCurrentFrequency, "Magnetic Field");
        checkSamplingPeriod(rvCurrentFrequency, "Rotation Vector");

        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acPreviousTimestamp = event.timestamp;
                break;
            case Sensor.TYPE_GRAVITY:
                grPreviousTimestamp = event.timestamp;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyPreviousTimestamp = event.timestamp;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                laPreviousTimestamp = event.timestamp;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mfPreviousTimestamp = event.timestamp;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                rvPreviousTimestamp = event.timestamp;
                break;
        }

        // After first activation, a short warm-up time is required
        // to infer a stable period
        if (!ready && System.currentTimeMillis() - uptime > 1500) {
            ready = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
}