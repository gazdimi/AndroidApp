package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.sql.Timestamp;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{

    String userid;
    static final int req = 007;

    private SQLiteDatabase db;
    private GoogleMap mMap;
    SharedPreferences shpref;

    FirebaseDatabase fbdb = FirebaseDatabase.getInstance();
    DatabaseReference dbref;

    PolylineOptions routeopt = new PolylineOptions()
            .clickable(true);

    MarkerOptions markop = new MarkerOptions();

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


        shpref= PreferenceManager.getDefaultSharedPreferences(this);
        userid = shpref.getString("userid", "0");
        dbref = fbdb.getReference(userid); //get firebase reference for current user child
    }

    public void loadLastRoute()
    {
        Cursor cursorstart = db.rawQuery("SELECT timestamp FROM Locations " +
                "WHERE latitude == 'start' " +
                "ORDER BY timestamp DESC LIMIT 1;",null);

        if(cursorstart.moveToNext()) //there are local data
        {
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


            while (cursor.moveToNext())
            {
                Timestamp timestamp = new Timestamp(Long.parseLong(cursor.getString(3))*1000);

                LatLng loc = new LatLng(Double.parseDouble(cursor.getString(0)),
                        Double.parseDouble(cursor.getString(1)));
                markop.position(loc);
                markop.title(timestamp+"("+cursor.getString(2)+"m/s)");
                mMap.addMarker(markop);

                routeopt.add(new LatLng(Double.parseDouble(cursor.getString(0)),
                        Double.parseDouble(cursor.getString(1))));
            }
            mMap.addPolyline(routeopt);

        }
        else //we retrieve from firebase
        {

            dbref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    //we keep go through the children (routes) to find the last
                    DataSnapshot lastchild = dataSnapshot;
                    long num = dataSnapshot.getChildrenCount();
                    int j=0;
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren())
                    {
                        j++;
                        if (j==num)
                        {
                            lastchild = postSnapshot;
                        }
                    }

                    //in the last child(route), we retrieve the data from every point
                    for(int i = 0; i< lastchild.getChildrenCount(); i++)
                    {
                        String lat = lastchild.child(String.valueOf(i)).child("0").getValue().toString();
                        String lon = lastchild.child(String.valueOf(i)).child("1").getValue().toString();
                        String spe = lastchild.child(String.valueOf(i)).child("2").getValue().toString();
                        String tim = lastchild.child(String.valueOf(i)).child("3").getValue().toString();

                        Timestamp timestamp = new Timestamp(Long.parseLong(tim)*1000);

                        LatLng loc = new LatLng(Double.parseDouble(lat),
                                Double.parseDouble(lon));
                        markop.position(loc);
                        markop.title(timestamp+"("+spe+"m/s)");
                        mMap.addMarker(markop);

                        routeopt.add(new LatLng(Double.parseDouble(lat),
                                Double.parseDouble(lon)));
                        mMap.addPolyline(routeopt);

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //check if we have permissions, if not we ask them
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req);
        }
        else
        {
            mMap.setMyLocationEnabled(true);
        }
        
        loadLastRoute();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == req))
        {
            mMap.setMyLocationEnabled(true);
        }
    }
}
