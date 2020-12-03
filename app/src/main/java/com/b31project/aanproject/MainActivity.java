package com.b31project.aanproject;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
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
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
            PermissionsListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permManage;
    private final String geojsonSourceLayerId = "geojsonSourceLayerId";
    private final String symbolIconId = "symbolIconId";
    private final String symbolLayerId= "SYMBOL_LAYER_ID";
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private Point origin;
    private Point destination;

    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    String feature ="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Load the mapview, create evrything needed
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if(FirstTime(preferences, editor)){
            //Switch activities to the preferences screen
            Toast.makeText(this, "First time!", Toast.LENGTH_SHORT).show();
        }

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    public boolean FirstTime(SharedPreferences prefs, SharedPreferences.Editor editor){
        if(prefs.getBoolean("FirstTime", true)){
            editor.putBoolean("FirstTime", false);
            editor.commit();
            editor.apply();
            return true;
        }else{
            return false;
        }
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
                                }
                            }
                        });
                        navBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, Navigation.class);
                                intent.putExtra("origin", origin.toJson());
                                intent.putExtra("destination", destination.toJson());
                                MainActivity.this.startActivity(intent);
                            }
                        });
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
        Toast.makeText(this, "Permission Granted!", Toast.LENGTH_LONG).show();
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
            finish();
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

                //-------------------Start of Database Data Retrieval---------------
                //GOAL: Modify the infobox with accessibility data in a location
                //Make method to retrieve any database information on a building and store in ArrayList
                //String accessibilityData = getDBLocation(selectedCarFeat.placeName().toUpperCase());


                //Retrieve references to the data in the database.
                String LocationInfo = selectedCarFeat.placeName().toUpperCase();
                String city = LocationInfo.split(",")[1];
                city = String.copyValueOf(city.toCharArray(), 1, city.length()-1);
                LocationInfo = String.join("" , Arrays.copyOfRange(LocationInfo.split(","),0,3));

                DatabaseReference reference = database.getReference("LOCATIONS").child(city).child(LocationInfo);

                //Get data from the references and compare against the LocationName that we are looking for
                //Once/if a match is found, then return the accessibility info.

                ArrayList<String> features = new ArrayList<>();
                readData(new FirebaseCallback() {
                    @Override
                    public void onCallback(String value) {
                        features.add(LogUtils.LOGD("MainActivity/getDBLocation/readData/onCallback", feature));
                        infobox.setText(selectedCarFeat.placeName() + "\nFeature(s):" + feature);
                        infobox.invalidate();
                    }
                }, reference);
                Log.d("MainActivity/onActivityResult", "Before infobox is visible, feature =" + feature);
                infobox.setText(selectedCarFeat.placeName() + '\n' + feature);
                infobox.setVisibility(View.VISIBLE);
                Layer markerlayer = style.getLayer(symbolLayerId);
                markerlayer.setProperties(visibility(VISIBLE));
                if (PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                    LocationComponent locationComponent = mapboxMap.getLocationComponent();
                    Location loc = locationComponent.getLastKnownLocation();
                    origin = Point.fromLngLat(loc.getLongitude(), loc.getLatitude());
                    destination = Point.fromLngLat(((Point) selectedCarFeat.geometry()).longitude(),
                                  ((Point) selectedCarFeat.geometry()).latitude());
                    navBtn.setVisibility(View.VISIBLE);
                }else{
                    Timber.e("Something's wrong!");
                }
            }
        }
    }
    private interface FirebaseCallback {
        void onCallback(String value);
    }
    private void readData(FirebaseCallback myCallback, DatabaseReference databaseReference) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child("FEATURE").exists()) {
                    feature = snapshot.child("FEATURE").getValue(String.class);
                    Log.d("MainActivity/getDBLocation/readData/ValueEventListener", "set feature to " + feature);
                }
                myCallback.onCallback(feature);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
    //Gets database information on accessibility
    public String getDBLocation(String LocationInfo) {
        //Retrieve references to the data in the database.

        String city = LocationInfo.split(",")[1];
        city = String.copyValueOf(city.toCharArray(), 1, city.length()-1);
        LocationInfo = String.join("" , Arrays.copyOfRange(LocationInfo.split(","),0,3));

        DatabaseReference reference = database.getReference("LOCATIONS").child(city).child(LocationInfo);

        //Get data from the references and compare against the LocationName that we are looking for
        //Once/if a match is found, then return the accessibility info.

        ArrayList<String> features = new ArrayList<>();

        readData(new FirebaseCallback() {
            @Override
            public void onCallback(String value) {
                features.add(LogUtils.LOGD("MainActivity/getDBLocation/readData/onCallback", feature));
            }
        }, reference);

        Log.d("MainActivity/getDBLocation", "Before return, feature is set to " + feature);
        return feature;
    }

    public static class LogUtils {

        public static String LOGD(String tag, String message) {
            Log.d(tag, message);
            return message;
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
