package com.example.kcy.megabus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PurchaseActivity extends AppCompatActivity {

    MegabusAPI.Journey journey;
    WebView webView;
    CookieManager cookieManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get journey from intent
        Intent intent = getIntent();
        journey = (new Gson()).fromJson(intent.getStringExtra("journey"), MegabusAPI.Journey.class);
        // If no journey, return (Handles hackers opening activities manually)
        if (journey == null) finish();
        webView = new WebView(this);
        getSupportActionBar().hide();
        setContentView(webView);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        // Removes cookie policy warning on megabus's site
        cookieManager.setCookie("https://uk.megabus.com", "AcceptedCookiePolicy=");
        // Don't run networking on UI thread
        new Thread(this::getBasket).start();
    }

    void getBasket() {

        String postData = "{\"journeys\":[{\"journeyId\":\"" + journey.journeyId + "\",\"passengerCount\":1,\"concessionCount\":0,\"nusCount\":0,\"otherDisabilityCount\":0,\"wheelchairSeatedCount\":0,\"pcaCount\":0}]}";
        JSONObject json = null;
        try {
            json = new JSONObject(postData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                "https://uk.megabus.com/journey-planner/api/basket/add",
                json,
        response -> runOnUiThread(() -> webView.loadUrl("https://uk.megabus.com/journey-planner/basket")),
        error -> {
            Toast.makeText(this, "Unable to order a ticket, please try again.", Toast.LENGTH_SHORT).show();
            // Return to results activity
            finish();
        })
        {
            @Override public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<>();
                params.put("referer", "https://uk.megabus.com/journey-planner/journeys/return");
                //params.put("content-type","application/json");
                //params.put("Content-Type","application/json");
                params.put("accept","application/json, text/plain, */*");
                params.put("authority","uk.megabus.com");
                return params;
            }
            @Override public String getBodyContentType()
            {
                return "application/json";
            }
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                // Due to the fact that the cookies we need to resend are marked secure
                // Android does not seem to give us access to them
                // Potential fix: https://stackoverflow.com/questions/30310627/reading-secure-cookies-in-android-webview
                // Did not work
                // So this function will parse the cookies from the response headers manually
                for(Header header : response.allHeaders) {
                    if(Objects.equals(header.getName(), "Set-Cookie")) {
                        String cookie = header.getValue().split(";")[0];
                        cookieManager.setCookie("https://uk.megabus.com", cookie);
                    }
                }
                cookieManager.flush();
                return super.parseNetworkResponse(response);
            }
        };
        queue.add(request);
    }
}

