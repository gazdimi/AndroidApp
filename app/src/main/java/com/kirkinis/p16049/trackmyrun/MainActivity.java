package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements LocationListener {

    LocationManager locationManager; //reference to the system Location Manager
    static final int REQ_CODE = 432;
    TextView weather;
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

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); //info about location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_CODE); //case of denying accessing location
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this); //get location updates
        }

        weather = findViewById(R.id.weather);
        Weather condition = new Weather();
        try {
            StringBuffer content = condition.execute("https://openweathermap.org/data/2.5/weather?lat="+latitude+"&lon="+longitude+"&appid=b6907d289e10d714a6e88b30761fae22").get();
            JSONObject jsonobject = new JSONObject(content.toString());
            weather.setText(jsonobject.getJSONObject("main").getString("temp") + (char) 0x00B0 + "C");

        }catch (Exception e){}

        this.onLocationChanged(null); //for re-opening app, get current speed (initialize with null)
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
    public void onLocationChanged(Location location) {
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
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
}
