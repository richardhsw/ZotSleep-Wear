package com.example.zotsleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class AwakeActivity extends WearableActivity implements LocationListener {

    private final double HOME_RADIUS = 20; // in meters

    private Animation fadeOut;

    private TextView tv;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private double[] homeLatLong;

    // Google API Key = "AIzaSyAzjrZb2QSHltBZt1tZB7fVEB-P35AVNM8"


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_awake);

        ImageView awakeIB = findViewById(R.id.sleepingIB);
        configureFadeAnimation();
        awakeIB.startAnimation(fadeOut);

        tv = findViewById(R.id.messageTV);

        // Create Fused Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setUpLocationRequest();

        // Build location request client.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setNeedBle(true);

        setUpSettingsClient(builder);

        homeLatLong = getLocationFromAddress();
        System.out.println("Home address = (" + homeLatLong[0] + ", " + homeLatLong[1] + ").");

        getLocationCallback();

        Button stopGeofencing = findViewById(R.id.stopGeofenceB);
        stopGeofencing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    /*
     * PRIVATE HELPER METHODS
     */

    // Sets up the fade in and fade out animation.
    private void configureFadeAnimation() {
        fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(3000);
        fadeOut.setFillAfter(true);
    }

    // Get Lat Long location from String Address.
    private double[] getLocationFromAddress() {
        double[] result = new double[2];
        result[0] = 3.6486731;
        result[1] = -117.8325512;

        return result;
        /*
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        result[0] = (double) sharedPref.getFloat("home_lat", 3.6486731f);
        result[1] = (double) sharedPref.getFloat("home_lng", -117.8325512f);

        return result;
        */
    }

    // Initialize locationRequest with interval time and priority.
    private void setUpLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void setUpSettingsClient(LocationSettingsRequest.Builder builder) {
        System.out.println("Started task");
        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    System.out.println("Location settings satisfied");
                } catch (ApiException exception) {
                    System.out.print("Location settings not satisfied " + exception.toString());
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            System.out.println(" RESOLUTION REQUIRED");
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        AwakeActivity.this,
                                        0);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            System.out.println(" SETTINGS CHANGE UNAVAILABLE");
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    private void getLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                System.out.print("LOCATION DETECTED ");
                for (Location location : locationResult.getLocations()) {
                    System.out.print(location.getLatitude() + ", " + location.getLongitude());
                    double distance = calculateDistance(homeLatLong[0], homeLatLong[1],
                            location.getLatitude(), location.getLongitude());

                    System.out.println("\nDistance = " + distance);
                    tv.setText(Double.toString(distance));
                    if (distance >= HOME_RADIUS) {
                        System.out.println("User left the home");
                        Toast.makeText(AwakeActivity.this, "User left the home", Toast.LENGTH_LONG).show();
                    }
                }
            }

            ;
        };
    }

    // Calculate distance between two (lat, long) locations.
    // Returns distance result in meters and as a double.
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        int R = 6371 * 1000;
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
    /*
     * OVERRIDEN FUNCTIONS
     */

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // request_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        System.out.println("onResume ing");
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */ );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case 0:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        System.out.println("Changes made ok");
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        System.out.println("Changes not made");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onLocationChanged(Location location) {
        int lat = (int) (location.getLatitude());
        int lng = (int) (location.getLongitude());

        System.out.println("New lat: " + lat + "; New lng: " + lng);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }
}