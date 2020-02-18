package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener, SensorEventListener
{

    Button voiceb;
    Text2Speech t2s;
    LocationManager locationManager; //reference to the system Location Manager
    SQLiteDatabase db;
    SensorManager sensorManager;
    Sensor light;
    boolean running = false;
    static final int req = 001;
    static final int voice_req = 002;
    static  final int REQ = 003;
    TextView weather, li;
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

            }catch (Exception e){}

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceb = findViewById(R.id.voicecom);
        weather = findViewById(R.id.weather);
        li = findViewById(R.id.light);
        voiceb.setOnClickListener(this);

        t2s = new Text2Speech(this);

        db = openOrCreateDatabase("Running",MODE_PRIVATE,null); //anoigoume i dimiourgoume ti basi
        db.execSQL("CREATE TABLE IF NOT EXISTS Locations(latitude TEXT, longitude TEXT, speed TEXT, timestamp TEXT);"); //dimiourgoume ton pinaka an den iparxei

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE); //info about location
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req); //case of denying accessing location
        }
        else{ locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,0,this);}

        Weather condition = new Weather();
        try {
            StringBuffer content = condition.execute("https://openweathermap.org/data/2.5/weather?lat="+latitude+"&lon="+longitude+"&appid=b6907d289e10d714a6e88b30761fae22").get();
            JSONObject jsonobject = new JSONObject(content.toString());
            weather.setText(jsonobject.getJSONObject("main").getString("temp") + (char) 0x00B0 + "C");
            locationManager.removeUpdates(this);
            running = false;
        }catch (Exception e){}

        //Get an instance of the sensor service, and use that to get an instance of humidity sensor.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void startRunning()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        { // an den iparxoun ta zitame
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
        }
        else
        { // dimiourgoume ton listener
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0,this);
            running = true;
        }
    }

    public void stopRunning()
    {
        locationManager.removeUpdates(this); //katastrefoyme ton listener
        running = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
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
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
    }

    @Override
    public void onLocationChanged(Location location)
    {
        double tempspeed  = location.getSpeed()*3.6; //convert to km/h
        float speed = (float)Math.round(tempspeed * 100) /100;

        Long ts = System.currentTimeMillis()/1000; //dimiourgia timestamp
        String timestamp = ts.toString();

        db.execSQL("INSERT INTO Locations VALUES " +
                "('"+location.getLatitude() +"','"
                +location.getLongitude() + "','"
                +speed + "','"
                +timestamp +"');");

        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
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
        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == REQ))
        {
            startRunning(); //recall if accepted
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        li.setText(String.valueOf(event.values[0]) + "%");
        sensorManager.unregisterListener(this); // stop listener
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
