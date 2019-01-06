package com.example.michele.incar;

import android.Manifest;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //Button btnGL;
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

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
        final Handler handler = new Handler();
        Timer t = new Timer();
        TimerTask doTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {

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
                                check[0] = true;
                            }
                            if(check[0]) {
                                Toast.makeText(getApplicationContext(),"RES: " + res[0] + "\nL1 = " + lat2[0] +
                                        "\nLO1 = " + long2[0] + "\nL2 = " + latitudeGPS[0] + "\nLO2 = " + longitudeGPS[0],Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        };
        t.schedule(doTask,100,30000);
    }
}
