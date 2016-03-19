package net.mpetroff.campworkcoemanmap;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
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
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_LOCATION = 0;
    private MapView mapView;
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

        /** Initialize map */
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(getString(R.string.map_style));
        mapView.setLatLng(new LatLng(41.88780, -73.04193));
        mapView.setZoom(16);
        mapView.onCreate(savedInstanceState);
        mapView.setLogoVisibility(View.GONE);
        mapView.setAttributionVisibility(View.GONE);
        mapView.setCompassEnabled(true);
        mapView.setTiltEnabled(false);
        mapView.setZoomControlsEnabled(PreferenceManager.getDefaultSharedPreferences(
                this).getBoolean("zoom_buttons", false));

        /**
         * Location button toggles between location disabled, track location, and track location
         * and rotate map based on compass bearing. When map is moved, location remains displayed
         * but is no longer tracked.
         */
        locationToggle = (FloatingActionButton) findViewById(R.id.location_toggle);
        locationToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mapView.getMyLocationTrackingMode() == MyLocationTracking.TRACKING_NONE) {
                    enableLocation();
                } else if (mapView.getMyBearingTrackingMode() == MyBearingTracking.NONE) {
                    mapView.setMyBearingTrackingMode(MyBearingTracking.COMPASS);
                    locationToggle.setImageDrawable(ContextCompat.getDrawable(MapActivity.this,
                            R.drawable.ic_location_bearing_tracking));
                } else {
                    disableLocation();
                }
            }
        });
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                "location_button", true)) {
            locationToggle.setVisibility(View.GONE);
        }

        /**
         * Restrict map bounds. There doesn't seem to be a built-in way to do this yet...
         */
        mapView.addOnMapChangedListener(new MapView.OnMapChangedListener() {
            @Override
            public void onMapChanged(int change) {
                if ((change == MapView.REGION_DID_CHANGE
                        || change == MapView.REGION_DID_CHANGE_ANIMATED
                        || change == MapView.REGION_IS_CHANGING) && !updating) {
                    CameraPosition center = mapView.getCameraPosition();
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
                    float zoom = center.zoom;
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
                        mapView.animateCamera(CameraUpdateFactory.newCameraPosition(
                                        new CameraPosition(newCenter, zoom, 0, 0)), 500,
                                new MapView.CancelableCallback() {
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
            mapView.setMyLocationEnabled(true);
            mapView.setMyBearingTrackingMode(MyBearingTracking.NONE);
            mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
            locationToggle.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_location_tracking));
            mapView.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View view, MotionEvent event) {
                    mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
                    mapView.setMyBearingTrackingMode(MyBearingTracking.NONE);
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
        mapView.setMyLocationEnabled(false);
        mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
        mapView.setMyBearingTrackingMode(MyBearingTracking.NONE);
        locationToggle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_location));
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
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload preferences
        mapView.setZoomControlsEnabled(PreferenceManager.getDefaultSharedPreferences(
                this).getBoolean("zoom_buttons", false));
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                "location_button", true)) {
            locationToggle.setVisibility(View.VISIBLE);
        } else {
            locationToggle.setVisibility(View.GONE);
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
