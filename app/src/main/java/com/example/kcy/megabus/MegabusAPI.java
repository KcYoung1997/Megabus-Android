package com.example.kcy.megabus;

import android.annotation.SuppressLint;
import android.icu.util.Calendar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

interface ErrorCallback {
    void onFailure(Exception e);
}

interface CityCallback {
    void onSuccess(List<City> cities);
}
interface TravelDatesCallback {
    void onSuccess(Calendar start, Calendar end);
}
interface JourneyCallback {
    void onSuccess(List<MegabusAPI.Journey> journeys);
}

class MegabusAPI {
    static private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    class Journey {

        class City {
            String cityName;
            short cityId;
            String stopName;
            String stopId;
        }
        class Leg {
            String carrier;
            byte transportTypeId;
            String departureDateTime;
            String arrivalDateTime;
            String duration;
            float price;
            City origin;
            City destination;
        }
        int journeyId;
        String departureDateTime;
        Date departure() throws ParseException { return format.parse(departureDateTime); }
        String arrivalDateTime;
        Date arrival() throws ParseException { return format.parse(arrivalDateTime); }
        String duration;
        float price;
        City origin;
        City destination;
        ArrayList<Leg> legs;
        String reservableType;
        String serviceInformation;
        String routeName;
        int lowStockCount; // Tickets left OR null
        String promotionCodeStatus;
    }
    // Locale warnings can be ignored as the format is for the megabus API call
    // Also the Megabus site is NodeJS (known) using MomentJS (assumed/tested) so should understand most formats
    // however in this case we're using the format the site uses in requests
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("Y-M-d");
    private static final String baseURL =    "https://uk.megabus.com";
    private static final String originURL =  baseURL+"/journey-planner/api/origin-cities";
    private static       String destURL(int origin) { return baseURL+"/journey-planner/api/destination-cities?originCityId=" + origin; }
    private static       String journeyURL(int origin, int destination) { return baseURL+"/journey-planner/api/journeys/travel-dates?originCityId="+origin+"&destinationCityId="+destination; }


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
    static void getJourneys(RequestQueue queue, int origin, int destination, Calendar startDate, Calendar endDate, Calendar startTime, Calendar endTime, JourneyCallback callback, ErrorCallback errorCallback) {
        if(startDate == null) startDate = Calendar.getInstance();
        if(endDate == null) endDate = ResultActivity.calendarFromMillis(startDate.getTimeInMillis());
        // Ensure that startdate is before enddate when both are the same day
        startDate.set(Calendar.HOUR, 0);
        endDate.set(Calendar.HOUR, 23);
        final List<RequestFuture<String>> futures = new ArrayList<>();
        // API can only return one day at a time
        // so we make multiple requests
        while(!startDate.after(endDate)) {
            RequestFuture<String> future = RequestFuture.newFuture();
            StringRequest request = new StringRequest(Request.Method.GET,
                baseURL + "/journey-planner/api/journeys?" +
                        "originId=" + origin +
                        "&destinationId=" + destination +
                        "&departureDate=" + dateFormat.format(startDate.getTime()) +
                        "&totalPassengers=1", future, future);
            futures.add(future);
            queue.add(request);
            startDate.add(Calendar.DATE, 1);
        }
        Type t = new TypeToken<List<Journey>>() {}.getType();
        Thread awaitFutures = new Thread() {
            @Override
            public void run() {
                List<Journey> ret = new ArrayList<>();
                for(RequestFuture<String> future : futures) {
                    try {
                        JsonArray arr = ((JsonObject)new JsonParser().parse(future.get())).getAsJsonArray("journeys");
                        ret.addAll(new Gson().fromJson(arr, t));
                    } catch (InterruptedException | ExecutionException e)
                    {e.printStackTrace();}
                }
                callback.onSuccess(ret);
            }
        };
        awaitFutures.start();
    }
}
