package com.example.zotsleep;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FadeText extends WearableActivity {

    private TextView message_text;
    private String message;

    private Animation fadeIn, fadeOut;
    private int fadeDuration;

    private Class start_activity;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fade_text);

        // frameLayout is used to detect if user taps on the screen.
        // If tapped, skip the fade text activity and jumps to the
        // next activity.
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fadeTextFL);
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FadeText.this, start_activity));
                handler.removeCallbacksAndMessages(null);
            }
        });

        Intent intent = getIntent();

        // Get the time it takes to fade in and fade out.
        fadeDuration = intent.getIntExtra("fade_duration", 1200);

        // Get the message to display.
        message = intent.getStringExtra("message");

        // Get the activity to jump to after displaying the fade text.
        try {
            start_activity = Class.forName(intent.getStringExtra("class"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        configureFadeAnimation();
        startFade();

        // Enables Always-on
        setAmbientEnabled();
    }

    // Sets up the fade in and fade out animation.
    private void configureFadeAnimation() {
        fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(fadeDuration);
        fadeIn.setFillAfter(true);

        fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(fadeDuration);
        fadeOut.setFillAfter(true);
    }

    private void startFade() {
        // Get the text view to display the message in the activity.
        message_text = findViewById(R.id.messageTV);

        // Set the text view to display the message that was passed in
        // from the intent.
        message_text.setText(message);

        // Start the fade in animation.
        message_text.startAnimation(fadeIn);

        // Delay the fade out animation for specified seconds, so
        // the user has time to read the entire message.
        // FUTURE NOTE: make this time customizable too, like the
        // fade in and fade out duration.
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // After delay, start fade out animation.
                message_text.startAnimation(fadeOut);

                // Jump to the next activity. Delayed until the fade
                // out animation is completely finished.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(FadeText.this, start_activity));
                    }
                }, fadeDuration);
            }
        }, 3000);   //3 seconds
    }
}
