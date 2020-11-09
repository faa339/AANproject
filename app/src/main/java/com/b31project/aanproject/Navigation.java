
package com.b31project.aanproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.navigation.ui.OnNavigationReadyCallback;
import com.mapbox.navigation.ui.listeners.NavigationListener;

import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

public class Navigation extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener {
    private NavigationView navigationView;
    private DirectionsRoute currentRoute;
    private NavigationMapboxMap navigationMapboxMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_navigation);
        Intent intent = getIntent();
        String jsonRoute = intent.getStringExtra("currentRoute");
        currentRoute = DirectionsRoute.fromJson(jsonRoute);
        navigationView = findViewById(R.id.navigationView);
        navigationView.initialize(this::onNavigationReady);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        if(navigationView.retrieveNavigationMapboxMap() != null && currentRoute!=null){
            navigationMapboxMap = navigationView.retrieveNavigationMapboxMap();
            NavigationViewOptions options = NavigationViewOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(true)
                    .build();
            navigationView.startNavigation(options);
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