package com.inskade.webviewapptemplate;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.IOException;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogInterface.OnClickListener {

    private WebView webView;
    private LottieAnimationView loadingAnimationView;
    private LottieAnimationView noInternetAnimationView;
    private TextView noInternetText;
    private ImageView appIcon;
    private View dummyView;

    private AlertDialog dialog;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        findViews();

        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);

        fetchUrlFromFirebase();
        dummyView.setOnClickListener(this);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadSite();
            }
        }, 2500);
    }

    private void fetchUrlFromFirebase() {
        long cacheExpiration = 3600; //in seconds not milliseconds

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
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
                    }
                });
                fetchUrlFromFirebase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findViews() {
        webView = findViewById(R.id.main_web_view);
        loadingAnimationView = findViewById(R.id.loading_anim);
        noInternetText = findViewById(R.id.no_internet_text);
        dummyView = findViewById(R.id.dummy_for_onclick);
        noInternetAnimationView = findViewById(R.id.no_internet_anim);
        appIcon = findViewById(R.id.app_icon);
    }

    public boolean isConnected() throws InterruptedException, IOException {
        String command = "ping -c 1 -w 5 www.google.com";
        return (Runtime.getRuntime().exec(command).waitFor() == 0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dummy_for_onclick:
                loadSite();
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
            builder.setNeutralButton("Rate App", this);
            builder.setNegativeButton("No", this);

            // create and show the alert dialog
            dialog = builder.create();
            dialog.show();

        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case BUTTON_POSITIVE:
                finish();
                break;
            case BUTTON_NEUTRAL:
                //TODO:Add rating feature
                break;
            case BUTTON_NEGATIVE:
                dialog.dismiss();
                break;
        }
    }
}
