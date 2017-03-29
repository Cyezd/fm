package fm.feed.android.radioplayer;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import fm.feed.android.playersdk.Player;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        // initialize player
        Player.setTokens(this, "sdkdemo", "sdkdemo");
    }

}
