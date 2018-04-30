package com.example.kcy.megabus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

//TODO: globally fix issues highlighted by suppressed linter messages

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    // Volley Request Queue
    RequestQueue queue;
    // Google Maps UI fragment
    SupportMapFragment mapFragment;
    // View selector
    enum content { Main, Origin, Destination, Date }
    content current;
    // Views
    View mainView;
    View mapView;
    View dateView;
    TextView mapText;
    Menu navMenu;
    Button continueButton;
    EditText            startDateText;
    DatePickerDialog    startDatePicker;
    EditText            endDateText;
    DatePickerDialog    endDatePicker;
    EditText            startTimeText;
    TimePickerDialog    startTimePicker;
    EditText            endTimeText;
    TimePickerDialog    endTimePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Ask for permissions
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        continueButton = findViewById(R.id.button_continue);
        // Volley API
        queue = Volley.newRequestQueue(this);
        // Get views
        mainView =      findViewById(R.id.content_main);
        mapView =       findViewById(R.id.content_map);
        dateView =      findViewById(R.id.content_date);
        mapText =       findViewById(R.id.map_text);
        // Navigation menu
        navMenu =       navigationView.getMenu();
        navMenu.findItem(R.id.nav_destination).setEnabled(false);
        navMenu.findItem(R.id.nav_times).setEnabled(false);
        navMenu.findItem(R.id.nav_search).setEnabled(false);
        // Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // Pickers
        startDateText = findViewById(R.id.startDateText);
        startDatePicker = TextDatePicker(startDateText, this::setStartDate);
        endDateText = findViewById(R.id.endDateText);
        endDatePicker = TextDatePicker(endDateText, this::setEndDate);
        startTimeText = findViewById(R.id.startTimeText);
        startTimePicker = TextTimePicker(startTimeText, this::setStartTime);
        endTimeText = findViewById(R.id.endTimeText);
        endTimePicker = TextTimePicker(endTimeText, this::setEndTime);
        // Start Button
        findViewById(R.id.button_start).setOnClickListener(v -> show(content.Origin));
        // Search Button
        findViewById(R.id.button_search).setOnClickListener(v -> search());
        // Show home screen
        show(content.Main);
        // Error messages
        AlertDialog noInternetDialog = new AlertDialog.Builder(this)
                .setMessage("No internet connection found. Connect to the internet and try again.")
                .setNegativeButton("Retry", (dialog, id) -> {
                    // Since we're at the start of the activity, just recreate
                    recreate();
                }).setPositiveButton("Ignore", null).create();
        AlertDialog noOriginDialog = new AlertDialog.Builder(this)
                .setMessage("Could not get station information. Check internet connection and try again.")
                .setNegativeButton("Retry", (dialog, id) -> {
                    // Since we're at the start of the activity, just recreate
                    recreate();
                }).setPositiveButton("Exit", (dialog, id ) -> finish()).create();

        // Show loading dialog until origin locations are loaded
        final ProgressDialog progressDialog = ProgressDialog.show(this, "",
                "Loading. Please wait...", true);
        new Thread(() -> {
            final ConnectivityManager connectivityManager = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE));
            try {
                // If we don't have internet
                if(connectivityManager == null ||
                        connectivityManager.getActiveNetworkInfo() == null ||
                        !connectivityManager.getActiveNetworkInfo().isConnected() ||
                        InetAddress.getByName("google.com") == null ) {
                    // Alert the user
                    runOnUiThread(noInternetDialog::show);
                } else {
                    // If we have internet
                    // Get original locations
                    MegabusAPI.getOrigins(queue, cities -> {
                        progressDialog.dismiss();
                        origins = cities;
                    }, error -> {
                        progressDialog.dismiss();
                        runOnUiThread(noOriginDialog::show);
                    });
                }
            } catch (UnknownHostException e) {
                runOnUiThread(noInternetDialog::show);
            }
        }).start();
    }
    // onStart/Pause/Resume is not needed
    // application doesn't need to handle a returning user
    // application doesn't hold resources
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        // If drawer is open, close it
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        // If were not already on the first page
        else if (current != content.Main) {
            // Go back a page
            show(content.values()[current.ordinal()-1]);
        }
        // Super (close the app)
        else {
            super.onBackPressed();
        }
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_home:
                show(content.Main); break;
            case R.id.nav_origin:
                show(content.Origin); break;
            case R.id.nav_destination:
                show(content.Destination); break;
            case R.id.nav_times:
                show(content.Date); break;
            case R.id.nav_search:
                search();
                break;
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    // Shows/hides views as required and
    // Runs logic moving between views
    void show(content c) {
        mainView.setVisibility(View.INVISIBLE);
        mapView.setVisibility(View.INVISIBLE);
        dateView.setVisibility(View.INVISIBLE);
        switch (c) {
            case Main:
                setTitle("Megabus");
                mainView.setVisibility(View.VISIBLE);
                break;
            case Origin:
                setTitle("Select Origin");
                mapView.setVisibility(View.VISIBLE);
                // If we have a previous origin selected, enable continue
                continueButton.setEnabled(origin!=null);
                // If we have a previous origin selected, display name
                mapText.setText(origin!=null?"Selected: " + origin.name:"");
                // Load map and run origin logic
                mapFragment.getMapAsync(originReadyCallback);
                break;
            case Destination:
                setTitle("Select Destination");
                mapView.setVisibility(View.VISIBLE);
                // If we have a previous destination selected, enable continue
                continueButton.setEnabled(destination!=null);
                // If we have a previous destination selected, display name
                mapText.setText(destination!=null?"Selected: " + destination.name:"");
                // Load map and run destination logic
                mapFragment.getMapAsync(destinationReadyCallback);
                break;
            case Date:
                setTitle("Search Dates");
                dateView.setVisibility(View.VISIBLE);
                break;
        }
        // Store current page for use when going back
        current = c;
    }
    void search() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("Origin", origin.id);
        intent.putExtra("Destination", destination.id);
        if(startDate!=null)
            intent.putExtra("StartDate", startDate.getTimeInMillis());
        if(endDate!=null)
            intent.putExtra("EndDate", endDate.getTimeInMillis());
        if(startTime!=null)
            intent.putExtra("StartTime", startTime.getTimeInMillis());
        if(endTime!=null)
            intent.putExtra("EndTime", endTime.getTimeInMillis());
        startActivity(intent);
    }

    City        origin;
    List<City>  origins;
    void        setOrigin(@NonNull City city) {
        // If origin isn't changing return
        if(origin == city) return;
        this.origin = city;
        // Get Destinations
        MegabusAPI.getDestinations(queue, origin.id, cities -> {
            navMenu.findItem(R.id.nav_destination).setEnabled(true);
            destinations = cities;
            // If current destination set and no longer supported remove it
            if(!destinations.contains(destination)) setDestination(null);
        }, e -> Toast.makeText(this, "Unable to get destinations for " + origin.name + ", please try again later", Toast.LENGTH_LONG).show());
    }
    City        destination;
    List<City>  destinations;
    void        setDestination(City city) {
        // If destination isn't changing return
        if(destination == city) return;
        this.destination = city;
        // If new destination is null return
        if(destination == null)
        {
            navMenu.findItem(R.id.nav_times).setEnabled(false);
            navMenu.findItem(R.id.nav_search).setEnabled(false);
            return;
        }
        navMenu.findItem(R.id.nav_times).setEnabled(true);
        // startDate will be set to current day if null
        // endDate will be set to startDate if null
        // Times are optional
        navMenu.findItem(R.id.nav_search).setEnabled(true);
        // Get travel dates
        MegabusAPI.getTravelDates(queue, origin.id, destination.id, (newStart, newEnd) -> {
            // If start date now invalid
            if(startDate != null && newStart.after(startDate)) {
                // Remove start date
                setStartDate(null);
            }
            // If end date now invalid
            if(endDate != null && newEnd.after(endDate)) {
                // Remove end date
                setEndDate(null);
            }
            //Set valid date ranges
            startDatePicker.getDatePicker().setMinDate(newStart.getTimeInMillis());
            endDatePicker.getDatePicker().setMinDate(newStart.getTimeInMillis());
            startDatePicker.getDatePicker().setMaxDate(newEnd.getTimeInMillis());
            endDatePicker.getDatePicker().setMaxDate(newEnd.getTimeInMillis());
        }, e -> Toast.makeText(this, "Unable to get travel dates for " + origin.name + "to " + destination.name + ", please try again later", Toast.LENGTH_LONG).show());
    }
    Calendar    startDate;
    void        setStartDate(Calendar calendar) {
        startDate = calendar;
        if(calendar != null) {
            // If new start after current end, reset end
            if(endDate!=null && startDate.after(endDate)) setEndDate(null);
            // Display date
            startDateText.setText(DateFormat.getDateFormat(this).format(startDate.getTime()));
        } else {
            // If new calendar is null, reset text display
            startDateText.setText("");
        }
    }
    Calendar    endDate;
    void        setEndDate(Calendar calendar) {
        endDate = calendar;
        if(calendar != null) {
            if(startDate!=null && endDate.before(startDate)) setStartDate(null);
            endDateText.setText(DateFormat.getDateFormat(this).format(endDate.getTime()));
        } else {
            endDateText.setText("");
        }
    }
    Calendar    startTime;
    @SuppressLint("DefaultLocale") // Locale should not effect format due to only using Int printing
    void        setStartTime(Calendar calendar) {
        startTime = calendar;
        if(calendar != null) {
            if(endTime!=null && startTime.after(endTime)) setEndTime(null);
            startTimeText.setText(String.format("%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE)));
        } else {
            startTimeText.setText("");
        }
    }
    Calendar    endTime;
    @SuppressLint("DefaultLocale") // Locale should not effect format due to only using Int printing
    void        setEndTime(Calendar calendar) {
        endTime = calendar;
        if(calendar != null) {
            if(startTime!=null && endTime.before(startTime)) setStartTime(null);
            endTimeText.setText(String.format("%02d:%02d", endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE)));
        } else {
            endTimeText.setText("");
        }
    }

    // Functions for Origin selection
    @SuppressLint("SetTextI18n")
    GoogleMap.OnMarkerClickListener originMarkerClickListener = marker -> {
        ((TextView)findViewById(R.id.map_text)).setText("Selected: " + marker.getTitle());
        findViewById(R.id.button_continue).setEnabled(true);
        assert marker.getTag() != null; // Tag should never be null as it is always set at marker creation
        setOrigin((City)marker.getTag());
        return false;
    };
    @SuppressLint("MissingPermission") // NOTE: There is an incorrect permission assumption for mFusedLocationClient.getLastLocation()
            OnMapReadyCallback              originReadyCallback = googleMap ->  {
        // Clear map markers
        googleMap.clear();
        if(origin != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(origin.latitude, origin.longitude)));
        }
        else {
            // Check if we have permission to get GPS
            boolean hasFineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarseLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            // If we have permission
            if (hasFineLocation || hasCoarseLocation) {
                // Get location
                LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                // Set map to center on location, zoom between city/county level
                                // NOTE: zoom from https://developers.google.com/maps/documentation/android-api/views#zoom
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 8));
                            }
                            // If location is null, set default
                            else noLocation(googleMap);
                        });
            }
            // If location access is denied, set default
            else noLocation(googleMap);
        }
        // Add map markers
        for(City city : origins) {
            Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(city.latitude, city.longitude)).title(city.name));
            marker.setTag(city);
        }
        // Enable marker click listener
        googleMap.setOnMarkerClickListener(originMarkerClickListener);
        // Set continue button click action
        continueButton.setOnClickListener(view -> show(content.Destination));
    };
    void noLocation(final GoogleMap googleMap) {
        try {
            Address location = new Geocoder(getApplicationContext()).getFromLocationName("Great Britain", 1).get(0);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 5));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Functions for Destination selection
    @SuppressLint("SetTextI18n")
    GoogleMap.OnMarkerClickListener destinationMarkerClickListener = marker ->{
        if(Objects.equals(origin.name, marker.getTitle())) return true;
        ((TextView)findViewById(R.id.map_text)).setText("Selected: " + marker.getTitle());
        findViewById(R.id.button_continue).setEnabled(true);
        setDestination ((City)marker.getTag());
        return false;
    };
    OnMapReadyCallback              destinationReadyCallback = googleMap ->  {
        // Clear map markers
        googleMap.clear();
        // Add map markers
        for(City city : destinations) {
            Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(city.latitude, city.longitude)).title(city.name));
            marker.setTag(city);
        }
        // Add origin marker
        googleMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(origin.latitude, origin.longitude))
                        .title(origin.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );
        // Enable marker click listener
        googleMap.setOnMarkerClickListener(destinationMarkerClickListener);
        // Set continue button click action
        continueButton.setOnClickListener(view -> show(content.Date));
    };

    // Helper functions to create Pickers tied to EditTexts
    DatePickerDialog TextDatePicker(final EditText editText, Consumer<Calendar> setDate) {
        Calendar calendar = Calendar.getInstance();
        final DatePickerDialog datePickerDialog = new DatePickerDialog(this, (dateView, year, monthOfYear, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, monthOfYear, dayOfMonth);
            setDate.accept(c);
        },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        editText.setOnClickListener(textView -> datePickerDialog.show());
        return datePickerDialog;
    }
    TimePickerDialog TextTimePicker(final EditText editText, Consumer<Calendar> setTime) {
        Calendar calendar = Calendar.getInstance();
        final TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour, minute) -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            setTime.accept(c);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        editText.setOnClickListener(textView -> timePickerDialog.show());
        return timePickerDialog;
    }
}