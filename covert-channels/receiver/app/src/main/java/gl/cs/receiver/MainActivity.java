package gl.cs.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Spinner sensorSpinner;
    private TextView textViewStatus, textViewDataID;
    private EditText customWaitPeriod;
    private int dataID = 0;
    private final String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    private String chosenSensor;
    private SharedPreferences mPrefs;
    private int noBitsReceived;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartService = findViewById(R.id.button);
        Button btnStopService = findViewById(R.id.button2);
        Button btnResetDataID = findViewById(R.id.resetDataID);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        textViewStatus = findViewById(R.id.textView);
        textViewDataID = findViewById(R.id.textViewDataID);
        sensorSpinner = findViewById(R.id.sensorSpinner);
        customWaitPeriod = findViewById(R.id.textFieldWaitPeriod);

        btnStartService.setOnClickListener(v -> startService());
        btnStopService.setOnClickListener(v -> {
            stopService();
            updateStopServiceText();
        });
        btnResetDataID.setOnClickListener(v -> resetDataID());

        mPrefs = getSharedPreferences("receiver-id", Context.MODE_PRIVATE);
        dataID = mPrefs.getInt("dataID", 0);
        textViewDataID.setText(String.valueOf(dataID));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("receiver-app"));
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        chosenSensor = sensorSpinner.getSelectedItem().toString().toLowerCase();

        serviceIntent.putExtra("customWaitPeriod",
                Integer.valueOf(customWaitPeriod.getText().toString()));
        serviceIntent.putExtra("chosenSensor", chosenSensor);

        textViewStatus.setText("Activated receiver signal...");
        sensorSpinner.setEnabled(false);
        customWaitPeriod.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void updateStopServiceText() { textViewStatus.setText("* Status *"); }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
        sensorSpinner.setEnabled(true);
        customWaitPeriod.setEnabled(true);
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void resetDataID() {
        SharedPreferences.Editor ed = mPrefs.edit();
        dataID = 0;
        ed.putInt("dataID", 0);
        ed.apply();
        textViewDataID.setText(String.valueOf(dataID));
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DATARECEIVER", "Got broadcast!");
            String message = intent.getStringExtra("message");
            noBitsReceived = intent.getIntExtra("noBitsReceived", 0);
            textViewStatus.setText("Received " + noBitsReceived + " bits:\n" + message);
            writeToFile(message);
            stopService();
        }
    };

    private void storeDataID() {
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("dataID", this.dataID);
        ed.apply();
    }

    private void writeToFile(String data) {
        try {
            String filePath = dateStr + "_" + chosenSensor + "_" + customWaitPeriod.getText().toString()
                        +  "_receiver.txt";

            Log.d("DATARECEIVER", "Saving to "+ getExternalFilesDir(null) + filePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(
                    new File(getExternalFilesDir(null), filePath), true));
            String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            outputStreamWriter.write(timestamp + ";" + dataID + ";" + data + "\n");
            outputStreamWriter.close();
            dataID++;
            storeDataID();
            textViewDataID.setText(String.valueOf(dataID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onResume() {
        super.onResume();
        mPrefs = getSharedPreferences("receiver-id", Context.MODE_PRIVATE);
        dataID = mPrefs.getInt("dataID", 0);
        textViewDataID.setText(String.valueOf(dataID));
    }

    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("dataID", this.dataID);
        ed.apply();
    }
}