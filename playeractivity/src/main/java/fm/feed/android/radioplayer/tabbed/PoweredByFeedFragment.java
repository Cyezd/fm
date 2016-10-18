package fm.feed.android.radioplayer.tabbed;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * This implements the simple disclaimer page that is popped up
 * by the player.
 *
 */
public class PoweredByFeedFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_powered_by_feed, container, false);

        rootView.findViewById(R.id.poweredByFeedCloseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        /**
         * Override default PlayerView Shared Subject title
         */
        return rootView;
    }

}
