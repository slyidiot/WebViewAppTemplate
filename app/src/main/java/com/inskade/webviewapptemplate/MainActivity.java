package com.inskade.webviewapptemplate;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private LottieAnimationView loadingAnimationView;
    private LottieAnimationView noInternetAnimationView;
    private TextView appName;
    private View dummyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        findViews();
        dummyView.setOnClickListener(this);
        loadSite();
    }

    private void loadSite() {
        try {
            if (!isConnected()) {
                noInternetAnimationView.setVisibility(View.VISIBLE);
                loadingAnimationView.setVisibility(View.GONE);
                appName.setText(getString(R.string.no_internet));
                dummyView.setVisibility(View.VISIBLE);
            } else {
                noInternetAnimationView.setVisibility(View.GONE);
                loadingAnimationView.setVisibility(View.VISIBLE);
                appName.setText(getString(R.string.loading_text_to_display));
                dummyView.setVisibility(View.GONE);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        loadingAnimationView.setVisibility(View.GONE);
                        appName.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                    }
                });
                webView.loadUrl(getString(R.string.url_to_load));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findViews() {
        webView = findViewById(R.id.main_web_view);
        loadingAnimationView = findViewById(R.id.loading_anim);
        appName = findViewById(R.id.app_name_text);
        dummyView = findViewById(R.id.dummy_for_onclick);
        noInternetAnimationView = findViewById(R.id.no_internet_anim);
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
}
