package com.kirkinis.p16049.trackmyrun;

import androidx.fragment.app.FragmentActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{

    private SQLiteDatabase db;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        db = openOrCreateDatabase("Running",MODE_PRIVATE,null);
        db.execSQL("CREATE TABLE IF NOT EXISTS Locations(latitude TEXT, longitude TEXT, speed TEXT, timestamp TEXT);"); //dimiourgoume ton pinaka an den iparxei


    }

    public  void loadLastRoute()
    {
        Cursor cursorstart = db.rawQuery("SELECT timestamp FROM Locations " +
                "WHERE latitude == 'start' " +
                "ORDER BY timestamp DESC LIMIT 1;",null);

        //cursor.moveToLast();
        cursorstart.moveToFirst();
        String starttime = cursorstart.getString(0);


        Cursor cursorstop = db.rawQuery("SELECT timestamp FROM Locations " +
                "WHERE latitude == 'stop' " +
                "ORDER BY timestamp DESC LIMIT 1;",null);

        //cursor.moveToLast();
        cursorstop.moveToFirst();
        String stoptime = cursorstop.getString(0);


        Cursor cursor = db.rawQuery("SELECT * FROM Locations " +
                "WHERE timestamp > '"+starttime+"' AND timestamp < '"+stoptime+"'", null);


        PolylineOptions routeopt = new PolylineOptions()
                .clickable(true);

        while (cursor.moveToNext())
        {
            MarkerOptions markop = new MarkerOptions();

            Timestamp timestamp = new Timestamp(Long.parseLong(cursor.getString(3))*1000);

            LatLng loc = new LatLng(Double.parseDouble(cursor.getString(0)),
                    Double.parseDouble(cursor.getString(1)));
            markop.position(loc);
            markop.title(timestamp+"("+cursor.getString(2)+"km/h)");
            mMap.addMarker(markop);

            routeopt.add(new LatLng(Double.parseDouble(cursor.getString(0)),
                    Double.parseDouble(cursor.getString(1))));
        }

        Polyline route = mMap.addPolyline(routeopt);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        
        loadLastRoute();
    }
}
