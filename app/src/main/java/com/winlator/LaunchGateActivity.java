package com.winlator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Gate activity: on first load, user must sign in with Steam.
 * Once signed in, we start MainActivity and finish.
 */
public class LaunchGateActivity extends AppCompatActivity {
    private static final String STEAM_OPENID_URL =
        "https://steamcommunity.com/openid/login?" +
        "openid.return_to=gamehub-open%3A%2F%2Fsteam%2Fcallback" +
        "&openid.realm=gamehub-open%3A%2F%2F" +
        "&openid.claimed_id=https%3A%2F%2Fsteamcommunity.com%2Fopenid%2Fid%2F" +
        "&openid.identity=https%3A%2F%2Fsteamcommunity.com%2Fopenid%2Fid%2F" +
        "&openid.mode=checkid_setup" +
        "&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0";

    private SteamAuthPrefs steamAuthPrefs;
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        steamAuthPrefs = new SteamAuthPrefs(this);
        if (steamAuthPrefs.isSignedIn()) {
            startMainAndFinish();
            return;
        }

        setContentView(R.layout.activity_launch_gate);
        webView = findViewById(R.id.SteamSignInWebView);
        progressBar = findViewById(R.id.SteamSignInProgress);

        setupWebView();
        webView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        webView.loadUrl(STEAM_OPENID_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("gamehub-open://steam/callback")) {
                    handleSteamCallback(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.startsWith("gamehub-open://steam/callback")) {
                    handleSteamCallback(url);
                }
            }
        });
    }

    private void handleSteamCallback(String url) {
        // Parse openid.claimed_id (may be URL-encoded): .../openid/id/76561198012345678
        String steamId = null;
        try {
            int idx = url.indexOf("openid.claimed_id=");
            if (idx >= 0) {
                String rest = url.substring(idx + 18);
                int end = rest.indexOf("&");
                String claimed = end >= 0 ? rest.substring(0, end) : rest;
                String decoded = java.net.URLDecoder.decode(claimed, "UTF-8");
                int lastSlash = decoded.lastIndexOf("/");
                if (lastSlash >= 0) steamId = decoded.substring(lastSlash + 1).trim();
            }
        } catch (Exception ignored) {}

        if (steamId != null && !steamId.isEmpty()) {
            steamAuthPrefs.setSignedIn(steamId, null);
            startMainAndFinish();
        } else {
            Toast.makeText(this, R.string.steam_sign_in_failed, Toast.LENGTH_SHORT).show();
            webView.loadUrl(STEAM_OPENID_URL);
        }
    }

    private void startMainAndFinish() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
