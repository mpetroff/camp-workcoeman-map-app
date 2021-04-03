package net.mpetroff.campworkcoemanmap;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FloatingActionButton locationToggle;
    private boolean locationComponentActivated = false;

    private static final LatLngBounds CAMP_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(41.904, -73.032))
            .include(new LatLng(41.878, -73.058))
            .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));

        setTitle(R.string.app_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name)));
        }
        setContentView(R.layout.activity_map);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final MapActivity thisActivity = this;

        /* Initialize map */
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap m) {
                mapboxMap = m;
                UiSettings ui = mapboxMap.getUiSettings();
                ui.setLogoEnabled(false);
                ui.setAttributionEnabled(false);
                ui.setCompassEnabled(true);
                ui.setTiltGesturesEnabled(false);

                // Restrict map bounds
                mapboxMap.setLatLngBoundsForCameraTarget(CAMP_BOUNDS);
                mapboxMap.setMinZoomPreference(13);
                mapboxMap.setMaxZoomPreference(20);

                // Set map style
                mapboxMap.setStyle(new Style.Builder().fromUri("asset://map-data/style.json"));

                /*
                 * Location button toggles between location disabled, track location, and track location
                 * and rotate map based on compass bearing. When map is moved, location remains displayed
                 * but is no longer tracked.
                 */
                locationToggle = (FloatingActionButton) findViewById(R.id.location_toggle);
                locationToggle.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        LocationComponent locationComponent = mapboxMap.getLocationComponent();
                        if (!locationComponentActivated) {
                            LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                                    .builder(thisActivity, mapboxMap.getStyle())
                                    .build();
                            locationComponent.activateLocationComponent(locationComponentActivationOptions);
                            locationComponentActivated = true;
                        }
                        PackageManager packageManager = getPackageManager();
                        if (!locationComponent.isLocationComponentEnabled()) {
                            enableLocation();
                        } else if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)
                                || !packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                            disableLocation();
                        } else if (locationComponent.getCameraMode() == CameraMode.TRACKING) {
                            locationComponent.setCameraMode(CameraMode.TRACKING_COMPASS);
                            locationToggle.setImageDrawable(ContextCompat.getDrawable(MapActivity.this,
                                    R.drawable.ic_location_bearing_tracking));
                        } else {
                            disableLocation();
                        }
                    }
                });
                if (!PreferenceManager.getDefaultSharedPreferences(thisActivity).getBoolean(
                        "location_button", true)) {
                    locationToggle.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Handles permissions checking to enabled location services and enables location tracking.
     */
    private void enableLocation() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSIONS_LOCATION);
        } else {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.setLocationComponentEnabled(true);
            PackageManager packageManager = getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)
                    && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_location_tracking));
                locationComponent.setRenderMode(RenderMode.COMPASS);
            } else {
                locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_location_disabled_black_24dp));
                locationComponent.setRenderMode(RenderMode.NORMAL);
            }
            locationComponent.setCameraMode(CameraMode.TRACKING);
            mapView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    LocationComponent locationComponent = mapboxMap.getLocationComponent();
                    locationComponent.setLocationComponentEnabled(false);
                    locationComponent.setCameraMode(CameraMode.NONE);
                    locationToggle.setImageDrawable(ContextCompat.getDrawable(MapActivity.this,
                            R.drawable.ic_location));
                    return false;
                }
            });
        }
    }

    /**
     * Disable location tracking / display.
     */
    private void disableLocation() {
        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        locationComponent.setLocationComponentEnabled(false);
        locationComponent.setCameraMode(CameraMode.NONE);
        locationToggle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location));
    }

    @Override
    public void onLowMemory() {
        super.onStart();
        mapView.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload preferences
        if (mapboxMap != null) {
            if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                    "location_button", true)) {
                locationToggle.setVisibility(View.VISIBLE);
            } else {
                locationToggle.setVisibility(View.GONE);
            }
        }
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation();
                } else {
                    View view = getCurrentFocus();
                    if (view != null) {
                        Snackbar bar = Snackbar.make(view,
                                R.string.snackbar_denied_location, Snackbar.LENGTH_LONG);
                        bar.setAction(R.string.settings, new View.OnClickListener() {
                            public void onClick(View view) {
                                Intent intent = new Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getPackageName()));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                        bar.show();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
