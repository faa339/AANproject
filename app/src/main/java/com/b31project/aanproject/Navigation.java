
package com.b31project.aanproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.mapboxsdk.Mapbox;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;


import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.base.internal.VoiceUnit;

import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewOptions;
import com.mapbox.navigation.ui.OnNavigationReadyCallback;
import com.mapbox.geojson.Point;

import com.mapbox.navigation.ui.listeners.BannerInstructionsListener;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.SpeechAnnouncementListener;
import com.mapbox.navigation.ui.map.NavigationMapboxMap;


import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;


public class Navigation extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, Callback<DirectionsResponse> {
      private NavigationView navigationView;
      private NavigationMapboxMap navigationMapboxMap;
      private Point origin;
      private Point destination;
      private DirectionsRoute currRoute;
      private User thisUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_navigation);
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        navigationView = findViewById(R.id.navigationView);
        Intent intent = getIntent();
        origin = Point.fromJson(intent.getStringExtra("origin"));
        destination = Point.fromJson(intent.getStringExtra("destination"));
        String path = getFilesDir().getAbsolutePath() + File.separator + "userPrefs.json";
        thisUser = User.getInstance(path);
        navigationView.initialize(this, new CameraPosition.Builder()
                .zoom(16)
                .tilt(5)
                .target(new LatLng(origin.latitude(), origin.longitude()))
                .build());
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        navigationMapboxMap = navigationView.retrieveNavigationMapboxMap();
        getRoute(origin,destination);
    }

    private void getRoute(Point origin, Point destination){
        //Get our route with all required options
        MapboxDirections directions = MapboxDirections.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(origin)
                .destination(destination)
                .steps(true)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .voiceInstructions(thisUser.VoiceInstruct)
                .voiceUnits(VoiceUnit.IMPERIAL)
                .bannerInstructions(true)
                .steps(true)
                .language(Locale.ENGLISH)
                .build();
        directions.enqueueCall(this);
    }


    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if(response.body() == null){
            Timber.e("No route found");
            this.onNavigationFinished();
        }else if(response.body().routes().size() < 1){
            Timber.e("Response empty");
            this.onNavigationFinished();
        }else{
            currRoute = response.body().routes().get(0);
        }

        NavigationViewOptions navigationViewOptions = NavigationViewOptions.builder(this)
                .navigationListener(this)
                .directionsRoute(currRoute)
                .bannerInstructionsListener(new BannerInstructionsListener() {
                    @NonNull
                    @Override
                    public BannerInstructions willDisplay(BannerInstructions instructions) {
                        return instructions;
                    }
                })
                .build();

        navigationView.startNavigation(navigationViewOptions);
    }

    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
        Timber.e("Error: " + t.getMessage());
        this.onNavigationFinished();
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