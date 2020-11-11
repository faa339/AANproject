
package com.b31project.aanproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;


import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

public class Navigation extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener {
    private NavigationView navigationView;
    private NavigationMapboxMap navigationMapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);

        navigationView.initialize(this);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        Intent intent = getIntent();
        String jsonRoute = intent.getStringExtra("currentRoute");
        DirectionsRoute currentRoute = DirectionsRoute.fromJson(jsonRoute);

        if(navigationView.retrieveNavigationMapboxMap() != null && currentRoute!=null){
            navigationMapboxMap = navigationView.retrieveNavigationMapboxMap();
            NavigationViewOptions options = NavigationViewOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(true)
                    .build();
            //navigationView.startNavigation(options); Todo: Fix the error this causes
        }
    }

    @Override
    public void onCancelNavigation() {
        navigationView.stopNavigation();
        finish();
    }

    @Override
    public void onNavigationFinished() {
        finish();
    }

    @Override
    public void onNavigationRunning() {

    }
}