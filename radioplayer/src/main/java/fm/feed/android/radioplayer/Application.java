package fm.feed.android.radioplayer;

/**
 * Created by ericlambrecht on 6/19/15.
 * Copyright Feed Media, 2016
 */

import fm.feed.android.playersdk.Player;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        //  ** note ** - if your app is just being started to 
        //     respond to a push notification, and not actually
        //     display an activity that could lead to music, 
        //     you might want to skip this.
  
        // initialize player
        Player.setTokens(this, "sdkdemo", "sdkdemo");
    }

}
