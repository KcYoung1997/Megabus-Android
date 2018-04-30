package com.example.kcy.megabus;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.Volley;
import com.example.kcy.megabus.MegabusAPI.Journey;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.toIntExact;


public class ResultActivity extends AppCompatActivity {
    private class JourneyAdapter extends ArrayAdapter<Journey> {
        private List<Journey> journeys;

        JourneyAdapter(Context context, int textViewResourceId, List<Journey> journeys) {
            super(context, textViewResourceId, journeys);
            this.journeys = journeys;
        }

        public int getCount() {
            return journeys.size();
        }

        public Journey getItem(int position) {
            return journeys.get(position);
        }

        SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm", Locale.US);
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.result_item, parent, false);
            }
            Journey j = getItem(position);

            if (j != null) {
                TextView travel_times =   v.findViewById(R.id.travel_times);
                TextView travel_date =    v.findViewById(R.id.travel_date);
                TextView origin =         v.findViewById(R.id.origin);
                TextView destination =    v.findViewById(R.id.destination);
                TextView price =    v.findViewById(R.id.price);
                int minutes = 0;
                if(j.duration.contains("M")){
                    String[] tmp = j.duration.split("M")[0].split("[a-zA-Z]");
                    minutes = Integer.parseInt(tmp[tmp.length-1]);
                }
                int hours = 0;
                if(j.duration.contains("H")){
                    String[] tmp = j.duration.split("H")[0].split("[a-zA-Z]");
                    hours = Integer.parseInt(tmp[tmp.length-1]);
                }
                try {
                    travel_times.setText(
                            String.format(Locale.US, "%s → %s (%02d:%02d)",
                                    timeFormat.format(inputTimeFormat.parse(j.departureDateTime.split("T")[1])),
                                    timeFormat.format(inputTimeFormat.parse(j.arrivalDateTime.split("T")[1])),
                                    hours,
                                    minutes));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                travel_date.setText(j.departureDateTime.split("T")[0]);
                origin.setText(String.format(Locale.US, "%s, %s", j.legs.get(0).origin.cityName, j.legs.get(0).origin.stopName));
                destination.setText(String.format(Locale.US, "%s, %s", j.legs.get(0).destination.cityName, j.legs.get(0).destination.stopName));
                price.setText(String.format(Locale.US, "£%02.2f", j.price));
            }
            return v;
        }
    }

    public static Calendar calendarFromMillis(long millis) {
        if (millis==-1) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

    ListView list;
    Intent createIntent;
    AlertDialog noResultDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setTitle("Results");
        list = findViewById(R.id.list);
        list.setOnItemClickListener(listClick);
        // Get contents of search
        createIntent = getIntent();
        // Define error message
        noResultDialog = new AlertDialog.Builder(this)
                .setMessage("Could not get results. Check internet connection and try again.")
                .setNegativeButton("Retry", (dialog, id) -> {
                    // Since we're at the start of the activity, just recreate
                    recreate();
                })
                .setPositiveButton("Go Back", (dialog, id ) -> finish()).create();
        // Set refresh pull listener
        SwipeRefreshLayout refreshLayout = findViewById(R.id.swiperefresh);
        refreshLayout.setOnRefreshListener(
                () -> {
                    refreshJourneys();
                    refreshLayout.setRefreshing(false);
                }
        );
    }
    // Price grabbing is done in onResume in order to keep prices up to date when reentering the activity
    @Override
    protected void onResume() {
        super.onResume();
        refreshJourneys();
    }

    void refreshJourneys() {
        int origin =        createIntent.getIntExtra("Origin", -1);
        int destination =   createIntent.getIntExtra("Destination", -1);
        Calendar startDate = calendarFromMillis(createIntent.getLongExtra("StartDate", -1));
        Calendar endDate = calendarFromMillis(createIntent.getLongExtra("EndDate", -1));
        Calendar startTime =       calendarFromMillis(createIntent.getLongExtra("StartTime", -1));
        Calendar endTime =      calendarFromMillis(createIntent.getLongExtra("EndTime", -1));

        final ProgressDialog progressDialog = ProgressDialog.show(this, "",
                "Loading. Please wait...", true);

        MegabusAPI.getJourneys(Volley.newRequestQueue(this), origin, destination, startDate, endDate, startTime, endTime, j -> {
            progressDialog.dismiss();
            // Remove any journeys who depart after startTime
            if(startTime != null) {
                j.removeIf(x -> {
                    String[] departTime = x.departureDateTime.split("T")[1].split(":");
                    int departHour = Integer.parseInt(departTime[0]);
                    int departMin = Integer.parseInt(departTime[1]);

                    return (departHour < startTime.get(Calendar.HOUR_OF_DAY) ||
                            (departHour == startTime.get(Calendar.HOUR_OF_DAY) && departMin < startTime.get(Calendar.MINUTE)));
                });
            }
            // Remove any journeys who depart after endTime
            if(endTime != null) {
                j.removeIf(x -> {
                    String[] arriveTime = x.arrivalDateTime.split("T")[1].split(":");
                    int arriveHour = Integer.parseInt(arriveTime[0]);
                    int arriveMin = Integer.parseInt(arriveTime[1]);

                    return (arriveHour > endTime.get(Calendar.HOUR_OF_DAY) ||
                            (arriveHour == endTime.get(Calendar.HOUR_OF_DAY) && arriveMin > endTime.get(Calendar.MINUTE)));
                });
            }
            runOnUiThread(() -> {
                // If no journeys were found
                if(j.size() == 0) {
                    // Warn
                    Toast.makeText(this, "No journeys found. Edit your search and try again.", Toast.LENGTH_SHORT).show();
                    // Return to MainActivity
                    finish();
                }
                // If we're refreshing, reset the spinner
                // TODO: try and reapply the sort on refresh
                ((Spinner)findViewById(R.id.action_spinner)).setSelection(options.length-1);
                JourneyAdapter adapter = new JourneyAdapter(this, R.id.result_item, j);
                list.setAdapter(adapter);
                onContentChanged();
            });
        }, e -> noResultDialog.show());
    }

    ListView.OnItemClickListener listClick = (adapterView, view, pos, id) -> {
        //TODO: ask for passenger count
        Intent intent = new Intent(this, PurchaseActivity.class);
        intent.putExtra("journey", (new Gson()).toJson(adapterView.getItemAtPosition(pos)));
        startActivity(intent);
    };

    final static String[] options =  new String[] {"Price", "Departure Time", "Arrival Time", "Travel Duration", "None"};
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.result, menu);
        // Get spinner
        Spinner spin = (Spinner) menu.findItem(R.id.action_spinner).getActionView();
        // Create an adapter, themed to fit the appbar, using android provided layout, and static string options
        SpinnerAdapter adapter = new ArrayAdapter<String>(new ContextThemeWrapper(this, R.style.AppTheme_AppBarOverlay), R.layout.support_simple_spinner_dropdown_item, options) {
            // Override getCount to not display the "None" option in the list
            @Override
            public int getCount() {
                int count = super.getCount();
                return count > 0 ? count - 1 : count;
            }
        };
        // Set spinner to use adapter
        spin.setAdapter(adapter);
        // Set to display "None"
        spin.setSelection(adapter.getCount());
        // On option selected
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //NOTE: this is not called when the item selected is not changed
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                // If adapter not set, then list is not populated yet and we should not order
                if(list.getAdapter() == null) return;
                String test = (String)adapterView.getItemAtPosition(pos);
                JourneyAdapter adapter = (JourneyAdapter) list.getAdapter();
                switch (test) {
                    case "Price":
                        adapter.journeys.sort((i, j) -> (int)((i.price - j.price)*100));
                        break;

                    case "Departure Time":
                        adapter.journeys.sort((i,j) -> {
                            try {
                                return i.departure().compareTo(j.departure());
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return -1;
                            }
                        });
                        break;
                    case "Arrival Time":
                        adapter.journeys.sort((i,j) -> {
                            try {
                                return i.arrival().compareTo(j.arrival());
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return -1;
                            }
                        });
                        break;
                    case "Travel Duration":
                        adapter.journeys.sort((i,j) -> {
                            try {

                                return toIntExact((i.arrival().getTime() - i.departure().getTime()) - (j.arrival().getTime() - j.departure().getTime()));
                            } catch (ParseException e) {
                                e.printStackTrace();
                                return -1;
                            }
                        });
                        break;
                }
                list.setAdapter(adapter);
                runOnUiThread(() -> onContentChanged());
            }
            // No program logic removes the items, so this will never be called despite being required
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        return true;
    }
}
