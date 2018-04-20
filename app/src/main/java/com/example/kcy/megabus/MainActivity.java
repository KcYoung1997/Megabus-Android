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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Context mainContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mainContext = this.getApplicationContext();

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

        // Location Provider API
        final FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Volley API
        RequestQueue queue = Volley.newRequestQueue(this);

        // Get views
        mainView = (View) findViewById(R.id.content_main);
        mapView =  (View) findViewById(R.id.content_map);
        show(content.Main);

        // Start Button
        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                show(content.Origin);
            }
        });

        // Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.content_map);
        // When on the origin selection screen
        originReadyCallback = new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                // Check if we have permission to get GPS
                boolean hasFineLocation = ContextCompat.checkSelfPermission(mainContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                boolean hasCoarseLocation = ContextCompat.checkSelfPermission(mainContext, Manifest.permission.ACCESS_COARSE_LOCATION)
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
                    googleMap.addMarker(new MarkerOptions().position(new LatLng(city.latitude, city.longitude)).title(city.name));
                }
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
        originMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        };

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

    SupportMapFragment mapFragment;
    OnMapReadyCallback originReadyCallback;
    GoogleMap.OnMarkerClickListener originMarkerClickListener;

    List<City> origins;

    enum content { Main, Origin };
    View mainView;
    View mapView;

    void show(content c) {
        mainView.setVisibility(View.INVISIBLE);
        mapView.setVisibility(View.INVISIBLE);
        switch (c) {
            case Main:
                mainView.setVisibility(View.VISIBLE);
                break;
            case Origin:
                mapView.setVisibility(View.VISIBLE);
                mapFragment.getMapAsync(originReadyCallback);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

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
