package com.example.kcy.megabus;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;

import com.android.volley.toolbox.Volley;


public class ResultActivity extends ListActivity {

    public static Calendar calendarFromMillis(long millis) {
        if (millis==-1) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        // Get contents of search
        Intent intent = getIntent();
        int origin =        intent.getIntExtra("Origin", -1);
        int destination =   intent.getIntExtra("Destination", -1);
        Calendar startDate = calendarFromMillis(intent.getLongExtra("StartDate", -1));
        Calendar endDate = calendarFromMillis(intent.getLongExtra("EndDate", -1));
        Calendar startTime =       calendarFromMillis(intent.getLongExtra("StartTime", -1));
        Calendar endTime =      calendarFromMillis(intent.getLongExtra("EndTime", -1));

        final ProgressDialog progressDialog = ProgressDialog.show(this, "",
                "Loading. Please wait...", true);
        MegabusAPI.getJourneys(Volley.newRequestQueue(this), origin, destination, startDate, endDate, startTime, endTime, journeys -> {
            progressDialog.dismiss();
        }, e -> {/*TODO*/});
    }
}
