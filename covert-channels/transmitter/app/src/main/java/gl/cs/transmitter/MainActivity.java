package gl.cs.transmitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button btnStartService, btnStopService, btnResetDataID;
    Spinner bitsSpinner, sensorSpinner;
    TextView textViewStatus, textViewDataID;
    EditText customWaitPeriod;
    ProgressBar progressBar;
    private int dataID = 0;
    private int[] data;
    String dateStr = new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault()).format(new Date());
    String timestamp, chosenSensor, bitsChoice;
    private short noBitsToSend;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStartService = findViewById(R.id.buttonStartService);
        btnStopService = findViewById(R.id.buttonStopService);
        btnResetDataID = findViewById(R.id.buttonResetDataID);
        bitsSpinner = findViewById(R.id.bitsSpinner);
        sensorSpinner = findViewById(R.id.sensorSpinner);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewDataID = findViewById(R.id.textViewDataID);
        progressBar = findViewById(R.id.progressBar);
        customWaitPeriod = findViewById(R.id.textFieldWaitPeriod);

        progressBar.setVisibility(View.INVISIBLE);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });
        btnResetDataID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDataID();
            }
        });

        mPrefs = getSharedPreferences("transmitter-id", Context.MODE_PRIVATE);
        dataID = mPrefs.getInt("dataID", 0);
        textViewDataID.setText(String.valueOf(dataID));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("transmitter-app"));
    }

    protected void onResume() {
        super.onResume();
        mPrefs = getSharedPreferences("transmitter-id", Context.MODE_PRIVATE);
        dataID = mPrefs.getInt("dataID", 0);
        textViewDataID.setText(String.valueOf(dataID));
    }

    public void resetDataID() {
        SharedPreferences.Editor ed = mPrefs.edit();
        dataID = 0;
        ed.putInt("dataID", 0);
        ed.apply();
        textViewDataID.setText(String.valueOf(dataID));
    }

    private void storeDataID() {
      SharedPreferences.Editor ed = mPrefs.edit();
      ed.putInt("dataID", this.dataID);
      ed.apply();
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, TransmissionService.class);
        btnStartService.setEnabled(false);
        bitsChoice = bitsSpinner.getSelectedItem().toString();
        chosenSensor = sensorSpinner.getSelectedItem().toString().toLowerCase();

        Log.d("DATATRANSMITTER", "Sending # bits: " + noBitsToSend);
        noBitsToSend = Short.parseShort(bitsChoice);
        data = new int[noBitsToSend];
        for (int i = 0; i < noBitsToSend; i++) {
            data[i] = (int) Math.round(Math.random());
        }
        serviceIntent.putExtra("secretData", data);
        Log.d("DATATRANSMITTER", "Sending: " + Arrays.toString(data));
        textViewStatus.setText("Sending " + Arrays.toString(data));

        serviceIntent.putExtra("chosenSensor", chosenSensor);
        serviceIntent.putExtra("customWaitPeriod", Integer.valueOf(customWaitPeriod.getText().toString()));
        
        customWaitPeriod.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        startForegroundService(serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, TransmissionService.class);
        progressBar.setVisibility(View.INVISIBLE);
        stopService(serviceIntent);
        customWaitPeriod.setEnabled(true);
        btnStartService.setEnabled(true);
    }

    private void showCompletedMsg() {
        Toast.makeText(this, "Completed transmission!", Toast.LENGTH_LONG).show();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showCompletedMsg();
            writeToFile(Arrays.toString(data));
            btnStartService.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
            customWaitPeriod.setEnabled(true);
        }
    };

    private void writeToFile(String data) {
        try {
            String filePath = dateStr + "_" + chosenSensor + "_"
                    + customWaitPeriod.getText().toString()
                    +  "_transmitter.txt";

            Log.d("DATATRANSMITTER", "Saving to "+ getExternalFilesDir(null) + filePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(
                    new File(getExternalFilesDir(null), filePath), true));
            timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            outputStreamWriter.write(timestamp + ";" + String.valueOf(dataID) + ";" + data + "\n");
            outputStreamWriter.close();
            dataID++;
            storeDataID();
            textViewDataID.setText(String.valueOf(dataID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("dataID", this.dataID);
        ed.apply();
    }
}

