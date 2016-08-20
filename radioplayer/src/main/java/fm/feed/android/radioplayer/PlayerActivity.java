package fm.feed.android.radioplayer;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import fm.feed.android.playersdk.Player;
import fm.feed.android.playersdk.PlayerAvailabilityListener;
import fm.feed.android.playersdk.PlayerState;
import fm.feed.android.playersdk.model.Station;
import fm.feed.android.playersdk.view.NotificationStyle;



public class PlayerActivity extends AppCompatActivity implements PlayerFragment.PlayerFragmentEventListener {

    private final static String TAG = PlayerActivity.class.getSimpleName();

    // String array of stations that should be displayed (see createStationTabs())
    public final static String EXTRA_VISIBLE_STATION_LIST = "fm.feed.android.radioplayer.visibleStationList";
    // String array of stations that will be unhidden (see createStationTabs())
    public final static String EXTRA_UNHIDE_STATION_LIST = "fm.feed.android.radioplayer.unhideStationList";
    // Name of station to display by default, if desired
    public final static String EXTRA_DEFAULT_STATION = "fm.feed.android.radioplayer.defaultStation";

    private Player mPlayer = null;

    private ViewPager mViewPager;
    private TabLayout mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        mPlayer = Player.getInstance();

        // make sure we're using our sexy icons
        NotificationStyle ni = new NotificationStyle()
            .setSmallIcon(R.drawable.ic_headset_white_24dp)
            .setPlayIcon(R.drawable.feedfm_ic_play_white_24dp)
            .setPauseIcon(R.drawable.feedfm_ic_pause_white_24dp)
            .setSkipIcon(R.drawable.feedfm_ic_skip_white_24dp)
            .setColor(Color.BLACK)
            .setThumbsUpIcons(R.drawable.feedfm_ic_thumbsup_hollow_white_24dp, R.drawable.feedfm_ic_thumbsup_white_24dp)
            .setThumbsDownIcons(R.drawable.feedfm_ic_thumbsdown_hollow_white_24dp, R.drawable.feedfm_ic_thumbsdown_white_24dp)

            // .. and our custom notification layouts
            .setBigContentView(getPackageName(), R.layout.notification_big)
            .setContentView(getPackageName(), R.layout.notification_small)
            .setMediaImageId(R.id.notification_icon)
            .setProgressId(R.id.progress)
            .setDislikeButtonId(R.id.dislike_button)
            .setLikeButtonId(R.id.like_button)
            .setPlayPauseButtonId(R.id.play_pause_button)
            .setSkipButtonId(R.id.skip_button)
            .setTrackTextId(R.id.notification_track_title)
            .setArtistTextId(R.id.notification_track_artist)
            .setReleaseTextId(R.id.notification_track_release);

        mPlayer.setNotificationStyle(ni);

        // pick out stations to display
        Station[] visibleStations = collectStations();
        int selectedTabIndex = selectDefaultStation(visibleStations);

        mViewPager = (ViewPager) findViewById(R.id.playerContainer);
        mViewPager.setAdapter(new PlayerFragmentStatePagerAdapter(getSupportFragmentManager(), visibleStations, this));

        mTabs = (TabLayout) findViewById(R.id.radioTabs);
        if (visibleStations.length > 1) {
            for (Station station : visibleStations) {
                mTabs.addTab(mTabs.newTab().setText(station.getName()));
            }

            // listen for tab clicks
            mTabs.setOnTabSelectedListener(onTabSelectedListener);

            // listen for swipes
            mViewPager.addOnPageChangeListener(mOnPageChangeListener);

        } else {
            // no point in displaying tabs if we have only one station
            mTabs.setVisibility(View.GONE);
        }

        // display default station
        mViewPager.setCurrentItem(selectedTabIndex);

