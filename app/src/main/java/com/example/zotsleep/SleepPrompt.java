package com.example.zotsleep;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.activity.WearableActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
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
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class SleepPrompt extends WearableActivity {

    private Handler handler;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private final int NOTIFICATION_ID = 1;
    private final String NOTIFICATION_CHANNEL = "Sleep Reminder";
    private NotificationManagerCompat notificationManager;
    private NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_prompt);

        startLocationDetection();

        // If the user clicks the yes button, the user
        // is ready to sleep. Jump to sleep tracking activity.
        ImageButton sleep = findViewById(R.id.yesButton);
        sleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                }

                /*
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(SleepPrompt.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                System.out.println("Task success");
                                if (location != null) {
                                    // Logic to handle location object
                                    System.out.println("Current location = " + location.getLongitude() + ", " + location.getLongitude());

                                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putFloat("home_lat", (float) location.getLatitude());
                                    editor.putFloat("home_lng", (float) location.getLongitude());
                                    editor.apply();
                                }
                            }
                        });
                */


                Intent intent = new Intent(SleepPrompt.this, FadeText.class);
                intent.putExtra("fade_duration", 1200);
                intent.putExtra("message", getString(R.string.good_night));
                intent.putExtra("class", "com.example.zotsleep.SleepTracking");
                startActivity(intent);

            }
        });

        // If the user clicks the no button, the user
        // is not ready to sleep yet. Send a notification
        // after 10 minutes.
        // FUTURE NOTE: make minutes customizable.
        ImageButton snooze = findViewById(R.id.noButton);
        snooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Displaying Notification...");

                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ChannelNotification();
                        } else {
                            DefaultNotification();
                        }
                    }
                }, 5000);
            }
        });

        // Enables Always-on
        setAmbientEnabled();
    }

    /*
     * PRIVATE FUNCTIONS TO SET NOTIFICATIONS
     */

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(NOTIFICATION_ID);
        } else {
            notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(NOTIFICATION_ID);
        }

    }

    // Intent when the user clicks on the notification.
    private PendingIntent restartIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    // Create Inline Action for snoozing the notification.
    private PendingIntent snoozeIntent() {
        Intent snoozeIntent = new Intent(this, SleepPrompt.class);
        return PendingIntent.getActivity(this, 0, snoozeIntent, 0);
    }

    // Create a wearable action extender.
    private NotificationCompat.Action.WearableExtender buildActionExtender() {
        return new NotificationCompat.Action.WearableExtender()
                .setHintLaunchesActivity(true)
                .setHintDisplayActionInline(true);
    }

    // Create a action builder that can extened the above wearable action extender.
    // Sets the intent called to snoozeIntent, so that the user can choose to snooze
    // the notification more.
    private NotificationCompat.Action.Builder createActionBuilder(PendingIntent snoozePendingIntent) {
        return new NotificationCompat.Action.Builder(
                R.drawable.ic_sleeping, "Action Channel", snoozePendingIntent);
    }

    private NotificationCompat.Builder buildNotification(PendingIntent pendingIntent,
                                                         NotificationCompat.Action.Builder actionBuilder,
                                                         NotificationCompat.Action.Extender actionExtender) {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)

                // Standard notification customizations.
                .setSmallIcon(R.drawable.ic_full_sad)
                .setContentTitle("Bedtime, Buddy.")
                .setContentText("To wake up at 7:30AM, we suggest you sleep now.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // Wear specific notification customization.
                .extend(new NotificationCompat.WearableExtender()
                        .setContentIcon(R.drawable.ic_sleeping)
                        .setGravity(Gravity.BOTTOM))
                .addAction(actionBuilder.extend(actionExtender).build());
    }


    @TargetApi(26)
    private void configureNotificationManager() {
        // The user-visible name of the channel.
        CharSequence name = "Sleep Reminder";

        // The user-visible description of the channel.
        String description = "Kind reminder that it's time to go to sleep.";

        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);

        // Configure the notification channel.
        mChannel.setDescription(description);

        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.setLightColor(Color.BLUE);

        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

        mNotificationManager.createNotificationChannel(mChannel);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @TargetApi(26)
    private void ChannelNotification() {
        configureNotificationManager();

        PendingIntent pendingIntent = restartIntent();
        PendingIntent snoozePendingIntent = snoozeIntent();

        NotificationCompat.Action.WearableExtender actionExtender = buildActionExtender();
        NotificationCompat.Action.Builder actionBuilder = createActionBuilder(snoozePendingIntent);

        // Create the builder for the notification.
        NotificationCompat.Builder notificationBuilder =
                buildNotification(pendingIntent, actionBuilder, actionExtender)
                        .setChannelId(NOTIFICATION_CHANNEL);

        // Issue the notification.
        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void DefaultNotification() {
        PendingIntent pendingIntent = restartIntent();
        PendingIntent snoozePendingIntent = snoozeIntent();

        // Create a wearable action extender.
        NotificationCompat.Action.WearableExtender actionExtender = buildActionExtender();
        NotificationCompat.Action.Builder actionBuilder = createActionBuilder(snoozePendingIntent);

        // Create the builder for the notification.
        NotificationCompat.Builder notificationBuilder = buildNotification(pendingIntent, actionBuilder, actionExtender);

        // Show notification.
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    /*
     * PRIVATE FUNCTIONS FOR LOCATION DETECTION
     */

    @Override
    protected void onResume() {
        super.onResume();

        /*
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // request_permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        System.out.println("onResume ing");
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null /* Looper */
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

    private void startLocationDetection() {
        // Create Fused Location Client
        fusedLocationClient = getFusedLocationProviderClient(this);

        setUpLocationRequest();

        // Build location request client.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setNeedBle(true);

        setUpSettingsClient(builder);

        getLocationCallback();
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
                    System.out.println("Location settings satisfied");
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
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
                                        SleepPrompt.this,
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
        System.out.println("callback");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    System.out.println("location result is null");
                    return;
                }
                System.out.print("HOME LOCATION DETECTED");
                for (Location location : locationResult.getLocations()) {
                    System.out.print(location.getLatitude() + ", " + location.getLongitude());
                    Toast.makeText(SleepPrompt.this, "Local: " + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_LONG).show();
                }
            }
        };
    }

}
