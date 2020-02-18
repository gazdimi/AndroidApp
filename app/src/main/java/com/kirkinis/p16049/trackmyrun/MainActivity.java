package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener
{

    private Button voiceb;

    private Text2Speech t2s;
    private LocationManager locman;
    private SQLiteDatabase db;

    private boolean running = false;
    static final int req = 001;
    static final int voice_req = 002;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceb = findViewById(R.id.voicecom);
        voiceb.setOnClickListener(this);

        t2s = new Text2Speech(this);

        locman = (LocationManager)getSystemService(LOCATION_SERVICE);

        db = openOrCreateDatabase("Running",MODE_PRIVATE,null); //anoigoume i dimiourgoume ti basi
        db.execSQL("CREATE TABLE IF NOT EXISTS Locations(latitude TEXT, longitude TEXT, speed TEXT, timestamp TEXT);"); //dimiourgoume ton pinaka an den iparxei


    }

    public void startRunning()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        { // an den iparxoun ta zitame
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req);
        }
        else
        { // dimiourgoume ton listener
            locman.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0,this);
            running = true;
        }
    }

    public void stopRunning()
    {
        locman.removeUpdates(this); //katastrefoyme ton listener
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
        switch (item.getItemId())
        {
            case R.id.mapbutton:
                Intent mapintent = new Intent(this,MapsActivity.class);
                startActivity(mapintent);
                break;
            case R.id.statisticsbutton:
                Intent statintent = new Intent(this,Statistics.class);
                startActivity(statintent);
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
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            startRunning(); //to kaloume ksana oste na energopoiithei an to dektei o xristis
        }
    }
}
