package com.winlator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gate activity: on first load, user must sign in with Steam.
 * Once signed in, we start MainActivity and finish.
 * In-app debug log: tap "Show debug log" to see Steam sign-in events.
 */
public class LaunchGateActivity extends AppCompatActivity {
    private static final String TAG = "SteamSignIn";
    private static final int DEBUG_LOG_MAX = 200;
    private static final List<String> debugLog = new ArrayList<>();

    /** Append a line to the in-app debug log (and Log.d). Tap "Show debug log" on the sign-in screen to view. */
    public static void dlog(String message) {
        String line = String.format(Locale.US, "[%tT] %s", System.currentTimeMillis(), message);
        synchronized (debugLog) {
            debugLog.add(line);
            while (debugLog.size() > DEBUG_LOG_MAX) debugLog.remove(0);
        }
        Log.d(TAG, message);
    }

    private static String truncateUrl(String url) {
        if (url == null) return "null";
        if (url.length() <= 180) return url;
        return url.substring(0, 180) + "...(len=" + url.length() + ")";
    }

    /** Build OpenID URL with https return_to (Steam rejects custom schemes). Realm = origin only (no trailing path). */
    private String buildSteamOpenIdUrl() {
        String returnTo = getString(R.string.steam_openid_return_to).split("\\?")[0];
        String realm = returnTo.replaceFirst("/[^/]*$", ""); // e.g. https://example.github.io/Steam_APK
        if (realm.endsWith("/")) realm = realm.substring(0, realm.length() - 1);
        try {
            return "https://steamcommunity.com/openid/login?" +
                "openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0" +
                "&openid.mode=checkid_setup" +
                "&openid.return_to=" + java.net.URLEncoder.encode(returnTo, "UTF-8") +
                "&openid.realm=" + java.net.URLEncoder.encode(realm, "UTF-8") +
                "&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select" +
                "&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select";
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    private SteamAuthPrefs steamAuthPrefs;
    private WebView webView;
    private ProgressBar progressBar;
    /** Prevents handling the same callback twice and ensures we leave to MainActivity once. */
    private boolean callbackHandled;

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

        dlog("onCreate: loading OpenID URL (https return_to)");
        setupWebView();
        webView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        webView.loadUrl(buildSteamOpenIdUrl());

        View showLogBtn = findViewById(R.id.SteamSignInShowLog);
        if (showLogBtn != null) {
            showLogBtn.setOnClickListener(v -> showDebugLogDialog());
        }
    }

    private void showDebugLogDialog() {
        StringBuilder sb = new StringBuilder();
        synchronized (debugLog) {
            for (String line : debugLog) sb.append(line).append("\n");
        }
        if (sb.length() == 0) sb.append("(No log entries yet.)");
        ScrollView scroll = new ScrollView(this);
        TextView text = new TextView(this);
        text.setText(sb.toString());
        text.setPadding(40, 24, 40, 24);
        text.setTextSize(12f);
        scroll.addView(text);
        new AlertDialog.Builder(this)
            .setTitle("Steam sign-in debug log")
            .setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            private String getHttpsCallbackBase() {
                return LaunchGateActivity.this.getString(R.string.steam_openid_return_to).split("\\?")[0];
            }

            private void tryHandleCallback(String url) {
                if (url == null) return;
                boolean isHttpsCallback = url.startsWith(getHttpsCallbackBase());
                boolean isCustomScheme = url.startsWith("gamehub-open://steam/callback");
                if (!isHttpsCallback && !isCustomScheme) return;
                if (url.contains("openid.mode=error")) {
                    dlog("tryHandleCallback: Steam error response, not loading page");
                    handleSteamErrorCallback(url);
                    return;
                }
                if (!url.contains("openid.claimed_id")) return;
                dlog("tryHandleCallback: callback url received (https=" + isHttpsCallback + ")");
                if (callbackHandled) {
                    dlog("tryHandleCallback: already handled, skip");
                    return;
                }
                callbackHandled = true;
                webView.setVisibility(View.GONE);
                dlog("handleSteamCallback( url=" + truncateUrl(url) + " )");
                handleSteamCallback(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                dlog("shouldOverrideUrlLoading(request): " + truncateUrl(url));
                if (url.startsWith(getHttpsCallbackBase())) {
                    tryHandleCallback(url);
                    return true;
                }
                if (url.startsWith("gamehub-open://steam/callback")) {
                    tryHandleCallback(url);
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                dlog("shouldOverrideUrlLoading(string): " + truncateUrl(url));
                if (url != null && url.startsWith(getHttpsCallbackBase())) {
                    tryHandleCallback(url);
                    return true;
                }
                if (url != null && url.startsWith("gamehub-open://steam/callback")) {
                    tryHandleCallback(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                dlog("onPageStarted: " + truncateUrl(url));
                if (url != null && (url.startsWith(getHttpsCallbackBase()) || url.startsWith("gamehub-open://steam/callback"))) {
                    tryHandleCallback(url);
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                dlog("onReceivedError(deprecated) code=" + errorCode + " desc=" + description + " url=" + truncateUrl(failingUrl));
                if (failingUrl != null && failingUrl.startsWith("gamehub-open://steam/callback")) {
                    tryHandleCallback(failingUrl);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                String url = request.getUrl() != null ? request.getUrl().toString() : "null";
                dlog("onReceivedError(request) isMainFrame=" + request.isForMainFrame() + " url=" + truncateUrl(url));
                if (request.isForMainFrame() && request.getUrl() != null) {
                    if (url.startsWith("gamehub-open://steam/callback")) {
                        tryHandleCallback(url);
                    }
                }
            }
        });
    }

    private void handleSteamErrorCallback(String url) {
        String message = getString(R.string.steam_sign_in_failed);
        try {
            int idx = url.indexOf("openid.error=");
            if (idx >= 0) {
                String rest = url.substring(idx + 13);
                int end = rest.indexOf("&");
                String err = end >= 0 ? rest.substring(0, end) : rest;
                message = java.net.URLDecoder.decode(err.replace("+", " "), "UTF-8");
            }
        } catch (Exception ignored) {}
        dlog("handleSteamErrorCallback: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(buildSteamOpenIdUrl());
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
        } catch (Exception e) {
            dlog("handleSteamCallback parse error: " + e.getMessage());
        }

        if (steamId != null && !steamId.isEmpty()) {
            dlog("handleSteamCallback: success steamId=" + steamId + " -> MainActivity");
            steamAuthPrefs.setSignedIn(steamId, getString(R.string.account_signed_in));
            startMainAndFinish();
        } else {
            dlog("handleSteamCallback: failed to parse steamId, retry");
            callbackHandled = false;
            Toast.makeText(this, R.string.steam_sign_in_failed, Toast.LENGTH_SHORT).show();
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(buildSteamOpenIdUrl());
        }
    }

    private void startMainAndFinish() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
