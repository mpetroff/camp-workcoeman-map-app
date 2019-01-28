package net.mpetroff.campworkcoemanmap;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.TrackingSettings;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.services.android.telemetry.constants.TelemetryConstants;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FloatingActionButton locationToggle;

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

        /* Make sure phoning home to Mapbox is good and dead. */
        SharedPreferences prefs = this.getSharedPreferences(TelemetryConstants.MAPBOX_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(TelemetryConstants.MAPBOX_SHARED_PREFERENCE_KEY_TELEMETRY_ENABLED, false);
        editor.apply();
        editor.commit();

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
                ui.setZoomControlsEnabled(PreferenceManager.getDefaultSharedPreferences(
                        thisActivity).getBoolean("zoom_buttons", false));

                // Restrict map bounds
                mapboxMap.setLatLngBoundsForCameraTarget(CAMP_BOUNDS);
                mapboxMap.setMinZoomPreference(13);

                /*
                 * Location button toggles between location disabled, track location, and track location
                 * and rotate map based on compass bearing. When map is moved, location remains displayed
                 * but is no longer tracked.
                 */
                locationToggle = (FloatingActionButton) findViewById(R.id.location_toggle);
                locationToggle.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        TrackingSettings tracking = mapboxMap.getTrackingSettings();
                        PackageManager packageManager = getPackageManager();
                        if (tracking.getMyLocationTrackingMode() == MyLocationTracking.TRACKING_NONE) {
                            enableLocation();
                        } else if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)
                                || !packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                            disableLocation();
                        } else if (tracking.getMyBearingTrackingMode() == MyBearingTracking.NONE) {
                            tracking.setMyBearingTrackingMode(MyBearingTracking.COMPASS);
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
            TrackingSettings tracking = mapboxMap.getTrackingSettings();
            mapboxMap.setMyLocationEnabled(true);
            tracking.setMyBearingTrackingMode(MyBearingTracking.NONE);
            tracking.setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
            PackageManager packageManager = getPackageManager();
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)
                    && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
                locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_location_tracking));
            } else {
                locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_location_disabled_black_24dp));
            }
            mapView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    TrackingSettings tracking = mapboxMap.getTrackingSettings();
                    tracking.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
                    tracking.setMyBearingTrackingMode(MyBearingTracking.NONE);
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
        TrackingSettings tracking = mapboxMap.getTrackingSettings();
        mapboxMap.setMyLocationEnabled(false);
        tracking.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
        tracking.setMyBearingTrackingMode(MyBearingTracking.NONE);
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
            mapboxMap.getUiSettings().setZoomControlsEnabled(PreferenceManager.getDefaultSharedPreferences(
                    this).getBoolean("zoom_buttons", false));
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
