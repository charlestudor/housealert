package net.ctudor.housealert;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.content.ClipboardManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;

import static com.google.android.gms.common.GooglePlayServicesUtil.*;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private TextView mTextView;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean mSplashHasShown = false;
    private boolean mTokenIsAvailableLocal = false;
    private String mTokenLocalCopy = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(RegStatus.SENT_TOKEN_TO_SERVER, false);
                mTokenLocalCopy = sharedPreferences.getString("tokenKey", "null");
                mTokenIsAvailableLocal = true;

                if (sentToken) {
                    addMessage("Token retrieved and sent to server!");
                    addMessage("Your token is: " + mTokenLocalCopy);
                } else {
                    addMessage("An error occurred while either fetching the InstanceID token");
                }
            }
        };

        if(!mSplashHasShown) {
            View decorView = getWindow().getDecorView();
            decorView.setBackgroundColor(Color.BLACK);

            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);

            setContentView(R.layout.splash_screen);

            LinearLayout splashLayout = (LinearLayout) findViewById(R.id.splash);
            splashLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideSplash();
                    mSplashHasShown = true;
                }
            });
        } else {
            hideSplash();
        }
    }

    private void createMainLayout() {
        mTextView = (TextView) findViewById(R.id.scrolllog);
        mTextView.setKeyListener(null);

        Button copyButton = (Button) findViewById(R.id.copyTokenButton);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTokenIsAvailableLocal) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("tokenCopy", mTokenLocalCopy));
                    Toast.makeText(getApplicationContext(), "Copied to clipboard.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "No token available.", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button emailButton = (Button) findViewById(R.id.emailTokenButton);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTokenIsAvailableLocal) {
                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                    emailIntent.setType("plain/text");
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My HouseAlert Key");
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "My new HouseAlert key is: " + mTokenLocalCopy);

                    v.getContext().startActivity(Intent.createChooser(emailIntent, "Send mail..."));

                } else {
                    Toast.makeText(getApplicationContext(), "No token available.", Toast.LENGTH_LONG).show();
                }
            }
        });

        addMessage("Initialising...");
        if(checkPlayServices()) {
            addMessage("Google Play Services are available.");
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        addMessage("HouseAlert is active!");
    }

    private void addMessage(String msg) {
        // append the new string
        mTextView.append(msg + "\n");
//        // find the amount we need to scroll.  This works by
//        // asking the TextView's internal layout for the position
//        // of the final line and then subtracting the TextView's height
//        final int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
//        // if there is no need to scroll, scrollAmount will be <=0
//        if (scrollAmount > 0)
//            mTextView.scrollTo(0, scrollAmount);
//        else
//            mTextView.scrollTo(0, 0);
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
            // Start menu activity
            Intent intent = new Intent(this, SettingsMenuActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_exits) {
            // Exit application
            finish();
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(RegStatus.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private void hideSplash() {
        View decorView = getWindow().getDecorView();
        decorView.setBackgroundColor(Color.WHITE);

        // Show the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);

        // Remove splash view
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createMainLayout();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            addMessage("Result Code:" + resultCode);
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }
}
