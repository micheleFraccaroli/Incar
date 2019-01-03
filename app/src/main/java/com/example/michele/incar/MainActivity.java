package com.example.michele.incar;

import android.Manifest;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button btnGL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGL = (Button)findViewById(R.id.btnGL);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
        btnGL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GPSlocalizator gpsL = new GPSlocalizator(getApplicationContext());
                Location l = gpsL.getlocation();

                if(l != null) {
                    double latitudeGPS = l.getLatitude();
                    double longitudeGPS = l.getLongitude();

                    Toast.makeText(getApplicationContext(),"LAT: " + latitudeGPS + "\nLONG: " + longitudeGPS,Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
