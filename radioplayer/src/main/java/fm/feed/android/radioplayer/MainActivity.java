package fm.feed.android.radioplayer;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import fm.feed.android.playersdk.Player;
import fm.feed.android.playersdk.PlayerAvailabilityListener;

import fm.feed.android.radioplayer.tabbed.PlayerActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        configureRadio();
    }

    private void configureRadio() {
        Button openPlayer = (Button) findViewById(R.id.openPlayerButton);

        openPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start the player and show _all_ stations not marked as hidden
                Intent ai = new Intent(MainActivity.this, PlayerActivity.class);
                ai.putExtra(PlayerActivity.EXTRA_PARENT_ACTIVITY, new Intent(MainActivity.this, MainActivity.class));

                startActivity(ai);
            }
        });

        // make buttons visible if radio is available
        Player.getInstance().onPlayerAvailability(new PlayerAvailabilityListener() {
            @Override
            public void onAvailable() {
                findViewById(R.id.openButtons).setVisibility(View.VISIBLE);
            }

            @Override
            public void onUnavailable() {
                // button stays visible = 'GONE'
                Toast.makeText(MainActivity.this, "Sorry, music is not available to you right now", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