        if (mPlayer.getState() == PlayerState.READY) {
            // start pre-loading the first song in this station if we
            // just started the player up
            mPlayer.setStation(visibleStations[selectedTabIndex].getName());
            mPlayer.tune();
        }

    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        public void onPageSelected(int position) {
            mTabs.getTabAt(position).select();
        }
    };

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // don't care
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // don't care
        }
    };

    /**
     * Iterate through all the available stations and pick out the ones
     * to display. A station with a 'hidden' option from the server will not be shown
     * in the tabs unless it was specifically requested in the Intent
     * via the EXTRA_VISIBLE_STATION_LIST or EXTRA_UNHIDE_STATION_LIST
     *
     * @return the index of the selected tab
     */

    private Station[] collectStations() {
        Station activeStation = mPlayer.getStation();
        LinkedList<Station> visibleStations = new LinkedList<Station>();

        Intent ai = getIntent();
        if (ai.hasExtra(EXTRA_VISIBLE_STATION_LIST)) {
            // caller provided us with the explicit list of stations to display
            ArrayList<String> visibleStationNames = ai.getStringArrayListExtra(EXTRA_VISIBLE_STATION_LIST);

            for (int i = 0; i < visibleStationNames.size(); i++) {
                Station station = mPlayer.getStationWithName(visibleStationNames.get(i));

                if (station != null) {
                    visibleStations.add(station);
                }
            }

        } else {
            // collect all stations except for those that have 'hidden=true' option
            // and are not in the EXTRA_UNHIDE_STATION_LIST.
            HashSet<String> unhide = new HashSet<String>();
            if (ai.hasExtra(EXTRA_UNHIDE_STATION_LIST)) {
                unhide.addAll(ai.getStringArrayListExtra(EXTRA_UNHIDE_STATION_LIST));
            }

            for (Station station : mPlayer.getStationList()) {
                Object hidden = station.getOption("hidden");

                Log.i(TAG, "debugging station " + station.getName() + ", " + station.getOption("mobile_app_backgrounds"));


                if ((hidden == null) ||
                        ((hidden instanceof Boolean) && (!(Boolean) hidden)) ||
                        unhide.contains(station.getName())) {

                    visibleStations.add(station);
                }
            }
        }

        return visibleStations.toArray(new Station[visibleStations.size()]);
    }

    /**
     * Return the index of the station that should be visible when
     * the activity starts.
     *
     * @param stations the list of available stations
     * @return index into returned array of station to display on activity startup
     */

    private int selectDefaultStation(Station[] stations) {

        Intent ai = getIntent();

        Station activeStation = mPlayer.getStation();

        String displayStation = ai.getStringExtra(EXTRA_DEFAULT_STATION);
        int displayStationTabIndex = -1;
        int activeStationTabIndex = 0;

        for (int i = 0; i < stations.length; i++) {
            Station station = stations[i];

            if (station.getId().equals(activeStation.getId())) {
                activeStationTabIndex = i;
            }

            if (station.getName().equals(displayStation)) {
                displayStationTabIndex = i;
            }
        }

        int selectedTab = (displayStationTabIndex > -1) ? displayStationTabIndex : activeStationTabIndex;

        return selectedTab;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // update the station selection buttons based on the current station
        mPlayer.onPlayerAvailability(new PlayerAvailabilityListener() {
            @Override
            public void onAvailable() {
                // hardware buttons control music volume
                setVolumeControlStream(AudioManager.STREAM_MUSIC);

                // at this point we might review the list of stations
                // and make sure the tuned-in one still exists
            }

            @Override
            public void onUnavailable() {
                Toast.makeText(PlayerActivity.this, "Sorry, music is not available in this location.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * When the user tunes in to one of our stations, make sure the
     * notification_small that shows what is playing points to the tuned-in
     * station.
     *
     * @param station
     */

    @Override
    public void onStationStartedPlayback(Station station) {
        Log.d(TAG, "updating notification_small intent to point to " + station.getName());

        Intent ai = new Intent(getIntent());
        ai.putExtra(EXTRA_DEFAULT_STATION, station.getName());

        PendingIntent pi = PendingIntent.getActivity(this, 0, ai, PendingIntent.FLAG_UPDATE_CURRENT);

        mPlayer.setPendingIntent(pi);
    }

    /**
     * Display the 'powered by feed.fm' page
     */

    @Override
    public void onClickPoweredBy() {
        FragmentManager fm = getSupportFragmentManager();

        String tag = PoweredByFeedFragment.class.getSimpleName();

        PoweredByFeedFragment fragment = (PoweredByFeedFragment) fm.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = new PoweredByFeedFragment();
        }

        fm
                .beginTransaction()
                .replace(R.id.feedPlayer, fragment, PoweredByFeedFragment.class.getSimpleName())
                .addToBackStack(PoweredByFeedFragment.class.getSimpleName())
                .commit();

    }
}
