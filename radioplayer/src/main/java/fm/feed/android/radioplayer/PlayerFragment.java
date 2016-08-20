package fm.feed.android.radioplayer;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import fm.feed.android.playersdk.NavListener;
import fm.feed.android.playersdk.Player;
import fm.feed.android.playersdk.PlayerError;
import fm.feed.android.playersdk.PlayerListener;
import fm.feed.android.playersdk.PlayerState;
import fm.feed.android.playersdk.model.Play;
import fm.feed.android.playersdk.model.Station;
import fm.feed.android.playersdk.service.PlayInfo;
import fm.feed.android.playersdk.view.StationButton;

/**
 *
 * Simple fragment to render a music station.
 *
 * This fragment must be provided with a station name that the fragment
 * will be rendering. The newInstance() convenience methods handle that
 * for you.
 *
 * The fragment searches for a resource that is an array of drawables
 * that will be periodically swapped out in the fragment. The array
 * has an id that is
 * constructed by converting the station name to lower case, removing
 * all non-alphanumeric characters, converting strings of spaces to
 * a single '_', and then appending "_backgrounds". So, "My Cool  Station"
 * is converted to "my_cool_station_backgrounds" and and we expect
 * to find an array of drawables. If no resource with that id is found,
 * we look for "default_backgrounds".
 *
 * The fragment has three viewgroups that it makes visible at different times:
 *   tuneInView - this is displayed when the current station is not
 *      being played.
 *   tuningView - this is displayed when we are stalled waiting for audio data
 *      to arrive over the network.
 *   playerControlsView - this holds player controls that are revealed
 *      when this station is playing music in the current station.
 *
 */

public class PlayerFragment extends Fragment {

    public final static String TAG = PlayerFragment.class.getSimpleName();

    public final static String EXTRA_STATION_NAME = "fm.feed.android.radioplayer.stationName";
    public final static String EXTRA_BACKGROUND_OFFSET = "fm.feed.android.radioplayer.backgroundOffset";

    /**
     * Frequency of background rotations
     */

    private static long MINIMUM_STATION_ROTATION_TIME_IN_SECONDS = 5;

    public Player mPlayer;

    private boolean mUserInteraction; // true if the user has tapped to request music

    private Station mStation;

    private View mTuneInView;
    private View mTuningView;
    private View mPlayerControlsView;

    private ImageView mBackgroundImageView;

    /**
     * This listener is called when various things in the station
     * fragment are poked.
     */

    public interface PlayerFragmentEventListener {
        public void onStationStartedPlayback(Station station);
        public void onClickPoweredBy();
    }

    private PlayerFragmentEventListener mPlayerFragmentEventListener;

    public static PlayerFragment newInstance(String name, PlayerFragmentEventListener psListener) {
        PlayerFragment pf = new PlayerFragment();
        Bundle args = new Bundle(1);
        args.putString(EXTRA_STATION_NAME, name);
        pf.setArguments(args);

        if (psListener != null) {
            pf.mPlayerFragmentEventListener = psListener;
        }

        return pf;
    }

