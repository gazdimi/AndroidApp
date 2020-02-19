package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.telephony.AvailableNetworkInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener, SensorEventListener
{

    MediaPlayer mp;

    SeekBar duration;
    Button voiceb, musicb, selectsong;
    Text2Speech t2s;
    LocationManager locationManager; //reference to the system Location Manager
    SQLiteDatabase db;
    SensorManager sensorManager;
    Sensor light;
    boolean running = false; boolean start = false;
    static final int req = 001;
    static final int voice_req = 002;
    static  final int REQ = 003;
    static  final int READ_REQUEST_CODE = 004;
    TextView weather, li, songtitle;
    ConstraintLayout background;
    double longitude, latitude;

    class Weather extends AsyncTask<String, Void, StringBuffer>{ //<Params (url in string), Progress, Result>
        @Override
        protected StringBuffer doInBackground(String... params) { //Strings... means multiple urls

            try{
                //url connection
                URL url = new URL(params[0]); //check for valid given url
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.connect();

                //get weather data from url
                InputStream input = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(input);

                //retrieve data and return as string
                int data = reader.read();
                StringBuffer buffer = new StringBuffer();
                char ch;
                while (data != -1 ){
                    ch = (char) data;
                    buffer.append(ch);
                    data = reader.read();
                }

                return buffer;

            }catch (Exception e){ showMessage("Error loading weather forecast","Sorry for the inconvenience, please try again later.");}

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectsong = findViewById(R.id.selectsong);
        songtitle = findViewById(R.id.songTitle);
        duration = findViewById(R.id.songduration);
        musicb = findViewById(R.id.musicbut);
        voiceb = findViewById(R.id.voicecom);
        weather = findViewById(R.id.weather);
        li = findViewById(R.id.light);
        background = findViewById(R.id.background);

        selectsong.setOnClickListener(this);
        musicb.setOnClickListener(this);
        voiceb.setOnClickListener(this);
        t2s = new Text2Speech(this);

        db = openOrCreateDatabase("Running",MODE_PRIVATE,null); //open or create db file
        db.execSQL("CREATE TABLE IF NOT EXISTS Locations(latitude TEXT, longitude TEXT, speed TEXT, timestamp TEXT);"); //create table if not exists

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE); //info about location
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req); //case of denying accessing location
        }
        else{
            if (connected() && locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER )) { //check for internet connection and gps enabled
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this); //get location updates
                start = true; //retrieve weather conditions on activity start
            }else { showMessage("Error loading weather forecast", "Make sure internet connection and GPS tracking system are available and restart the application.");}
        }

        //Get an instance of the sensor service, and use that to get an instance of light sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL);




    }

    public void startRunning()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        { //request location permission if not given
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
        }
        else
        { //register location listener for updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0,this);
            running = true;
        }
    }

    public void stopRunning()
    {
        locationManager.removeUpdates(this);
        running = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) //for choosing option
    {
        Intent intent;
        switch (item.getItemId())
        {
            case R.id.mapbutton:
                intent = new Intent(this,MapsActivity.class);
                startActivity(intent);
                break;
            case R.id.statisticsbutton:
                intent = new Intent(this,Statistics.class);
                startActivity(intent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.voicecom:
                Intent tempintent;
                tempintent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                tempintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                tempintent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Start / Stop");
                startActivityForResult(tempintent,voice_req);
                break;
            case R.id.selectsong:
                Intent intent;
                intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/mpeg");
                startActivityForResult(Intent.createChooser(intent,"audio file"), READ_REQUEST_CODE);
                break;
            case R.id.musicbut:
                if (!mp.isPlaying())
                {
                    mp.start();
                    v.setBackgroundResource(R.drawable.music_pause_button);
                }
                else
                {
                    mp.pause();
                    v.setBackgroundResource(R.drawable.music_play_button);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == voice_req && resultCode == RESULT_OK)
        {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null)
            {
                if (results.get(0).toUpperCase().contains("START") && running == false)
                {
                    db.execSQL("INSERT INTO Locations VALUES " +
                            "('start','start','0','"+(System.currentTimeMillis()/1000) +"');");

                    startRunning();
                }
                else if (results.get(0).toUpperCase().contains("STOP") && running == true)
                {
                    db.execSQL("INSERT INTO Locations VALUES " +
                            "('stop','stop','0','"+(System.currentTimeMillis()/1000) +"');");
                    stopRunning();
                }
            }
        }
        else if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK)
        {
            if (mp != null)
            {
                mp.stop();
            }
            Uri uri = data.getData();
            mp = MediaPlayer.create(this, uri);
            mp.setLooping(true);
            mp.seekTo(0);
            mp.setVolume(0.5f,0.5f);

            mp.start();
            musicb.setBackgroundResource(R.drawable.music_pause_button);

            String[] s = uri.toString().split("/");
            songtitle.setText(s[s.length-1]);

            duration.setMax(mp.getDuration());
            duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mp.seekTo(progress);
                    duration.setProgress(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    while(mp != null)
                    {
                        try
                        {
                            Message msg = new Message();
                            msg.what = mp.getCurrentPosition();
                            handler.sendMessage(msg);
                            Thread.sleep(1000);
                        }
                        catch (Exception e){}
                    }
                }
            });
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int currentposition = msg.what;
            duration.setProgress(currentposition);
            super.handleMessage(msg);
        }
    };

    @Override
    public void onLocationChanged(Location location)
    {
        double tempspeed  = location.getSpeed()*3.6; //convert to km/h
        float speed = (float)Math.round(tempspeed * 100) /100;

        Long ts = System.currentTimeMillis()/1000; //create timestamp
        String timestamp = ts.toString();

        db.execSQL("INSERT INTO Locations VALUES " +
                "('"+location.getLatitude() +"','"
                +location.getLongitude() + "','"
                +speed + "','"
                +timestamp +"');");

        if (location != null && start) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Weather condition = new Weather();
            try {
                StringBuffer content = condition.execute("https://openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=b6907d289e10d714a6e88b30761fae22").get();
                JSONObject jsonobject = new JSONObject(content.toString());
                String temperature = jsonobject.getJSONObject("main").getString("temp");
                color(temperature);
                weather.setText(temperature + (char) 0x00B0 + "C");
                } catch (Exception e) {
                    Toast.makeText(this,"Please try again later", Toast.LENGTH_LONG).show();
                }
            locationManager.removeUpdates(this);
            start = false;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == REQ)) {
            startRunning(); //recall if accepted
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float maxvalue = light.getMaximumRange();
        float percent = (event.values[0] * 100) / maxvalue;
        li.setText(String.valueOf(Math.round(percent)) + "%");
        sensorManager.unregisterListener(this); // stop listener
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void showMessage(String title, final String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setTitle(title).setMessage(message);
        builder.show();
    }

    public void color(String temperature){
        double temp = Double.parseDouble(temperature);
        if(temp < 0.0){ background.setBackgroundColor(Color.parseColor("#f9fdfe"));
        }else if (temp < 11.0){ background.setBackgroundColor(Color.parseColor("#1a5ac0"));
        }else if (temp < 36.0){ background.setBackgroundColor(Color.parseColor("#edcc08"));
        }else { background.setBackgroundColor(Color.parseColor("#ed2608"));}
    }

    public boolean connected(){ //check for internet connection
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        return (network != null);
    }
}
