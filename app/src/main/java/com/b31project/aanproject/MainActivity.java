package com.b31project.aanproject;


import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;
import android.util.Log;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;

import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
            PermissionsListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permManage;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private String symbolLayerId= "SYMBOL_LAYER_ID";
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private Point origin;
    private Point destination;
    private DirectionsRoute currRoute;
    private NavigationMapRoute navigationMapRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Load the mapview, create evrything needed
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    public void onMapReady(@NonNull final MapboxMap mapboxMap){
        //Display the map with the appropriate style -- found on the mapbox styles site
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/aanprojectteam/ckg0ewsj31vsk19o83pshzome"),
                new Style.OnStyleLoaded(){
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                        initSearchBtn();
                        style.addImage(symbolIconId, BitmapFactory.decodeResource(
                                MainActivity.this.getResources(), R.drawable.blue_marker_view));
                        style.addSource(new GeoJsonSource(geojsonSourceLayerId));
                        style.addLayer(new SymbolLayer(symbolLayerId, geojsonSourceLayerId)
                                .withProperties(iconImage(symbolIconId), iconOffset(new Float[] {0f, -8f})));
                        TextView box = findViewById(R.id.LocationInfo);
                        FloatingActionButton navBtn = findViewById(R.id.navigateswitch);

                        box.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int visibility = box.getVisibility();
                                if(visibility != View.INVISIBLE){
                                    box.setVisibility(View.INVISIBLE);
                                    Layer markerlayer = style.getLayer(symbolLayerId);
                                    markerlayer.setProperties(visibility(Property.NONE));
                                    navBtn.setVisibility(View.INVISIBLE);
                                    if (navigationMapRoute != null){
                                        navigationMapRoute.removeRoute();
                                    }
                                }
                            }
                        });
                        navBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //TODO: FIX THIS
                                if (currRoute != null){
                                    Intent intent = new Intent(MainActivity.this, Navigation.class);
                                    intent.putExtra("currentRoute", currRoute.toJson());
                                    MainActivity.this.startActivity(intent);
                                }
                            }
                        });
                    }
                });
    }

    private void getRoute(){
        NavigationRoute.builder(MainActivity.this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body() == null){
                            Timber.e("No routes found, make sure you set the right user and access token");
                            return;
                        }else if(response.body().routes().size()<1){
                            Timber.e("No routes");
                            return;
                        }
                        currRoute = response.body().routes().get(0);
                        if (navigationMapRoute != null){
                            navigationMapRoute.removeRoute();
                        }else{
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Timber.e("Error:" + t.getMessage());
                    }
                });
    }


    private void initSearchBtn(){
        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                      .backgroundColor((Color.parseColor("#EEEEEE")))
                                      .limit(10)
                                      .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }


    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle){
        //Enable user location if it can find it already enables, else ask to use permissions
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, loadedMapStyle).build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }else{
            permManage = new PermissionsManager(this);
            permManage.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permManage.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "We need your permission to access your location.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        }else{
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE){
            CarmenFeature selectedCarFeat = PlaceAutocomplete.getPlace(data);
            if (mapboxMap!=null){
                Style style = mapboxMap.getStyle();
                if (style!=null){
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source!=null){
                        source.setGeoJson(FeatureCollection.fromFeatures(new Feature[]
                                {Feature.fromJson(selectedCarFeat.toJson())}));
                    }
                }
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(new LatLng(((Point) selectedCarFeat.geometry()).latitude(),
                                        ((Point) selectedCarFeat.geometry()).longitude()))
                                .zoom(14)
                                .build()), 4000);
                TextView infobox = findViewById(R.id.LocationInfo);
                FloatingActionButton navBtn = findViewById(R.id.navigateswitch);
                infobox.setText(selectedCarFeat.placeName());
                infobox.setVisibility(View.VISIBLE);
                Layer markerlayer = style.getLayer(symbolLayerId);
                markerlayer.setProperties(visibility(VISIBLE));
                if (PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                    LocationComponent locationComponent = mapboxMap.getLocationComponent();
                    Location loc = locationComponent.getLastKnownLocation();
                    origin = Point.fromLngLat(loc.getLongitude(), loc.getLatitude());
                    destination = Point.fromLngLat(((Point) selectedCarFeat.geometry()).longitude(),
                                  ((Point) selectedCarFeat.geometry()).latitude());
                    getRoute();
                    navBtn.setVisibility(View.VISIBLE);
                }else{
                    Timber.e("Something's wrong!");
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}
