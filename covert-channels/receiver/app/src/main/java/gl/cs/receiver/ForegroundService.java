package gl.cs.receiver;


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

import java.util.ArrayList;
import java.util.Arrays;


public class ForegroundService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "Receiver";
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private ArrayList<Short> secretBufferRaw = new ArrayList<>();
    private ArrayList<Pair<Short,Long>> secretBuffer = new ArrayList<>();

    private final int carrierSamplingRateACGY = 10000;
    private final int targetSamplingRateACGY = 7500;
    private final int syncwordRateACGY = 5000;
    private final int endSamplingRateACGY = 2500;

    private final int carrierSamplingRateGRRV = 20000;
    private final int targetSamplingRateGRRV = 15000;
    private final int syncwordRateGRRV = 10000;
    private final int endSamplingRateGRRV = 5000;

    private final int carrierSamplingRateLA = 50000;
    private final int targetSamplingRateLA = 40000;
    private final int syncwordRateLA = 30000;
    private final int endSamplingRateLA = 20000;

    private final int carrierSamplingRateMF = 25000;
    private final int targetSamplingRateMF = 20000;
    private final int syncwordRateMF = 15000;
    private final int endSamplingRateMF = 10000;

    private int carrierSamplingRate, syncwordRate, targetSamplingRate, endSamplingRate, waitLength;

    private final float epsilon = 0.1f;

    private long waitLengthNs;
    private long timeLastValue = -1;
    private long previousTimestamp = 0L;

    private boolean syncwordReceived = false;
    private boolean completed = false;

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        secretBuffer.clear();
        secretBufferRaw.clear();
        String chosenSensor = intent.getStringExtra("chosenSensor");
        this.waitLength = intent.getIntExtra("customWaitPeriod", 150);
        this.waitLengthNs = this.waitLength * 1000000L;

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, gl.cs.receiver.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Receiver Service Title")
                .setContentText("Receiver Service Text Body")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.receivericon)
                .build();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Log.d("DATARECEIVER", mSensorManager.getSensorList(Sensor.TYPE_ALL).toString());

        int chosenSensorID;
        chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
        switch (chosenSensor) {
            case "magnetometer":
                chosenSensorID = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED;
                this.carrierSamplingRate = this.carrierSamplingRateMF;
                this.syncwordRate = this.syncwordRateMF;
                this.endSamplingRate = this.endSamplingRateMF;
                this.targetSamplingRate = this.targetSamplingRateMF;
                break;
            case "accelerometer":
                chosenSensorID = Sensor.TYPE_ACCELEROMETER;
                this.carrierSamplingRate = this.carrierSamplingRateACGY;
                this.syncwordRate = this.syncwordRateACGY;
                this.endSamplingRate = this.endSamplingRateACGY;
                this.targetSamplingRate = this.targetSamplingRateACGY;
                break;
            case "gyroscope":
                chosenSensorID = Sensor.TYPE_GYROSCOPE;
                this.carrierSamplingRate = this.carrierSamplingRateACGY;
                this.syncwordRate = this.syncwordRateACGY;
                this.endSamplingRate = this.endSamplingRateACGY;
                this.targetSamplingRate = this.targetSamplingRateACGY;
                break;
            case "gravity":
                chosenSensorID = Sensor.TYPE_GRAVITY;
                this.carrierSamplingRate = this.carrierSamplingRateGRRV;
                this.syncwordRate = this.syncwordRateGRRV;
                this.endSamplingRate = this.endSamplingRateGRRV;
                this.targetSamplingRate = this.targetSamplingRateGRRV;
                break;
            case "linear acceleration":
                chosenSensorID = Sensor.TYPE_LINEAR_ACCELERATION;
                this.carrierSamplingRate = this.carrierSamplingRateLA;
                this.syncwordRate = this.syncwordRateLA;
                this.endSamplingRate = this.endSamplingRateLA;
                this.targetSamplingRate = this.targetSamplingRateLA;
                break;
            case "rotation vector":
                chosenSensorID = Sensor.TYPE_ROTATION_VECTOR;
                this.carrierSamplingRate = this.carrierSamplingRateGRRV;
                this.syncwordRate = this.syncwordRateGRRV;
                this.endSamplingRate = this.endSamplingRateGRRV;
                this.targetSamplingRate = this.targetSamplingRateGRRV;
                break;
        }

        mSensor = mSensorManager.getDefaultSensor(chosenSensorID);
        mSensorManager.registerListener(this, mSensor, carrierSamplingRate);
        startForeground(1, notification);
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

    private void broadcastReceivedData() {
        Log.d("DATARECEIVER", "Broadcasting...");
        Intent intent = new Intent("receiver-app");
        intent.putExtra("noBitsReceived", secretBuffer.size());

        for(int i = 0; i < secretBuffer.size(); i++) {
            secretBufferRaw.add(secretBuffer.get(i).getL());
        }

        intent.putExtra("message", Arrays.toString(secretBufferRaw.toArray()));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
        long currentFrequency = (event.timestamp - previousTimestamp) / 1000;
        Log.d("DATARECEIVER", "Current freq.: " + currentFrequency);

        if(!completed && Math.abs(currentFrequency - endSamplingRate) < epsilon*endSamplingRate) {
            Log.d("DATARECEIVER", "Ended!");
            completed = true;
            Log.d("DATARECEIVER", "Received: " + Arrays.toString(secretBuffer.toArray()));
            syncwordReceived = false;
            mSensorManager.unregisterListener(this, mSensor);
            broadcastReceivedData();
        }

        if (syncwordReceived && !completed) {
            if(Math.abs(currentFrequency - carrierSamplingRate) < epsilon*carrierSamplingRate
                    && (event.timestamp - timeLastValue >= waitLengthNs)) {
                Log.d("DATARECEIVER", "Received 0!");
                secretBuffer.add(new Pair((short) 0, event.timestamp));
                timeLastValue = event.timestamp;
                Log.d("DATARECEIVER", "List size: " + secretBuffer.size());
            }
            else if (Math.abs(currentFrequency - targetSamplingRate) < epsilon*targetSamplingRate
                    && (event.timestamp - timeLastValue >= waitLengthNs)) {
                Log.d("DATARECEIVER", "Received 1!");
                secretBuffer.add(new Pair((short) 1, event.timestamp));
                timeLastValue = event.timestamp;
                Log.d("DATARECEIVER", "List size: " + secretBuffer.size());
            }
        }
        else if(Math.abs(currentFrequency - syncwordRate) < epsilon*syncwordRate) {
                // Beginning transmission
                Log.d("DATARECEIVER", "Received syncword!");
                syncwordReceived = true;
        }
        previousTimestamp = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
}