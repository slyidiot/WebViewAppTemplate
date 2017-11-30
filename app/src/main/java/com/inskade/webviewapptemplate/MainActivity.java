package com.inskade.webviewapptemplate;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.IOException;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener, View.OnTouchListener, Handler.Callback {

    private static final int EXIT_DIALOG = 0;
    private static final int RATE_US_DIALOG = 1;
    private static final int CLICK_ON_WEBVIEW = 1;
    private static final int CLICK_ON_URL = 2;
    private final Handler handler = new Handler(this);
    private int currentDialog = -1;
    private int interstitialAdCounter = 0;
    private WebView webView;
    private LottieAnimationView loadingAnimationView;
    private LottieAnimationView noInternetAnimationView;
    private TextView noInternetText;
    private ImageView appIcon;
    private View dummyView;
    private AdView adView;
    private InterstitialAd interstitialAd;
    private SharedPreferences sharedPreferences;
    private AlertDialog dialog;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(this.getPackageName() + "_SHARED_PREFERENCES", MODE_PRIVATE);

        findViews();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setOnTouchListener(this);

        fetchUrlFromFirebase();
        dummyView.setOnClickListener(this);
        loadAds();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadSite();
            }
        }, 2500);
    }

    private void loadAds() {
        MobileAds.initialize(this, getString(R.string.ad_mob_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        interstitialAd = new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                interstitialAd.show();
            }
        });
    }

    private void fetchUrlFromFirebase() {

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = 3600; //in seconds not milliseconds

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //noinspection StatementWithEmptyBody
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            //handle errors
                        }
                        loadUrl();
                    }
                });
    }

    private void loadUrl() {
        webView.loadUrl(mFirebaseRemoteConfig.getString("url_to_load"));
    }

    private void loadSite() {
        try {
            if (!isConnected()) {
                noInternetAnimationView.setVisibility(View.VISIBLE);
                noInternetText.setVisibility(View.VISIBLE);
                loadingAnimationView.setVisibility(View.GONE);
                noInternetText.setText(getString(R.string.no_internet));
                dummyView.setVisibility(View.VISIBLE);
                appIcon.setVisibility(View.GONE);
            } else {
                noInternetAnimationView.setVisibility(View.GONE);
                loadingAnimationView.setVisibility(View.VISIBLE);
                noInternetText.setVisibility(View.GONE);
                dummyView.setVisibility(View.GONE);
                appIcon.setVisibility(View.VISIBLE);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        loadingAnimationView.setVisibility(View.GONE);
                        noInternetText.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        appIcon.setVisibility(View.GONE);
                        adView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        handler.sendEmptyMessage(CLICK_ON_URL);
                        return false;
                    }
                });
                fetchUrlFromFirebase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInterstitialAd() {
        interstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void findViews() {
        webView = findViewById(R.id.main_web_view);
        loadingAnimationView = findViewById(R.id.loading_anim);
        noInternetText = findViewById(R.id.no_internet_text);
        dummyView = findViewById(R.id.dummy_for_onclick);
        noInternetAnimationView = findViewById(R.id.no_internet_anim);
        appIcon = findViewById(R.id.app_icon);
        adView = findViewById(R.id.adView);
    }

    public boolean isConnected() throws InterruptedException, IOException {
        String command = "ping -c 1 -w 5 www.google.com";
        return (Runtime.getRuntime().exec(command).waitFor() == 0);
    }

    public void rateApp() {

        sharedPreferences.edit().putBoolean("app_rated", true).apply();

        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dummy_for_onclick:
                loadSite();
                break;
            case R.id.main_web_view:
                Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
            builder.setTitle("Exit?");
            builder.setMessage("Are you sure you want to exit?");
            builder.setPositiveButton("Yes", this);
            builder.setNegativeButton("No", this);
            currentDialog = EXIT_DIALOG;

            // create and show the alert dialog
            dialog = builder.create();
            dialog.show();

        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case BUTTON_POSITIVE:
                if (currentDialog == EXIT_DIALOG) {
                    if (!sharedPreferences.getBoolean("app_rated", false)) {
                        showRateAppDialog();
                    } else {
                        finish();
                    }
                } else if (currentDialog == RATE_US_DIALOG) {
                    rateApp();
                }
                break;
            case BUTTON_NEUTRAL:
                if (currentDialog == RATE_US_DIALOG) {
                    finish();
                }
                break;
            case BUTTON_NEGATIVE:
                if (currentDialog == EXIT_DIALOG) {
                    dialog.dismiss();
                } else if (currentDialog == RATE_US_DIALOG) {
                    sharedPreferences.edit().putBoolean("app_rated", true).apply();
                }
                break;
        }
    }

    private void showRateAppDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        builder.setTitle("Rate This App");
        builder.setMessage("If you enjoy using this app, would you mind taking a moment to rate it? It won't take more than a minute. Thank you for your support!");
        builder.setPositiveButton("Rate Now", this);
        builder.setNeutralButton("Later", this);
        builder.setNegativeButton("No, Thanks", this);
        currentDialog = RATE_US_DIALOG;

        // create and show the alert dialog
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == CLICK_ON_URL) {
            interstitialAdCounter += 1;
            if (interstitialAdCounter == 4) {
                interstitialAdCounter = 0;
                showInterstitialAd();
            }
            return true;
        }
        if (message.what == CLICK_ON_WEBVIEW) {
            handler.removeMessages(CLICK_ON_URL);
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view.getId() == R.id.main_web_view && motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            handler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 500);
        }
        return false;
    }

}
