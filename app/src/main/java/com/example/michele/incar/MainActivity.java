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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import static java.lang.System.exit;


public class MainActivity extends AppCompatActivity {
    private NotificationManager mNotificationManager;

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
        Button btn;

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

        btn = (Button) findViewById(R.id.button_setting);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SetActivity.class));
            }
        });

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

                                // TIME CALC -------

                                double sec = ti / 1000;
                                double min = sec / 60;
                                double hours = sec / 3600;

                                // -----------------

                                this.vel = res[0] / hours;
                                if(this.vel >= 10) {
                                    try {
                                        startActivity(andrAuto);
                                    } catch (Exception e){
                                        changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                                    }
                                } else {
                                    changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_ALL);
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

    protected void changeInterruptionFiler(int interruptionFilter){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){ // If api level minimum 23
            /*
                boolean isNotificationPolicyAccessGranted ()
                    Checks the ability to read/modify notification policy for the calling package.
                    Returns true if the calling package can read/modify notification policy.
                    Request policy access by sending the user to the activity that matches the
                    system intent action ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.

                    Use ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED to listen for
                    user grant or denial of this access.

                Returns
                    boolean

            */
            // If notification policy access granted for this package
            if(mNotificationManager.isNotificationPolicyAccessGranted()){
                /*
                    void setInterruptionFilter (int interruptionFilter)
                        Sets the current notification interruption filter.

                        The interruption filter defines which notifications are allowed to interrupt
                        the user (e.g. via sound & vibration) and is applied globally.

                        Only available if policy access is granted to this package.

                    Parameters
                        interruptionFilter : int
                        Value is INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_PRIORITY,
                        INTERRUPTION_FILTER_ALARMS, INTERRUPTION_FILTER_ALL
                        or INTERRUPTION_FILTER_UNKNOWN.
                */

                // Set the interruption filter
                mNotificationManager.setInterruptionFilter(interruptionFilter);
            }else {
                /*
                    String ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        Activity Action : Show Do Not Disturb access settings.
                        Users can grant and deny access to Do Not Disturb configuration from here.

                    Input : Nothing.
                    Output : Nothing.
                    Constant Value : "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"
                */
                // If notification policy access not granted for this package
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
        }
    }
}


