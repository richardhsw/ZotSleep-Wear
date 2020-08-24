package com.example.zotsleep;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;


import java.util.Calendar;
import java.util.Map;


public class HeartSensor extends Service implements SensorEventListener {

    // /sdcard/Android/data/com.example.zotsleep/files/MyData.txt

    // Access a Cloud Firestore instance from your Activity
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    //private Sensor mPPGSensor;
    private Sensor lightSensor;
    private Sensor accelerometerSensor;
    private int acc_count = 0;
    private String filename = "/MyData.txt";
    private String urlstring = "https://us-central1-cs125-233405.cloudfunctions.net/watchBridge/addData";
    private FileOutputStream outputstream = null;
    private StringBuffer outputString = new StringBuffer("");
    private String heartRateValue;
    private final static int INTERVAL = 1000 * 60 * 3; //3 min
    private boolean firstAlarm = false;
    private boolean advice = false;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    Handler mHandler = new Handler();

    Runnable mHandlerTask = new Runnable()
    {
        @Override
        public void run() {
            new Background().execute();
            mHandler.postDelayed(mHandlerTask, INTERVAL);
        }
    };

    void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    void stopRepeatingTask()
    {
        mHandler.removeCallbacks(mHandlerTask);
    }

    public HeartSensor() {
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        HeartSensor getService() {
            // Return this instance of LocalService so clients can call public methods
            return HeartSensor.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {

        listenToDocument();
        Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(21);
        accelerometerSensor = mSensorManager.getDefaultSensor(1);
        lightSensor= mSensorManager.getDefaultSensor(5);
        //mPPGSensor = mSensorManager.getDefaultSensor(65545);

        //create fileoutputstream
        try {
            // File file = new File(getExternalFilesDir(null), filename);
            outputstream = new FileOutputStream(getExternalFilesDir(null).getPath() + filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //resetOutputStream();

        //Print all the sensors
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor1 : sensors) {
            Log.i("Sensor list", sensor1.getName() + ": " + sensor1.getType());
        }

        Log.d("external dir", getExternalFilesDir(null).getPath());
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        super.onDestroy();
        String end_time = Long.toString(Calendar.getInstance().getTimeInMillis());
        String end_csv = "End Time, " +"0, " + end_time;
        writeToFile(end_csv);
        stopMeasure();
        stopRepeatingTask();
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();

        super.onStartCommand(intent, flags, startId);
        String start_time = Long.toString(Calendar.getInstance().getTimeInMillis());
        String start_csv = "Start Time, " +"0, " + start_time;
        writeToFile(start_csv);
        startRepeatingTask();
        startMeasure();
        return START_STICKY;
    }

    private void startMeasure() {
        boolean HRSensorRegistered = mSensorManager.registerListener(
                this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        boolean accelerometerRegistered = mSensorManager.registerListener(
                this, accelerometerSensor, 1000000);
        boolean lightSensorRegistered = mSensorManager.registerListener(
                this, lightSensor, 1000);
        //boolean ppgSensorRegistered = mSensorManager.registerListener(
                //this, mPPGSensor, SensorManager.SENSOR_DELAY_FASTEST);

        Log.d("HR sensor Status:", " Sensor registered: " + (HRSensorRegistered ? "yes" : "no"));
        Log.d("Light sensor Status:", " Sensor registered: " + (lightSensorRegistered ? "yes" : "no"));
        Log.d("accelerometer sensor Status:", " Sensor registered: " + (accelerometerRegistered ? "yes" : "no"));
       //Log.d("PPG sensor Status:", " Sensor registered: " + (ppgSensorRegistered ? "yes" : "no"));
    }

    private void stopMeasure() {
        mSensorManager.unregisterListener(this);
        try {
            outputstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public String getHeartRate() {
        return heartRateValue;
    }

    public boolean getAdvice(){

        return advice;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        float mSensorValue = event.values[0];
        String sensor_time = Long.toString(Calendar.getInstance().getTimeInMillis());


        if (event.sensor.getName().equals("Heart Rate Sensor")) {
            int mHeartRate = Math.round(mSensorValue);
            heartRateValue = Integer.toString(mHeartRate);
        }

        if(event.sensor.getType() == 1){
            if(acc_count == 5){

                String sensor_value = event.sensor.getName() + ", " + Float.toString(mSensorValue) + ", " +
                        sensor_time + ", ";

                writeToFile(sensor_value);
                outputString.append((sensor_value + "\n"));
                acc_count = 0;
                Log.d("File Output:",  sensor_value); //console log to debug sensor values
            }

            acc_count ++;
        }
        else{
            String sensor_value = event.sensor.getName() + ", " + Float.toString(mSensorValue) + ", " +
                    sensor_time + ", ";

            writeToFile(sensor_value);
            outputString.append((sensor_value + "\n"));
            Log.d("File Output:",  sensor_value); //console log to debug sensor values
        }

    }

    public void listenToDocument() {
        // [START listen_document]
        Toast.makeText(this, "listener created!", Toast.LENGTH_LONG).show();
        final DocumentReference docRef = db.collection("nights").document("gaXp5FbSQ7D7KSmFoxP2");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("listenToDocument", "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {

                    Map<String, Object> nightData = snapshot.getData();

                    //if the alarm value from server is true and the first alarm is false, then set alarm
                    if(nightData.containsKey("alarm") && (Boolean)nightData.get("alarm") && firstAlarm == false){
                        setAlarm();
                        firstAlarm = true;
                    }
                    //if the health state is below the threshold 5*averageSleepcycle, set advice to true
                    if((Double)nightData.get("healthState") < 5*(Double)nightData.get("sleepCycleLength")){
                        advice = true;
                    }
                    //Log.d("listenToDocument", "Current data: " + nightData);
                } else {
                    //Log.d("listenToDocument", "Current data: null");
                }
            }
        });
        // [END listen_document]
    }

    private void setAlarm(){
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE) + 5;

        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Alarm set by Zotsleep");
        alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        Toast.makeText(this, "Setting new alarm", Toast.LENGTH_SHORT).show();
        startActivity(alarmIntent);
    }
    private String request(String urlString) {

        //StringBuffer chain = new StringBuffer("");
        String result = "";
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", "");
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            //String str = readFile(getExternalFilesDir(null).getPath() + filename);
            String str = outputString.toString();
            //resetOutputStream();
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write( str );
            osw.flush();
            osw.close();
            os.close();
            conn.connect();

            //read input stream and log it
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result2 = bis.read();
            while(result2 != -1){
                buf.write((byte)result2);
                result2 = bis.read();
            }
            result = buf.toString();
            //Log.d("Http read from server:", result);

        }
        catch (IOException e) {
            // Writing exception to log
            e.printStackTrace();
        }
        //return chain;
        return result;
    }
    private void writeToFile(String data) {
        try {
            outputstream.write((data + "\n").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private class Background extends AsyncTask<Void, Void, String> {
        private ProgressDialog pd;

        // onPreExecute called before the doInBackgroud start for display
        // progress dialog.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... param) {
            //Log.d("DoInBackground Called", "====================================================================");
            return request(urlstring);
        }

        // onPostExecute displays the results of the doInBackgroud and also we
        // can hide progress dialog.
        @Override
        protected void onPostExecute(String result) {
            outputString.delete(0, outputString.length());
            //not yet implemented
            //set alarm clock here
        }
    }

    //unused function for reading from file
    private String readFile(String file_name) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file_name));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            return everything;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    //unused function for resetting output stream
    private void resetOutputStream(){
        try {
            if(outputstream != null){
                outputstream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        filename = "/MyData" + ts + ".txt";
        try {
            // File file = new File(getExternalFilesDir(null), filename);
            outputstream = new FileOutputStream(getExternalFilesDir(null).getPath() + filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
