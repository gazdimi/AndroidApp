package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LocationListener, SensorEventListener
{

    MediaPlayer mp;
    Network network;
    SeekBar duration;
    TextView weather, li, songtitle, textView2;
    Button voiceb, musicb, selectsong;
    Text2Speech t2s;
    LocationManager locationManager; //reference to the system Location Manager
    SQLiteDatabase db;
    SharedPreferences shpref;
    SensorManager sensorManager;
    Sensor light;
    ConstraintLayout background;
    ImageView img;
    boolean running = false, start = false;
    static final int req = 001;
    static final int voice_req = 002;
    static final int REQ = 003;
    static final int READ_REQUEST_CODE = 004;
    double longitude, latitude;
    String icon, userid;
    Resources res;

    FirebaseDatabase fbdb = FirebaseDatabase.getInstance();
    DatabaseReference dbref;

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

            }catch (Exception e){ showMessage(R.string.error_title,R.string.message_error);}

            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shpref= PreferenceManager.getDefaultSharedPreferences(this);
        userid = shpref.getString("userid", "0");
        if (userid == "0")
        {
            Long ts = System.currentTimeMillis(); //create timestamp
            String timestamp = ts.toString();

            SharedPreferences.Editor editor = shpref.edit();
            editor.putString("userid",timestamp);
            editor.commit();
            userid = shpref.getString("userid", "0");
        }

        dbref = fbdb.getReference(userid);

        selectsong = findViewById(R.id.selectsong);
        songtitle = findViewById(R.id.songTitle);
        duration = findViewById(R.id.songduration);
        musicb = findViewById(R.id.musicbut);
        voiceb = findViewById(R.id.voicecom);
        weather = findViewById(R.id.weather);
        textView2 = findViewById(R.id.textView2);
        li = findViewById(R.id.light);
        background = findViewById(R.id.background);
        img = findViewById(R.id.sth);

        selectsong.setOnClickListener(this);
        musicb.setOnClickListener(this);
        voiceb.setOnClickListener(this);
        t2s = new Text2Speech(this); //class for using TextToSpeech
        res = getResources();

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE); //info about location
        getWeatherForecast();

        //Get an instance of the senso dbref.childr service, and use that to get an instance of light sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this,light,SensorManager.SENSOR_DELAY_NORMAL);

        db = openOrCreateDatabase("Running",MODE_PRIVATE,null); //open or create db file
        db.execSQL("CREATE TABLE IF NOT EXISTS Locations(latitude TEXT, longitude TEXT, speed TEXT, timestamp TEXT);"); //create table if not exists

        updateFirebase();
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
        updateFirebase();
    }

    public void updateFirebase()
    {
        //get all starting points
        Cursor cursorstart = db.rawQuery("SELECT timestamp FROM Locations " +
                "WHERE latitude == 'start' " +
                "ORDER BY timestamp;",null);


        //get all finishing points
        Cursor cursorstop = db.rawQuery("SELECT timestamp FROM Locations " +
                "WHERE latitude == 'stop' " +
                "ORDER BY timestamp;",null);


        //if there are any routes
        while (cursorstart.moveToNext() && cursorstop.moveToNext())
        {
            String starttime = cursorstart.getString(0);
            String stoptime = cursorstop.getString(0);

            //get all points inside the selected route
            Cursor cursor = db.rawQuery("SELECT * FROM Locations " +
                    "WHERE timestamp > '"+starttime+"' AND timestamp < '"+stoptime+"'", null);

            DatabaseReference tempdbref = dbref.child(starttime+"-"+stoptime); //create node inside user for current route

            //add the point data inside an arraylist
            ArrayList<ArrayList<String>> data = new ArrayList<>();
            while (cursor.moveToNext())
            {
                ArrayList<String> s = new ArrayList<>();
                s.add(cursor.getString(0));
                s.add(cursor.getString(1));
                s.add(cursor.getString(2));
                s.add(cursor.getString(3));
                data.add(s);
            }

            tempdbref.setValue(data); //upload data inside route firebase child
            dbref.getParent(); //go back to user node

        }
        //delete data fro sqlite since they are uploaded in firebase
        db.execSQL("DELETE FROM Locations;");
    }

    public void getWeatherForecast()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, req); //case of denying accessing location
        }
        else{
            if (connected() && locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER )) { //check for internet connection and gps enabled
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this); //get location updates
                start = true; //retrieve weather conditions on activity start
            }else { showMessage(R.string.error_title, R.string.message_error_2);}
        }
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
            case R.id.voicecom: //start or stop button for running
                Intent tempintent;
                tempintent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                tempintent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                tempintent.putExtra(RecognizerIntent.EXTRA_PROMPT,R.string.recognizer);
                startActivityForResult(tempintent,voice_req); //get result
                break;
            case R.id.selectsong:
                Intent intent;
                intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/mpeg");
                startActivityForResult(Intent.createChooser(intent,"audio file"), READ_REQUEST_CODE);
                break;
            case R.id.musicbut:
                if (mp != null)
                {
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
                }
                else
                {
                    Toast.makeText(this,R.string.no_song,Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == voice_req && resultCode == RESULT_OK)
        {
            try{ //list of results from speech recognition, ordered in descending
                final ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null)
                {
                    StringBuffer b = new StringBuffer();
                    b.append(results.get(0)); //add 1st element with best score to string buffer
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setCancelable(true).setTitle(res.getString(R.string.dialog_title,b)).setMessage(res.getString(R.string.dialog_message,b));
                    builder.setPositiveButton("yes", new DialogInterface.OnClickListener() { //DialogInterface using as anonymous interface, for doing something when yes/no option has been clicked on the dialog
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (results.get(0).toUpperCase().contains("START") && running == false)
                            {
                                if (start) //if gps is already active to get weather forecast
                                {
                                    Toast.makeText(MainActivity.this, R.string.wait_for_gps, Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    db.execSQL("INSERT INTO Locations VALUES " +
                                            "('start','start','0','"+(System.currentTimeMillis()/1000) +"');");
                                    voiceb.setBackgroundResource(R.drawable.ic_action_name);
                                    startRunning();
                                }
                            }
                            else if (results.get(0).toUpperCase().contains("STOP") && running == true)
                            {
                                db.execSQL("INSERT INTO Locations VALUES " +
                                        "('stop','stop','0','"+(System.currentTimeMillis()/1000) +"');");
                                voiceb.setBackgroundResource(R.drawable.ic_action_standing);
                                stopRunning();
                            }
                        }
                    });
                    builder.setNegativeButton("no",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();

                }
            }catch (Exception e){Toast.makeText(MainActivity.this, R.string.error_toast, Toast.LENGTH_LONG).show();}

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
        double tempspeed  = location.getSpeed();
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
                JSONArray jsonArray = new JSONArray(jsonobject.getString("weather"));
                for (int i=0; i< jsonArray.length(); i++) {
                    JSONObject j = jsonArray.getJSONObject(i);
                    icon = j.getString("icon");
                }
                String url = "http://openweathermap.org/img/wn/"+icon+"@2x.png";
                RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                ImageRequest imageRequest = new ImageRequest(url, new Response.Listener<Bitmap>() { //load icon from internet
                    @Override
                    public void onResponse(Bitmap response) {
                        img.setImageBitmap(response);
                    }
                }, 0, 0, null, Bitmap.Config.RGB_565,
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(MainActivity.this, R.string.error_icon, Toast.LENGTH_LONG).show();
                            }
                        });
                requestQueue.add(imageRequest);
                color(temperature);
                weather.setText(temperature + (char) 0x00B0 + "C");
                if(Double.parseDouble(temperature) < 10.0){
                    t2s.speak("The temperature is too cold to go running today");
                }else { t2s.speak("Start your running workout");}
                } catch (Exception e) {
                    Toast.makeText(this,R.string.toast, Toast.LENGTH_LONG).show();
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
        if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == REQ))
        {
            startRunning(); //recall if accepted
        }
        else if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (requestCode == req))
        {
            getWeatherForecast();
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

    public void showMessage(int title, final int message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setTitle(title).setMessage(message);
        builder.show();
    }

    public void color(String temperature){
        double temp = Double.parseDouble(temperature);
        if(temp < 0.0){
            background.setBackgroundResource(R.drawable.gradient_under_zero);
            weather.setTextColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimaryDark,null));
            li.setTextColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimaryDark,null));
            songtitle.setTextColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimaryDark,null));
            textView2.setTextColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimaryDark,null));
        }else if (temp < 11.0){ background.setBackgroundResource(R.drawable.gradient_under_eleven);
        }else if (temp < 36.0){ background.setBackgroundResource(R.drawable.gradient_under_thirty_six);
        }else { background.setBackgroundResource(R.drawable.gradient_hot);}

    }
    public boolean connected(){ //check for internet connection
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager!=null){
            network = connectivityManager.getActiveNetwork();
        }
        return (network != null);
    }
}
