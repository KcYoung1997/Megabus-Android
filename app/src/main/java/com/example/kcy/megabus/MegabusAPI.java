package com.example.kcy.megabus;

import android.support.v4.util.Pair;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

interface CityCallback {
    void onSuccess(List<City> cities);
    void onFailure(Exception e);
}

public class MegabusAPI {
    public static final String baseURL =    "https://uk.megabus.com";
    public static final String originURL =  baseURL+"/journey-planner/api/origin-cities";
    public static final String destURL =    baseURL+"/journey-planner/api/destination-cities";
    public static final String journeyURL = baseURL+"/journey-planner/api/journeys";


    static public void getOrigins(RequestQueue queue, final CityCallback callback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, originURL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // Get Cities Array
                    JsonArray arr = ((JsonObject)new JsonParser().parse(response)).getAsJsonArray("cities");
                    // Parse as City List
                    List<City> origins = new Gson().fromJson(arr, new TypeToken<List<City>>() {}.getType());
                    callback.onSuccess(origins);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //TODO: Error
                }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    static public void getDestinations(RequestQueue queue, CityCallback callback) {
    }
}
