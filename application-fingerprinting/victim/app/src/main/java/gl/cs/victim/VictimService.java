package gl.cs.victim;

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
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class VictimService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int targetSamplingRate;
    private boolean finishedTransmitting = true;

    private void transmission_wait(final int waitLength) {
        SystemClock.sleep(waitLength);
    }

    private void send() {
        // Start transmission
        mSensorManager.unregisterListener(this, mSensor);
        mSensorManager.registerListener(this, mSensor, targetSamplingRate);
        int waitLength = 1000;
        transmission_wait(waitLength);
        // End transmission
        mSensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String chosenSensor = intent.getStringExtra("chosenSensor");
        String chosenConfig = intent.getStringExtra("chosenConfig");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Victim Service")
                .setContentText("Victim Service Text")
                .setSmallIcon(R.drawable.transmittericon)
                .setContentIntent(pendingIntent)
                .build();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        switch(chosenConfig) {
            case "GAME":
                targetSamplingRate = SensorManager.SENSOR_DELAY_GAME;
                break;
            case "UI":
                targetSamplingRate = SensorManager.SENSOR_DELAY_UI;
                break;
            case "NORMAL":
                targetSamplingRate = SensorManager.SENSOR_DELAY_NORMAL;
                break;
            case "FASTEST":
                targetSamplingRate = SensorManager.SENSOR_DELAY_FASTEST;
                break;
        }


        int chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD;
        switch (chosenSensor) {
            case "Magnetic Field":
                chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD;
                break;
            case "Accelerometer":
                chosenSensorID = Sensor.TYPE_ACCELEROMETER;
                break;
            case "Gyroscope":
                chosenSensorID = Sensor.TYPE_GYROSCOPE;
                break;
            case "Gravity":
                chosenSensorID = Sensor.TYPE_GRAVITY;
                break;
            case "Linear Acceleration":
                chosenSensorID = Sensor.TYPE_LINEAR_ACCELERATION;
                break;
            case "Rotation Vector":
                chosenSensorID = Sensor.TYPE_ROTATION_VECTOR;
                break;
            case "proximity":
                chosenSensorID = Sensor.TYPE_PROXIMITY;
                break;
        }

        mSensor = mSensorManager.getDefaultSensor(chosenSensorID);
        startForeground(1, notification);

        // Start new thread for sending data
        if(finishedTransmitting) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    finishedTransmitting = false;
                    send();
                    Log.d("TRANSMITTER", "Finished broadcast!");
                    broadcastFinished();
                }
            };
            t.start();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {   }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {   }
    
    private void broadcastFinished() {
        Log.d("DATATRANSMITTER", "Broadcasting...");
        Intent intent = new Intent("transmitter-app");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        finishedTransmitting = true;
    }
}
