package com.kirkinis.p16049.trackmyrun;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}
