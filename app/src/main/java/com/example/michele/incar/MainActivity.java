package com.example.michele.incar;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
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
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

import org.tensorflow.lite.Interpreter;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private NotificationManager mNotificationManager;
    Interpreter tflite;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final boolean[] check = {true};
        final double[] res = new double[1];
        final double[] longitudeGPS = new double[1];
        final double[] latitudeGPS = new double[1];
        final double[] long2 = new double[1];
        final double[] lat2 = new double[1];
        final double ti = 30000;
        final boolean[] notif = new boolean[1];


        try {
            tflite = new Interpreter(loadModelFile(this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Intent andrAuto = getPackageManager().getLaunchIntentForPackage("com.google.android.projection.gearhead");
        if (andrAuto == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm to open Play Store")
                    .setMessage("If you wish, you can download the \"Android Auto\" app for a better driving experience.\nIf you don't want, I will disable all notification.")
                    .setPositiveButton("Go", new DialogInterface.OnClickListener() {
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
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 123);
        final Handler handler = new Handler();
        Timer t = new Timer();
        TimerTask doTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {

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
                                if (this.vel >= 10) {
                                    try {
                                        startActivity(andrAuto);
                                    } catch (Exception e) {
                                        changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                    }
                                } else {
                                    changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_ALL);
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
        //t.schedule(doTask,100, (long) ti);

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

        String output = (String) inference(toInf);
        Log.d("PREDICTION --------------------------->", output + " auto");
        Toast.makeText(MainActivity.this, "Prediction: " + output + " in auto", Toast.LENGTH_SHORT).show();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public String inference(float[][] toInf) {
        float[][] output = new float[1][2];
        String inference;

        try {
            tflite.run(toInf,output);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(output[0][0] > output[0][1]) {
            //return output[0][0];
            inference = "no";
        } else {
            //return output[0][1];
            inference = "yes";
        }
        return inference;
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
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("acc.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}


