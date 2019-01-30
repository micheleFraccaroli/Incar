package com.example.michele.incar;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.tensorflow.lite.Interpreter;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private NotificationManager mNotificationManager;
    Interpreter tflite;
    SharedPreferences prefs = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Intent andrAuto;
    ArrayList<float[][]> list_toInf = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            tflite = new Interpreter(loadModelFile(this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        andrAuto = getPackageManager().getLaunchIntentForPackage("com.google.android.projection.gearhead");
        prefs = getSharedPreferences("myPref", MODE_PRIVATE);
        if (andrAuto == null) {
            //if(prefs.getBoolean("firstrun", true)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Confirm to open Play Store")
                        .setMessage("If you wish, you can download the \"Android Auto\" app for a better driving experience.\nIf you don't want, I will disable all notification.")
                        .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.projection.gearhead")));
                            }
                        })
                        .setNegativeButton("Disable Notification", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                // Check if the notification policy access has been granted for the app.
                                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                                    startActivity(intent);
                                }
                                //changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_NONE);
                                Toast.makeText(getApplicationContext(), "Ok, I will disable the notifications ", Toast.LENGTH_LONG).show();
                            }
                        });
                builder.create().show();
                //prefs.edit().putBoolean("firstrun", false).commit();
            //}
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener((SensorEventListener) this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener((SensorEventListener) this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener((SensorEventListener) this);
    }

    public void onSensorChanged(SensorEvent event) {
        float[][] toInf = new float[1][3];

        toInf[0][0] = event.values[0];
        toInf[0][1] = event.values[1];
        toInf[0][2] = event.values[2];
        Log.d("--------------------------->", toInf[0][0] + " " + toInf[0][1] + " " + toInf[0][2]);

        list_toInf.add(toInf);
        Log.d("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~>", String.valueOf(list_toInf.size()));
        if(list_toInf.size() == 20) {
            onPause();
            reasoning(list_toInf);
            list_toInf.clear();
        }
    }

    // AI SECTION ---------------------------------------------------------------------------------------------------------------

    public void reasoning(ArrayList<float[][]> list_toInf){
        final boolean[] check = {true};
        final double[] res = new double[1];
        final double[] longitudeGPS = new double[1];
        final double[] latitudeGPS = new double[1];
        final double[] long2 = new double[1];
        final double[] lat2 = new double[1];
        final double ti = 15000;

        TextView textV = (TextView) findViewById(R.id.textView3);
        TextView textT = (TextView) findViewById(R.id.textView4);
        ImageView car = (ImageView) findViewById(R.id.imageView);
        ImageView gps = (ImageView) findViewById(R.id.imageView8);

        andrAuto = getPackageManager().getLaunchIntentForPackage("com.google.android.projection.gearhead");

        final Handler handler = new Handler();
        final Timer t = new Timer();
        TimerTask doTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    ImageView car = (ImageView) findViewById(R.id.imageView);
                    ImageView gps = (ImageView) findViewById(R.id.imageView8);

                    private double vel;

                    @Override
                    public void run() {
                        GPSlocalizator gpsL = new GPSlocalizator(getApplicationContext());
                        Location l = gpsL.getlocation();

                        if (l != null) {
                            latitudeGPS[0] = l.getLatitude();
                            longitudeGPS[0] = l.getLongitude();

                            if (check[0]) {
                                lat2[0] = latitudeGPS[0];
                                long2[0] = longitudeGPS[0];
                                check[0] = false;
                            } else {
                                Haversine hrv = new Haversine();
                                res[0] = hrv.distance(lat2[0], long2[0], latitudeGPS[0], longitudeGPS[0]);

                                // TIME CALC -------

                                double sec = ti / 1000;
                                double min = sec / 60;
                                double hours = sec / 3600;

                                // -----------------

                                this.vel = res[0] / hours;
                                if (this.vel >= 25) {
                                    try {
                                        car.setColorFilter(Color.GREEN);
                                        gps.setColorFilter(Color.GREEN);
                                        startActivity(andrAuto);
                                    } catch (Exception e) {
                                        car.setColorFilter(Color.GREEN);
                                        gps.setColorFilter(Color.GREEN);
                                        changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                    }
                                } else {
                                    changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_ALL);
                                    onResume();
                                    t.cancel();
                                }
                                check[0] = true;
                            }
                            if (check[0]) {
                                Toast.makeText(getApplicationContext(), "RES: " + res[0] + "\nL1 = " + lat2[0] +
                                        "\nLO1 = " + long2[0] + "\nL2 = " + latitudeGPS[0] + "\nLO2 = " + longitudeGPS[0] + "\n\nVEL: " + vel, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        };

        // NEURAL NETWORK INFERENCE ----------------------------------------------------------------

        String output = (String) inference(list_toInf);

        if(output == "yes") {
            Log.d("INTOIFFFF", "nell'if");
            onPause();
            t.schedule(doTask,100, (long) ti);
            //Toast.makeText(MainActivity.this, "Prediction: in auto", Toast.LENGTH_SHORT).show();
            textT.setText("GPS SIGNAL ON");
            textV.setText("MAYBE IN AUTO");
            car.setColorFilter(Color.BLUE);
            gps.setColorFilter(Color.BLUE);
        } else {
            Log.d("INTOIFFFF", "nell'else");
            t.cancel();
            textV.setText("NOT IN AUTO");
            textT.setText("GPS SIGNAL OFF");
            car.setColorFilter(Color.DKGRAY);
            gps.setColorFilter(Color.DKGRAY);
            onResume();
        }

        Log.d("PREDICTION --------------------------->", output + " auto");
    }

    public String inference(ArrayList<float[][]> list_toInf) {
        float[][] output = new float[1][2];
        List<String> list_res = new ArrayList<>();

        for (int i = 0; i <= list_toInf.size(); i++) {
            try {
                tflite.run(list_toInf.get(i),output);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(output[0][0] > output[0][1]) {
                list_res.add("no");
            } else {
                list_res.add("si");
            }
        }

        if(Collections.frequency(list_res,"no") > Collections.frequency(list_res,"yes")) {
            return "no";
        } else {
            return "yes";
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void changeInterruptionFiler(int interruptionFilter){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(mNotificationManager.isNotificationPolicyAccessGranted()){
                mNotificationManager.setInterruptionFilter(interruptionFilter);
            }else {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
        }
    }
    /** Memory-map the model file in Assets. */
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("ANN.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}


