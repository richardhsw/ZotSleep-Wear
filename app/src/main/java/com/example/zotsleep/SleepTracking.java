package com.example.zotsleep;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.support.v4.view.ViewCompat;
import android.support.wearable.activity.WearableActivity;
import android.transition.ChangeImageTransform;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class SleepTracking extends WearableActivity implements GestureDetector.OnGestureListener{

//    private boolean createdService = false;

    private Intent bgIntent;
    private GestureDetector gestureDetector;
    private ImageView wakeButton;

    // Binding background service
    HeartSensor mService;
    boolean mBound = false;
    Handler handler;
    Runnable runnable;
    TextView heartTV;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            HeartSensor.LocalBinder binder = (HeartSensor.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            System.out.println("Service bounded");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Deprecated
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        // inside your activity (if you did not enable transitions in your theme)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        // set an exit transition
        // getWindow().setExitTransition(new Fade());
        getWindow().setSharedElementExitTransition(new ChangeImageTransform());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_tracking);

        // Set up background heart sensor intent.
        startSensor();

        // Set Gesture Detectors on wake button.
        gestureDetector = new GestureDetector(this);
        wakeButton = findViewById(R.id.sleepingIB);
        wakeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });


        // Get Heart Rate from background service
        updateHRDisplay();

//        loadFragment(new SleepFragment());
//        createdService = true;

        checkAdvice();

        // Enables Always-on
        setAmbientEnabled();
    }

    /*
     * PRIVATE HELPER FUNCTIONS
     */
    private void startSensor() {
        bgIntent = new Intent(SleepTracking.this, HeartSensor.class);
        System.out.println("Starting service...");
        bindService(bgIntent, connection, Context.BIND_AUTO_CREATE);
        startService(bgIntent);
    }

    private void updateHRDisplay() {
        heartTV = findViewById(R.id.heartTV);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mBound) {
                    heartTV.setText(mService.getHeartRate());
                }
                handler.postDelayed(this, 200);
            }
        };
        handler.postDelayed(runnable, 200);
    }

    // Check if advice should be shown every 10 minutes.
    private void checkAdvice() {
        Handler advice_handler = new Handler();
        Runnable advice_runnable = new Runnable() {
            @Override
            public void run() {
                if (mService.getAdvice()) {
                    startActivityForResult(new Intent(SleepTracking.this, AwakeFragment.class), 0);
                }
            }
        };

        advice_handler.postDelayed(advice_runnable, 1000 * 10);
    }

//    private void loadFragment(Fragment fragment) {
//        // create a FragmentManager
//        FragmentManager fm = getFragmentManager();
//        // create a FragmentTransaction to begin the transaction and replace the Fragment
//        FragmentTransaction fragmentTransaction = fm.beginTransaction();
//
//        // Pass createdService variable to fragment so that it
//        // will not create a second background service when it
//        // enters Ambient mode a second time.
//        Bundle bgService = new Bundle();
//        bgService.putBoolean("First", createdService);
//        fragment.setArguments(bgService);
//
//        System.out.println("Switching fragments");
//        // replace the FrameLayout with new Fragment
//        fragmentTransaction.replace(R.id.ambientFL, fragment);
//        fragmentTransaction.commit(); // save the changes
//    }

    /*
     * AMBIENT MODE FUNCTIONS
     */
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        System.out.println("Exiting sleep tracking ambient mode");
        // startActivityForResult(new Intent(this, AwakeFragment.class), 0);
    }

    /*
     * GESTURE DETECTOR INTERFACE IMPLEMENTATIONS
     */
    @Override
    public boolean onDown(MotionEvent e) {
        /*
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE) + 1;

        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Alarm set by Zotsleep");
        alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        Toast.makeText(this, "Setting new alarm", Toast.LENGTH_SHORT).show();
        startActivity(alarmIntent);
        */

        System.out.println("Stopping service by touch...");
        stopService(bgIntent);
        unbindService(connection);
        mBound = false;

        System.out.println("Going to Awake Activity...");
        Intent intent = new Intent(SleepTracking.this, AwakeActivity.class);

        ActivityOptions options = ActivityOptions
                .makeSceneTransitionAnimation(SleepTracking.this, wakeButton,
                        ViewCompat.getTransitionName(wakeButton));

        startActivity(intent, options.toBundle());

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        /*
        Point dimensions = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(dimensions);

        if (e1.getY() - e2.getY() >= dimensions.y * 0.5)
        {
            System.out.println("Stopping service by touch...");
            stopService(bgIntent);
            unbindService(connection);
            mBound = false;

            System.out.println("Going to Awake Activity...");
            Intent intent = new Intent(SleepTracking.this, AwakeActivity.class);

            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(SleepTracking.this, wakeButton,
                            ViewCompat.getTransitionName(wakeButton));

            startActivity(intent, options.toBundle());
        }
        */

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
