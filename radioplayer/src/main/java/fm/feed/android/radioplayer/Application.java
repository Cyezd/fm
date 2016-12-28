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

        // LA Marathon credentials
        Player.setTokens(this, "4f915704f1adc23510d224726ac65e3a4b7778f4", "8806e61a8afd5d22c64c3f2d7491727ca13f8583");

        // demo credentials
        //Player.setTokens(this, "demo", "demo");
    }

}
