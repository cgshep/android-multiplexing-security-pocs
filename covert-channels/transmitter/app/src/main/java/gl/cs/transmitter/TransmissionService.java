package gl.cs.transmitter;

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

import java.util.Arrays;

public class TransmissionService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private final int targetSamplingRateACGY = 7500;
    private final int syncwordRateACGY = 5000;
    private final int endSamplingRateACGY = 2500;

    private final int targetSamplingRateGRRV = 15000;
    private final int syncwordRateGRRV = 10000;
    private final int endSamplingRateGRRV = 5000;

    private final int targetSamplingRateLA = 40000;
    private final int syncwordRateLA = 30000;
    private final int endSamplingRateLA = 20000;

    private final int targetSamplingRateMF = 20000;
    private final int syncwordRateMF = 15000;
    private final int endSamplingRateMF = 10000;

    private int syncwordRate, targetSamplingRate, endSamplingRate, waitLength;

    private int[] secretData;
    private boolean finishedTransmitting = true;

    private void transmission_wait(final int waitLength) {
        SystemClock.sleep(waitLength);
    }

    private void encode() {
        // Syncword: single bit at the target frequency
        Log.d("DATATRANSMITTER", "Sending syncword...");
        mSensorManager.registerListener(this, mSensor, syncwordRate);
        transmission_wait(waitLength);
        mSensorManager.unregisterListener(this, mSensor);
        Log.d("DATATRANSMITTER", "Finished syncword!");
        for (int b : secretData) {
            if (b == 0) {
                Log.d("DATATRANSMITTER", "Transmitting 0...");
                mSensorManager.unregisterListener(this, mSensor);
                transmission_wait(waitLength);
            } else if (b == 1) {
                Log.d("DATATRANSMITTER", "Transmitting 1...");
                mSensorManager.registerListener(this, mSensor, targetSamplingRate);
                transmission_wait(waitLength);
            }
        }

        // End transmission
        mSensorManager.unregisterListener(this, mSensor);
        mSensorManager.registerListener(this, mSensor, endSamplingRate);
        Log.d("DATATRANSMITTER", "Ending transmission...");
        transmission_wait(waitLength);

        // Finish
        mSensorManager.unregisterListener(this, mSensor);
    }

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String chosenSensor = intent.getStringExtra("chosenSensor");
        this.waitLength = intent.getIntExtra("customWaitPeriod", 0);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Transmission Service Title")
                .setContentText("Transmission Service Text Body")
                .setSmallIcon(R.drawable.transmittericon)
                .setContentIntent(pendingIntent)
                .build();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        secretData = intent.getIntArrayExtra("secretData");
        Log.d("DATATRANSMITTER", "secretData: " + Arrays.toString(secretData));

        int chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
        switch (chosenSensor) {
            case "magnetometer":
                chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
                this.targetSamplingRate = this.targetSamplingRateMF;
                this.syncwordRate = this.syncwordRateMF;
                this.endSamplingRate = this.endSamplingRateMF;
                break;
            case "accelerometer":
                chosenSensorID = Sensor.TYPE_ACCELEROMETER;
                this.targetSamplingRate = this.targetSamplingRateACGY;
                this.syncwordRate = this.syncwordRateACGY;
                this.endSamplingRate = this.endSamplingRateACGY;
                break;
            case "gyroscope":
                chosenSensorID = Sensor.TYPE_GYROSCOPE;
                this.targetSamplingRate = this.targetSamplingRateACGY;
                this.syncwordRate = this.syncwordRateACGY;
                this.endSamplingRate = this.endSamplingRateACGY;
                break;
            case "gravity":
                chosenSensorID = Sensor.TYPE_GRAVITY;
                this.targetSamplingRate = this.targetSamplingRateGRRV;
                this.syncwordRate = this.syncwordRateGRRV;
                this.endSamplingRate = this.endSamplingRateGRRV;
                break;
            case "linear acceleration":
                chosenSensorID = Sensor.TYPE_LINEAR_ACCELERATION;
                this.targetSamplingRate = this.targetSamplingRateLA;
                this.syncwordRate = this.syncwordRateLA;
                this.endSamplingRate = this.endSamplingRateLA;
                break;
            case "rotation vector":
                chosenSensorID = Sensor.TYPE_ROTATION_VECTOR;
                this.targetSamplingRate = this.targetSamplingRateGRRV;
                this.syncwordRate = this.syncwordRateGRRV;
                this.endSamplingRate = this.endSamplingRateGRRV;
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
                    encode();
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
