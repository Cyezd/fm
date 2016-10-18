
This is a demo Android app that makes use of the Feed Media SDK
to power a Pandora-style Internet-radio app. The app has a sample
custom notification layout and shows how you can use station
metadata to hide stations and assign images and
descriptive text to them.

This player will retrieve the list of stations do display
from the server and display or hide the station selector
accordingly. Background images for the music stations
are provided to the client along with station meta-data.
If background aren't provided by the server, there is a
default image included with this code.

*If you want to add a music player to your app with minimal
coding, see instructions below.*

Check out our [iOS version](https://github.com/feedfm/iOS-RadioPlayer) as well!

!["tabbed station page"](images/tune.png)
!["music playing"](images/playing.png)
!["notification layout"](images/notification.png)
!["lockscreen"](images/lockscreen.png)

# Add the player to your app

You can add this player to your app with minimal coding by
following the steps below:

- Download this repository

- Open your app in Android Studio, then select `File > New > Import Module` and select
  the `playeractivity` directory from this repository.

- After importing, a new `playeractivity` module should appear in your app. Make
  your app dependent on this new module by opening your app's `build.gradle` file
  and adding the following to the `dependencies` section:

```groovy
dependencies {
  // ...

  compile project(':playeractivity')

  // ...
}
```

- Then, you need to make sure you intialize the Feed.fm library when
your app starts up. This should happen in a custom `Application`
subclass:

```java

import fm.feed.android.Player;
// ...

public class Application extends android.app.Application {

  // ...

  public void onCreate() {
    super.onCreate();

    // ...

    // initialize player
    Player.setTokens(this, "sdkdemo", "sdkdemo");
  }

  // ...

}
```

Replace "sdkdemo" with credentials given to you by Feed.fm.

- In any activity that will link to the music player, you
should default to hiding the buttons to that access the player.
Then add the following to the Activity's `onCreate` method
to un-hide the buttons once music is confirmed available:

```java
import fm.feed.android.playersdk.Player;
import fm.feed.android.playersdk.PlayerAvailabilityListener;

public class MyActivity extends AppCompatActivity {

	// ...

  protected void onCreate(Bundle savedInstanceState) {
    // ...

    // this should be done before calling findViewById below
    setContenView(/* ... */);

		// make buttons visible if radio is available
		Player.getInstance().onPlayerAvailability(new PlayerAvailabilityListener() {
				@Override
				public void onAvailable() {
        		// In this method, make all your hidden buttons that access
            // radio visible.
						findViewById(R.id.playerButton).setVisibility(View.VISIBLE);
				}

				@Override
				public void onUnavailable() {
            // You can leave this empty if, by default, you do not offer
            // access to music in your UI. 

            // If you really want to disappoint your user, you could run the following:
						Toast.makeText(MyActivity.this, "Sorry, music is not available to you right now", Toast.LENGTH_LONG).show();
				}
		});

    // ...
  }
}
```

- Now, to pull up the music player activity, construct an Intent with
the following:

```java
import fm.feed.android.radioplayer.tabbed.PlayerActivity;

// ...

Intent ai = new Intent(mContext, PlayerActivity.class);

// if you want an 'up' button displayed, then tell the player
// where the up button goes to:
ai.putExtra(PlayerActivity.EXTRA_PARENT_ACTIVITY, new Intent(mContext, MainActivity.class));

// (or, alternatively, edit the activity in the `playeractivity`
// module to have an `android:parentActivityName` attribute)

startActivity(ai);

```

- Run your app, sit back, and groove!


