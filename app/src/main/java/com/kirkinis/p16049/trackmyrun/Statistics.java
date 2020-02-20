package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;

public class Statistics extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener, OnMapReadyCallback {

    SharedPreferences preferences;
    String userid;

    FirebaseDatabase fbdb = FirebaseDatabase.getInstance();
    DatabaseReference dbref;

    TextView speedlabel, timelabel;

    MarkerOptions markop = new MarkerOptions();
    MapView mapv;
    GoogleMap mmap;
    protected static final String mapkey = "MAPVIEW_BUNDLE_KEY";
    static final int req = 006;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        userid = preferences.getString("userid", "0");
        dbref = fbdb.getReference(userid);

        speedlabel = findViewById(R.id.avg_speed_label);
        timelabel = findViewById(R.id.avg_time_label);


        Spinner spinner = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.spinner_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        Bundle mapViewBundle = null;
        if (savedInstanceState != null)
        {
            mapViewBundle = savedInstanceState.getBundle(mapkey);
        }
        mapv = findViewById(R.id.mapView);
        mapv.onCreate(mapViewBundle);
        mapv.getMapAsync( this);


        mapv.bringToFront();
        mapv.setVisibility(View.INVISIBLE);

        getDataFromDatabase();
    }

    public void getDataFromDatabase()
    {
        //add listener for once
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                double avg_spd = 0;
                double avg_time = 0;
                int locnum = 0;
                int runnum = 0;
                DataSnapshot lastchild = dataSnapshot;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) //get every child(rute)
                {
                    for(int i = 0; i< postSnapshot.getChildrenCount(); i++) //get everypoint of current route
                    {
                        String lat = postSnapshot.child(String.valueOf(i)).child("0").getValue().toString();
                        String lon = postSnapshot.child(String.valueOf(i)).child("1").getValue().toString();
                        String spe = postSnapshot.child(String.valueOf(i)).child("2").getValue().toString();
                        String tim = postSnapshot.child(String.valueOf(i)).child("3").getValue().toString();

                        Timestamp timestamp = new Timestamp(Long.parseLong(tim)*1000);

                        LatLng loc = new LatLng(Double.parseDouble(lat),
                                Double.parseDouble(lon));
                        markop.position(loc);
                        markop.title(timestamp+"("+spe+"m/s)");
                        mmap.addMarker(markop); //add marker in map

                        locnum++;
                        avg_spd += Double.parseDouble(spe);
                    }
                    runnum++;
                    String[] times = postSnapshot.getKey().split("-");
                    avg_time += Integer.parseInt(times[1]) - Integer.parseInt(times[0]); //finish timestamp minus start timestamp
                }
                if (runnum !=0)
                {
                    avg_spd = Math.round(avg_spd*100/locnum)/100.0;
                    avg_time = Math.round(avg_time/runnum/60);

                    //update statistic labels
                    speedlabel.setText(speedlabel.getText()+" "+avg_spd);
                    timelabel.setText(timelabel.getText()+" "+avg_time);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (pos){
            case 0:
                mapv.setVisibility(View.INVISIBLE);
                speedlabel.setVisibility(View.VISIBLE);
                timelabel.setVisibility(View.VISIBLE);
                break;
            case 1:
                mapv.setVisibility(View.VISIBLE);
                speedlabel.setVisibility(View.INVISIBLE);
                timelabel.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mmap = googleMap;
        //check if we have permissions, if not we ask them
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req);
        }
        else
        {
            mmap.setMyLocationEnabled(true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == req))
        {
            mmap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        mapv.onResume();
        super.onResume();
    }

    @Override
    protected void onStart() {
        mapv.onStart();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mapv.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mapv.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapv.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapv.onLowMemory();
        super.onLowMemory();
    }
}
