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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.TrackingSettings;
import com.mapbox.mapboxsdk.maps.UiSettings;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FloatingActionButton locationToggle;
    private boolean updating = false;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name)));
        }
        setContentView(R.layout.activity_map);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final MapActivity thisActivity = this;

        /** Make sure phoning home to Mapbox is good and dead. */
        SharedPreferences prefs = this.getSharedPreferences(MapboxConstants.MAPBOX_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MapboxConstants.MAPBOX_SHARED_PREFERENCE_KEY_TELEMETRY_ENABLED, false);
        editor.apply();
        editor.commit();

        /** Initialize map */
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(getString(R.string.map_style));
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap m) {
                mapboxMap = m;
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.88780, -73.04193), 16));
                UiSettings ui = mapboxMap.getUiSettings();
                ui.setLogoEnabled(false);
                ui.setAttributionEnabled(false);
                ui.setCompassEnabled(true);
                ui.setTiltGesturesEnabled(false);
                ui.setZoomControlsEnabled(PreferenceManager.getDefaultSharedPreferences(
                        thisActivity).getBoolean("zoom_buttons", false));

                /**
                 * Location button toggles between location disabled, track location, and track location
                 * and rotate map based on compass bearing. When map is moved, location remains displayed
                 * but is no longer tracked.
                 */
                locationToggle = (FloatingActionButton) findViewById(R.id.location_toggle);
                locationToggle.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        TrackingSettings tracking = mapboxMap.getTrackingSettings();
                        if (tracking.getMyLocationTrackingMode() == MyLocationTracking.TRACKING_NONE) {
                            enableLocation();
                        } else if (tracking.getMyBearingTrackingMode() == MyLocationTracking.TRACKING_NONE) {
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

        mapView.onCreate(savedInstanceState);

        /**
         * Restrict map bounds. There doesn't seem to be a built-in way to do this yet...
         */
        mapView.addOnMapChangedListener(new MapView.OnMapChangedListener() {
            @Override
            public void onMapChanged(int change) {
                if ((change == MapView.REGION_DID_CHANGE
                        || change == MapView.REGION_DID_CHANGE_ANIMATED
                        || change == MapView.REGION_IS_CHANGING) && !updating && mapboxMap != null) {
                    CameraPosition center = mapboxMap.getCameraPosition();
                    LatLng newCenter = new LatLng(center.target);
                    boolean centerChange = false;
                    if (center.target.getLatitude() > 41.904) {
                        newCenter.setLatitude(41.904);
                        updating = centerChange = true;
                    } else if (center.target.getLatitude() < 41.878) {
                        newCenter.setLatitude(41.878);
                        updating = centerChange = true;
                    }
                    if (center.target.getLongitude() > -73.032) {
                        newCenter.setLongitude(-73.032);
                        updating = centerChange = true;
                    } else if (center.target.getLongitude() < -73.058) {
                        newCenter.setLongitude(-73.058);
                        updating = centerChange = true;
                    }
                    float zoom = (float) center.zoom;
                    if (center.zoom < 13) {
                        zoom = 13;
                        updating = true;
                    }
                    if (updating) {
                        disableLocation();
                        if (centerChange && (toast == null
                                || !toast.getView().isShown())) {
                            toast = Toast.makeText(getApplicationContext(), R.string.toast_left_camp,
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newCenter, zoom), 500,
                                new MapboxMap.CancelableCallback() {
                                    public void onFinish() {
                                        disableLocation();
                                        updating = false;
                                    }

                                    public void onCancel() {
                                        disableLocation();
                                        updating = false;
                                    }
                                });
                    }
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
            //mapView.setMyLocationEnabled(true);
            tracking.setMyBearingTrackingMode(MyBearingTracking.NONE);
            tracking.setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
            locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_location_tracking));
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
        //mapView.setMyLocationEnabled(false);
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
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
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
