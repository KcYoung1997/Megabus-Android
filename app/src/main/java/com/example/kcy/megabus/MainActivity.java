package com.example.kcy.megabus;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

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

        final FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Criteria criteria = new Criteria();

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
        originReadyCallback = new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                // Check if we have permission to get GPS
                boolean hasFineLocation = ContextCompat.checkSelfPermission(mainContext, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                boolean hasCoarseLocation = ContextCompat.checkSelfPermission(mainContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                if (hasFineLocation || hasCoarseLocation) {
                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        // Logic to handle location object
                                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                                                .zoom(17)                   // Sets the zoom
                                                .bearing(90)                // Sets the orientation of the camera to east
                                                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                                                .build();                   // Creates a CameraPosition from the builder
                                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                    }
                                    else noLocation();
                                }
                            });
                }
                else noLocation();
            }
            public void noLocation() {

            }
        };
    }

    SupportMapFragment mapFragment;
    OnMapReadyCallback originReadyCallback;


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
        if (id == R.id.action_settings) {
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
