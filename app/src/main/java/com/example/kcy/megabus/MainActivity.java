package com.example.kcy.megabus;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Location Provider API
    FusedLocationProviderClient mFusedLocationClient;
    Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Ask for permissions TODO: Check first?
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        continueButton = (Button) findViewById(R.id.button_continue);
        // Volley API
        queue = Volley.newRequestQueue(this);
        // Get views
        mainView = (View) findViewById(R.id.content_main);
        mapView =  (View) findViewById(R.id.content_map);
        // Start Button
        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                show(content.Origin);
            }
        });
        // Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        show(content.Main);
        // Get original locations
        MegabusAPI.getOrigins(queue, new CityCallback() {
            @Override
            public void onSuccess(List<City> cities) {
                origins = cities;
            }
            @Override
            public void onFailure(Exception e) {
                //TODO: Should warn about this as the app can't function without this list
                e.printStackTrace();
            }
        });
    }

    // Volley Request Queue
    RequestQueue queue;
    // Google Maps UI fragment
    SupportMapFragment mapFragment;
    // View selector
    enum content { Main, Origin, Destination, Date };
    content current;
    // Views
    View mainView;
    View mapView;

    // Functions for Origin selection
    OnMapReadyCallback originReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(final GoogleMap googleMap) {
            // Check if we have permission to get GPS
            boolean hasFineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarseLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            // If we have permission
            if (hasFineLocation || hasCoarseLocation) {
                // Get location
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    // Set map to center on location, zoom between city/county level
                                    // NOTE: zoom from https://developers.google.com/maps/documentation/android-api/views#zoom
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 8));
                                }
                                // If location is null, set default
                                else noLocation(googleMap);
                            }
                        });
            }
            // If location access is denied, set default
            else noLocation(googleMap);
            // Add map markers
            for(City city : origins) {
                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(city.latitude, city.longitude)).title(city.name));
                marker.setTag(city);
            }
            // Enable marker click listener
            googleMap.setOnMarkerClickListener(originMarkerClickListener);
            // Set continue button click action
            continueButton.setOnClickListener(originContinueClickListener);
            // Clear Location text
            ((TextView)findViewById(R.id.map_text)).setText("");
        }
        public void noLocation(final GoogleMap googleMap) {
        try {
            Address location = new Geocoder(getApplicationContext()).getFromLocationName("Great Britain", 1).get(0);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 5));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
};
    GoogleMap.OnMarkerClickListener originMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            ((TextView)findViewById(R.id.map_text)).setText("Selected: " + marker.getTitle());
            ((Button)findViewById(R.id.button_continue)).setEnabled(true);
            origin = (City)marker.getTag();
            // Reset destination list
            destination = null;
            return false;
        }
        public void test(){} //TODO: remove this in final build, just used to force android studio to collapse properly
    };
    View.OnClickListener originContinueClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            show(content.Destination);
        }
    };
    City origin;
    List<City> origins;

    // Functions for Destination selection
    OnMapReadyCallback destinationReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(final GoogleMap googleMap) {
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
            continueButton.setOnClickListener(destinationContinueClickListener);
            // Clear Location text
            ((TextView)findViewById(R.id.map_text)).setText("");
        }
        public void test(){} //TODO: remove this in final build, just used to force android studio to collapse properly
    };
    GoogleMap.OnMarkerClickListener destinationMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if(Objects.equals(origin.name, marker.getTitle())) return true;
            ((TextView)findViewById(R.id.map_text)).setText("Selected: " + marker.getTitle());
            ((Button)findViewById(R.id.button_continue)).setEnabled(true);
            destination = (City)marker.getTag();
            return false;
        }
        public void test(){} //TODO: remove this in final build, just used to force android studio to collapse properly
    };
    View.OnClickListener destinationContinueClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            show(content.Date);
        }
    };
    City destination;
    List<City> destinations;



    void show(content c) {
        mainView.setVisibility(View.INVISIBLE);
        mapView.setVisibility(View.INVISIBLE);
        switch (c) {
            case Main:
                setTitle("Megabus");
                mainView.setVisibility(View.VISIBLE);
                break;
            case Origin:
                setTitle("Select Origin");
                mapView.setVisibility(View.VISIBLE);
                mapFragment.getMapAsync(originReadyCallback);
                break;
            case Destination:
                setTitle("Select Destination");
                mapView.setVisibility(View.VISIBLE);
                // Get Destinations
                MegabusAPI.getDestinations(queue, origin.id, new CityCallback() {
                    @Override
                    public void onSuccess(List<City> cities) {
                        destinations = cities;
                        mapFragment.getMapAsync(destinationReadyCallback);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        //TODO: Error handling
                    }
                });
                break;
        }
        // Store current page for use when going back
        current = c;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (current != content.Main) {
            // Go back a page
            show(content.values()[current.ordinal()-1]);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
