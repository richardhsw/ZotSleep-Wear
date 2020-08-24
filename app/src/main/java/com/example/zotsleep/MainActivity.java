package com.example.zotsleep;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.telecom.ConnectionRequest;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends WearableActivity {

    private final int PERMISSION_CHECK = 0;

    private Animation fadeOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView logoIV = findViewById(R.id.logoIV);
        configureFadeAnimation();
        logoIV.startAnimation(fadeOut);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Check Permissions for app. If permission not granted, request
                // user to allow permissions.
                CheckPermissions();
            }
        };
        handler.postDelayed(runnable, 6000);

        // Check builtin GPS
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            System.out.println("This watch does not have GPS...");
            // Fall back to functionality that does not use location or
            // warn the user that location function is not available.
        }

        // Check Google Play Services Available
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
            == ConnectionResult.SUCCESS) {
            System.out.println("Google Play Services available");
        }
        else {
            System.out.println("Google Play Services no available");
        }

        // Enables Always-on
        setAmbientEnabled();
    }

    private void CheckPermissions() {
        System.out.println("Checking permissions...");

        List<String> request_permissions = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            request_permissions.add(Manifest.permission.BODY_SENSORS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            request_permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            request_permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            request_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            request_permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!request_permissions.isEmpty()) {
            System.out.println("Permission not granted");

            String[] temp = new String[request_permissions.size()];
            request_permissions.toArray(temp);
            ActivityCompat.requestPermissions(this, temp, PERMISSION_CHECK);

        }
        else {
            startMain();
        }
    }

    // Sets up the fade in and fade out animation.
    private void configureFadeAnimation() {
        fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(6000);
        fadeOut.setFillAfter(true);
    }

    private void startMain() {
        // Starts the welcome screen
        Intent intent = new Intent(this, FadeText.class);

        // Time it takes for text to fade in and fade out. Value is in milliseconds
        // (i.e. 1200ms = 1.2s).
        intent.putExtra("fade_duration", 1200);

        // Customize the welcome message and name.
        // FUTURE NOTE: needs to change the name of the welcome page to match the actual user's
        // name.
        intent.putExtra("message", getString(R.string.welcome));

        // Tells which activity the app will jump to after the text fades out.
        intent.putExtra("class", "com.example.zotsleep.SleepPrompt");

        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_CHECK) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMain();
            } else {
                System.out.println("User has denied permissions...");
            }
        }
    }
}
