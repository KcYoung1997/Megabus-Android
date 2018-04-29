package com.example.kcy.megabus;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
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

import com.android.volley.toolbox.Volley;
import com.example.kcy.megabus.MegabusAPI.Journey;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static java.lang.Math.toIntExact;


public class ResultActivity extends AppCompatActivity {
    private class JourneyAdapter extends ArrayAdapter<Journey> {
        private List<Journey> journeys;
        private LayoutInflater inflater = null;
        private Context context;

        public JourneyAdapter (Context context, int textViewResourceId, List<Journey> journeys) {
            super(context, textViewResourceId, journeys);
            try {
                this.context = context;
                this.journeys = journeys;

                inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            } catch (Exception e) {

            }
        }

        public int getCount() {
            return journeys.size();
        }

        public Journey getItem(int position) {
            return journeys.get(position);
        }

        public long getItemId(Journey journey) {
            return journeys.indexOf(journey);
        }

        SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm");
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.result_item, null);
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
                            String.format("%s → %s (%02d:%02d)",
                                    timeFormat.format(inputTimeFormat.parse(j.departureDateTime.split("T")[1])),
                                    timeFormat.format(inputTimeFormat.parse(j.arrivalDateTime.split("T")[1])),
                                    hours,
                                    minutes));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //TODO from API's parsed dates
                travel_date.setText(j.departureDateTime.split("T")[0]);
                origin.setText(j.legs.get(0).origin.cityName + ", " + j.legs.get(0).origin.stopName);
                destination.setText(j.legs.get(j.legs.size()-1).destination.cityName + ", " + j.legs.get(j.legs.size()-1).destination.stopName);
                price.setText(String.format("£%02.2f", j.price));
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        setTitle("Results");
        list = findViewById(R.id.list);
        list.setOnItemClickListener(listClick);
        // Get contents of search
        createIntent = getIntent();
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
            //TODO order price by default
            runOnUiThread(() -> {
                JourneyAdapter adapter = new JourneyAdapter(this, R.id.result_item, j);
                list.setAdapter(adapter);
                onContentChanged();
            });
        }, e -> {/*TODO*/});
    }


    ListView.OnItemClickListener listClick = (adapterView, view, pos, id) -> {
        Intent intent = new Intent(this, PurchaseActivity.class);
        intent.putExtra("journey", (new Gson()).toJson((Journey)adapterView.getItemAtPosition(pos)));
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
        //TODO: store previous selection to avoid pointless ordering
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
