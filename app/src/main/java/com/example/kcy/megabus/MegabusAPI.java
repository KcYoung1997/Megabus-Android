package com.example.kcy.megabus;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

interface ErrorCallback {
    void onFailure(Exception e);
}

interface CityCallback {
    void onSuccess(List<City> cities);
}
interface TravelDatesCallback {
    void onSuccess(Calendar start, Calendar end);
}

class MegabusAPI {
    private static final String baseURL =    "https://uk.megabus.com";
    private static final String originURL =  baseURL+"/journey-planner/api/origin-cities";
    private static       String destURL(int origin) { return baseURL+"/journey-planner/api/destination-cities?originCityId=" + origin; }
    private static       String journeyURL(int origin, int destination) { return baseURL+"/journey-planner/api/journeys/travel-dates?originCityId="+origin+"&destinationCityId="+destination; };


    static void getOrigins(RequestQueue queue, CityCallback callback, ErrorCallback errorCallback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, originURL,
                response -> {
                    // Get Cities Array
                    JsonArray arr = ((JsonObject)new JsonParser().parse(response)).getAsJsonArray("cities");
                    // Parse as City List
                    List<City> origins = new Gson().fromJson(arr, new TypeToken<List<City>>() {}.getType());
                    callback.onSuccess(origins);
                }, errorCallback::onFailure);
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    static void getDestinations(RequestQueue queue, int origin, final CityCallback callback, ErrorCallback errorCallback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, destURL(origin),
                response -> {
                    // Get Cities Array
                    JsonArray arr = ((JsonObject)new JsonParser().parse(response)).getAsJsonArray("cities");
                    // Parse as City List
                    List<City> destinations = new Gson().fromJson(arr, new TypeToken<List<City>>() {}.getType());
                    callback.onSuccess(destinations);
                }, errorCallback::onFailure);
        queue.add(stringRequest);
    }
    static void getTravelDates(RequestQueue queue, int origin, int destination, final TravelDatesCallback callback, ErrorCallback errorCallback) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, journeyURL(origin, destination),
                response -> {
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    // Get Cities Array
                    JsonArray arr = ((JsonObject)new JsonParser().parse(response)).getAsJsonArray("availableDates");
                    List<String> dates = new Gson().fromJson(arr, new TypeToken<List<String>>() {}.getType());

                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    try {
                        start.setTime(format.parse(dates.get(0)));
                        end.setTime(format.parse(dates.get(dates.size()-1)));
                    } catch (ParseException e) {
                        errorCallback.onFailure(e);
                    }
                    callback.onSuccess(start, end);
                }, errorCallback::onFailure);
        queue.add(stringRequest);
    }
}
