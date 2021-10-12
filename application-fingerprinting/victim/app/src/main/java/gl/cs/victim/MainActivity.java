package gl.cs.victim;

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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Button btnStartService, btnStopService, btnResetDataID;
    Spinner periodSpinner, sensorSpinner;
    TextView textViewStatus, textViewDataID;
    ProgressBar progressBar;
    private int dataID = 0;
    String dateStr = new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault()).format(new Date());
    String chosenSensor, chosenConfig;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStartService = findViewById(R.id.buttonStartService);
        btnStopService = findViewById(R.id.buttonStopService);
        btnResetDataID = findViewById(R.id.buttonResetDataID);

        periodSpinner = findViewById(R.id.configSpinner);
        sensorSpinner = findViewById(R.id.sensorSpinner);

        textViewStatus = findViewById(R.id.textViewStatus);
        textViewDataID = findViewById(R.id.textViewDataID);
        progressBar = findViewById(R.id.progressBar);
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
        btnStartService.setEnabled(false);
        chosenSensor = sensorSpinner.getSelectedItem().toString();
        chosenConfig = periodSpinner.getSelectedItem().toString();

        Intent serviceIntent = new Intent(this, VictimService.class);
        serviceIntent.putExtra("chosenSensor", chosenSensor);
        serviceIntent.putExtra("chosenConfig", chosenConfig);
        progressBar.setVisibility(View.VISIBLE);
        writeToFile();
        startForegroundService(serviceIntent);
    }

    public void stopService() {
        Log.d("TRANSMITTER", "Test...");
        Intent serviceIntent = new Intent(this, VictimService.class);
        progressBar.setVisibility(View.INVISIBLE);
        stopService(serviceIntent);
    }

    private void showCompletedMsg() {
        Toast.makeText(this, "Completed transmission!", Toast.LENGTH_LONG).show();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showCompletedMsg();
            btnStartService.setEnabled(true);
            progressBar.setVisibility(View.INVISIBLE);
        }
    };

    private void writeToFile() {
        try {
            String filePath = dateStr + "_transmitter.txt";
            Log.d("DATATRANSMITTER", "Saving to "+ getExternalFilesDir(null) + filePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(
                    new File(getExternalFilesDir(null), filePath), true));
            outputStreamWriter.write(Instant.now().toString() + ";" + dataID + ";" +
                    chosenSensor + ";" + chosenConfig + "\n");
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

