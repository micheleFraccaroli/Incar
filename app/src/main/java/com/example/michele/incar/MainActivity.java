package com.example.michele.incar;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.exit;

public class MainActivity extends AppCompatActivity {

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
        final double ti = 30000.0000;

        final Intent andrAuto = getPackageManager().getLaunchIntentForPackage("com.google.android.projection.gearhead");
        if(andrAuto == null) {
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
                            Toast.makeText(getApplicationContext(),"Ok, Notifications Disabled",Toast.LENGTH_LONG).show();
                        }
                    });
            builder.create().show();
        }

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
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

                        if(l != null) {
                            latitudeGPS[0] = l.getLatitude();
                            longitudeGPS[0] = l.getLongitude();

                            if(check[0]) {
                                lat2[0] = latitudeGPS[0];
                                long2[0] = longitudeGPS[0];
                                check[0] = false;
                            } else {
                                Haversine hrv = new Haversine();
                                res[0] = hrv.distance(lat2[0],long2[0],latitudeGPS[0],longitudeGPS[0]);
                                this.vel = res[0] / ti;
                                if(this.vel >= 10) {
                                    try {
                                        startActivity(andrAuto);
                                    } catch (Exception e){
                                        exit(1);
                                    }
                                }
                                check[0] = true;
                            }
                            if(check[0]) {
                                Toast.makeText(getApplicationContext(),"RES: " + res[0] + "\nL1 = " + lat2[0] +
                                        "\nLO1 = " + long2[0] + "\nL2 = " + latitudeGPS[0] + "\nLO2 = " + longitudeGPS[0] + "\n\nVEL: " + vel,Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        };
        t.schedule(doTask,100, (long) ti);
    }
}