    public static PlayerFragment newInstance(String name) {
        return PlayerFragment.newInstance(name, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlayer = Player.getInstance();

        Bundle args = getArguments();
        if (args != null) {
            String stationName = args.getString(EXTRA_STATION_NAME);
            mStation = mPlayer.getStationWithName(stationName);

            if (mStation == null) {
                Log.w(TAG, "PlayerFragment created with station name '" + stationName + "', but no such station exists");
                // default to active station to keep this boat floatin'
                mStation = mPlayer.getStation();
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        mTuneInView = rootView.findViewById(R.id.tuneInView);
        mTuningView = rootView.findViewById(R.id.tuningView);
        mPlayerControlsView = rootView.findViewById(R.id.playerControlsView);
        mBackgroundImageView = (ImageView) rootView.findViewById(R.id.backgroundImageView);

        // 'powered by feed.fm' link
        Button poweredByText = (Button) rootView.findViewById(R.id.powered_by_playing);
        poweredByText.setOnClickListener(onClickPoweredByText);
        poweredByText = (Button) rootView.findViewById(R.id.powered_by_tuning);
        poweredByText.setOnClickListener(onClickPoweredByText);
        poweredByText = (Button) rootView.findViewById(R.id.powered_by_tune_in);
        poweredByText.setOnClickListener(onClickPoweredByText);

        // update the view when we switch stations
        mPlayer.registerNavListener(mNavListener);
        mPlayer.registerPlayerListener(mPlayerListener);

        // clicking on this button will now start playback
        // in this station
        StationButton sb = (StationButton) rootView.findViewById(R.id.startButton);
        sb.setStationName(mStation.getName());

        // watch start button to distinguish between pre-buffering
        // and buffering
        PlayerState state = mPlayer.getState();
        mUserInteraction = (state != PlayerState.READY) && (state != PlayerState.TUNING) && (state != PlayerState.TUNED);
        sb.setOnClickListener(onClickStationButton);

        assignBackground();

        // show controls or 'tune in!' text
        displayMetadataGroupOrNot();

        return rootView;
    }

    /**
     * Make a note when the user clicks to start music playback
     * in the current station.
     */

    private View.OnClickListener onClickStationButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // if the player is TUNING but the user hasn't interacted with
            // anything yet, then assume we're pre-loading stuff in the background
            // and don't show the spinner.
            mUserInteraction = true;

            if (mPlayerFragmentEventListener != null) {
                mPlayerFragmentEventListener.onStationStartedPlayback(mPlayer.getStationWithName(mStation.getName()));
            }
        }
    };

    private View.OnClickListener onClickPoweredByText = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayerFragmentEventListener != null) {
                mPlayerFragmentEventListener.onClickPoweredBy();
            }
        }
    };

    /**
     * When the player state changes, update the displayed metadata
     **/

    private PlayerListener mPlayerListener = new PlayerListener() {
        @Override
        public void onPlayerInitialized(PlayInfo playInfo) {
            // don't care
        }

        @Override
        public void onPlaybackStateChanged(PlayerState playerState) {
            displayMetadataGroupOrNot();
        }

        @Override
        public void onSkipStatusChange(boolean b) {
            // don't care
        }

        @Override
        public void onError(PlayerError playerError) {
            displayMetadataGroupOrNot();
        }
    };

    /**
     * When we change stations or run out of music, update
     * the displayed metadata.
     */

    private NavListener mNavListener = new NavListener() {
        @Override
        public void onStationChanged(Station station) {
            displayMetadataGroupOrNot();
        }

        @Override
        public void onTrackChanged(Play play) {
            // don't care
        }

        @Override
        public void onEndOfPlaylist() {
            displayMetadataGroupOrNot();
        }

        @Override
        public void onSkipFailed() {
            // maybe do a Toast here to tell user they'll get more
            // skips in a little while?
        }

        @Override
        public void onBufferUpdate(Play play, int percentage) {
            // don't care
        }

        @Override
        public void onProgressUpdate(Play play, int elapsedTime, int totalTime) {
            // don't care
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        mPlayer.unregisterNavListener(mNavListener);
        mPlayer.unregisterPlayerListener(mPlayerListener);
    }

    /**
     * Determine which of the tuneInView, tuningView, or playerControlsView
     * do display
     */

    private void displayMetadataGroupOrNot() {
        Station station = mPlayer.getStation();
        PlayerState state = mPlayer.getState();

        Log.i(TAG, "determining metadata display, state is " + state.name());

        if (!mUserInteraction
                || (station == null)
                || (state == PlayerState.READY)
                || (state == PlayerState.TUNED)
                || (state == PlayerState.UNAVAILABLE)) {
            Log.i(TAG, "showing tune in text, state is " + state);

            // display 'tune in! text
            mTuneInView.setVisibility(View.VISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);

        } else if (!station.getId().equals(mStation.getId())) {
            Log.i(TAG, "showing tune in text, since station does not match");

            // display controls for non-tuned in stations
            mTuneInView.setVisibility(View.VISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);

        } else if (state == PlayerState.TUNING) {
            Log.i(TAG,"showing 'tuning...' text since we're tuning");

            // show tuning text if that's what we're doing
            mTuneInView.setVisibility(View.INVISIBLE);
            mTuningView.setVisibility(View.VISIBLE);
            mPlayerControlsView.setVisibility(View.INVISIBLE);

        } else {
            Log.i(TAG, "showing controls");

            // display controls
            mTuneInView.setVisibility(View.INVISIBLE);
            mTuningView.setVisibility(View.INVISIBLE);
            mPlayerControlsView.setVisibility(View.VISIBLE);

            // update the lock screen background
            assignLockScreen();

        }
    }

    private void assignLockScreen() {
        String bgUrl;

        try {
            bgUrl = (String) mStation.getOption("background_image_url");
        } catch (ClassCastException e) {
            bgUrl = null;
        }

        if (bgUrl != null) {
            final String bgRef  = bgUrl;

            Glide.with(this).load(bgUrl).asBitmap().into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    // set this image for the lockscreen and notifications if we're
                    // playing this station
                    mPlayer.setArtwork(resource);
                }
            });
        } else {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.default_station_background);

            mPlayer.setArtwork(bm);
        }
    }

    private void assignBackground() {
        // find a bitmap and assign it to 'bm'
        String bgUrl;

        try {
            bgUrl = (String) mStation.getOption("background_image_url");
        } catch (ClassCastException e) {
            bgUrl = null;
        }

        if (bgUrl != null) {
            Glide.with(this).load(bgUrl).centerCrop().into(mBackgroundImageView);

        } else {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.default_station_background);

            // update background image
            mBackgroundImageView.setImageBitmap(bm);
        }
    }

}
