package com.example.zotsleep;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.wearable.activity.WearableActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class SleepFragment extends Fragment implements GestureDetector.OnGestureListener {

    private View view;
    private Context context;
    private Activity activity;

    private boolean createdService;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_sleep_fragment, container, false);
        context = view.getContext();
        activity = getActivity();

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            createdService = bundle.getBoolean("First");
        }

        // If this isn't the first time creating the bg service,
        // start the heart sensor.
        if (!createdService) {
            // Set up background heart sensor intent.
            startSensor();

            // Set Gesture Detectors on wake button.
            gestureDetector = new GestureDetector(this);
            wakeButton = view.findViewById(R.id.sleepingIB);
            wakeButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            });

            // Get Heart Rate from background service
            updateHRDisplay();
        }

        return view;
    }


    /*
     * PRIVATE HELPER FUNCTIONS
     */
    private void startSensor() {
        bgIntent = new Intent(context, HeartSensor.class);
        System.out.println("Starting service...");
        activity.bindService(bgIntent, connection, Context.BIND_AUTO_CREATE);
        activity.startService(bgIntent);
    }

    private void updateHRDisplay() {
        heartTV = view.findViewById(R.id.heartTV);
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


    /*
     * GESTURE DETECTOR INTERFACE IMPLEMENTATIONS
     */
    @Override
    public boolean onDown(MotionEvent e) {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE) + 1;

        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Alarm set by Zotsleep");
        alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        Toast.makeText(context, "Setting new alarm", Toast.LENGTH_SHORT).show();
        startActivity(alarmIntent);

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
        Point dimensions = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(dimensions);

        if (e1.getY() - e2.getY() >= dimensions.y * 0.5)
        {
            System.out.println("Stopping service by touch...");
            activity.stopService(bgIntent);
            activity.unbindService(connection);
            mBound = false;

            System.out.println("Going to Awake Activity...");
            Intent intent = new Intent(context, AwakeActivity.class);

            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(activity, wakeButton,
                            ViewCompat.getTransitionName(wakeButton));

            startActivity(intent, options.toBundle());
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
}
