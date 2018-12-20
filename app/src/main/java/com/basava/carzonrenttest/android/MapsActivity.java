package com.basava.carzonrenttest.android;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.basava.carzonrenttest.R;
import com.basava.carzonrenttest.database.UserLocation;
import com.basava.carzonrenttest.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MapsActivity.class.getSimpleName();
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private GoogleMap mMap;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocationHome, mLocationOffice;
    private boolean mIsCurrentLocationSet, mIsInsideHome, mIsInsideOffice;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        List<UserLocation> userLocations = UserLocation.getUserLocations();
        Log.d(TAG, "userLocations: " + userLocations.size());

        mLocationHome = new Location("Home");
        mLocationHome.setLatitude(12.872582);
        mLocationHome.setLongitude(77.583673);

        mLocationOffice = new Location("Office");
        mLocationOffice.setLatitude(12.894169);
        mLocationOffice.setLongitude(77.574786);

        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDialog();
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private AlertDialog mAlertDialogGPS;

    private void showGPSDialog() {
        if (mAlertDialogGPS != null && mAlertDialogGPS.isShowing()) {
            mAlertDialogGPS.dismiss();
        }

        AlertDialog.Builder gpsDialog = new AlertDialog.Builder(this);
        gpsDialog.setTitle(getStringFromResource(R.string.gps_title));
        gpsDialog.setMessage(getStringFromResource(R.string.gps_message));
        gpsDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        gpsDialog.setPositiveButton(getStringFromResource(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                dialog.dismiss();
            }
        });
        mAlertDialogGPS = gpsDialog.create();
        mAlertDialogGPS.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_location) {
            if (mLastLocation != null)
                showAddLocationDialog();
            else
                showToast(getStringFromResource(R.string.location_not_found));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private AlertDialog mAlertDialogAddLocation;

    private void showAddLocationDialog() {
        AlertDialog.Builder addLocationConfirmationDialog = new AlertDialog.Builder(this);
        addLocationConfirmationDialog.setTitle(getStringFromResource(R.string.add_location_title));
        addLocationConfirmationDialog.setMessage(getStringFromResource(R.string.add_location_message));
        addLocationConfirmationDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        addLocationConfirmationDialog.setPositiveButton(getStringFromResource(R.string.add), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveCurrentLocationToDB(mLastLocation);
                dialog.dismiss();
            }
        });
        mAlertDialogAddLocation = addLocationConfirmationDialog.create();
        mAlertDialogAddLocation.show();
    }

    private void saveCurrentLocationToDB(Location location) {
        if (location != null) {
            UserLocation userLocation = new UserLocation();
            userLocation.latitude = location.getLatitude();
            userLocation.longitude = location.getLongitude();
            userLocation.save();
            showToast(getResources().getString(R.string.location_saved_message));
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }

        addOtherLocationMarker(mLocationHome, "Home");
        addOtherLocationMarker(mLocationOffice, "Office");
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.d(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                if (!mIsCurrentLocationSet) {
                    mIsCurrentLocationSet = true;
                    addCurrentLocationMarker(location);
                }
            }
        }
    };

    /**
     * Add current location marker on map and animate camera to that position
     *
     * @param location
     */
    private void addCurrentLocationMarker(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        // Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerOptions.draggable(true);
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));

        mMap.setOnMarkerDragListener(onMarkerDragListener);
    }

    private GoogleMap.OnMarkerDragListener onMarkerDragListener = new GoogleMap.OnMarkerDragListener() {
        @Override
        public void onMarkerDragStart(Marker marker) {
            Log.d(TAG, "onMarkerDragStart: " + marker.getPosition().latitude);
        }

        @Override
        public void onMarkerDrag(Marker marker) {
            Log.d(TAG, "onMarkerDrag: " + marker.getPosition().latitude);
        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            Log.d(TAG, "onMarkerDragEnd: " + marker.getPosition().latitude);
            checkIfInRadius(marker);
        }
    };

    /**
     * Check if dragged marker is in radius of either Home or Office locations
     *
     * @param marker
     */
    private void checkIfInRadius(Marker marker) {

        // Check home location
        float[] distanceHome = new float[2];

        Location.distanceBetween(marker.getPosition().latitude, marker.getPosition().longitude,
                mLocationHome.getLatitude(), mLocationHome.getLongitude(), distanceHome);
        Log.d(TAG, "distanceHome[0]: " + distanceHome[0]);

        if (distanceHome[0] < Constants.GEOFENCE_RADIUS_IN_METERS) {
            showToast("Inside home");
            vibrateAndSound();
            setWifiState(true);
            mIsInsideHome = true;
        } else if (mIsInsideHome) {
            showToast("Outside home");
            setWifiState(false);
            mIsInsideHome = false;
        }

        // Check office location
        float[] distanceOffice = new float[2];
        Location.distanceBetween(marker.getPosition().latitude, marker.getPosition().longitude,
                mLocationOffice.getLatitude(), mLocationOffice.getLongitude(), distanceOffice);
        Log.d(TAG, "distanceOffice[0]: " + distanceOffice[0]);

        if (distanceOffice[0] < Constants.GEOFENCE_RADIUS_IN_METERS) {
            showToast("Inside office");
            vibrateAndSound();
            setWifiState(true);
            mIsInsideOffice = true;
        } else if (mIsInsideOffice) {
            showToast("Outside office");
            setWifiState(false);
            mIsInsideOffice = false;
        }
    }

    private void vibrateAndSound() {
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(500);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setWifiState(boolean turnOn) {
        boolean isWifiEnabled = mWifiManager.isWifiEnabled();
        Log.d(TAG, "isWifiEnabled: " + isWifiEnabled);
        if (turnOn && !isWifiEnabled) {
            mWifiManager.setWifiEnabled(true);
        } else if (!turnOn && isWifiEnabled) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    /**
     * Add Home and Office locations on map with a circle of given radius
     *
     * @param location
     * @param title
     */
    private void addOtherLocationMarker(Location location, String title) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(title);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.addMarker(markerOptions);

        //Instantiates a new CircleOptions object +  center/radius
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius(Constants.GEOFENCE_RADIUS_IN_METERS)
                .fillColor(0x40ff0000)
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(2);

        // Get back the mutable Circle
        Circle circle = mMap.addCircle(circleOptions);
        // more operations on the circle...
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        if (mMap != null) {
                            mMap.setMyLocationEnabled(true);
                        }
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private String getStringFromResource(int resourceId) {
        return getResources().getString(resourceId);
    }

    private Toast mToast;

    private void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            checkLocationPermission();
        }
    }

//    private GeofencingRequest getGeofencingRequest() {
//        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
//        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
//        builder.addGeofences(mGeofenceList);
//        return builder.build();
//    }

    @Override
    public void onPause() {
        // stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAlertDialogAddLocation != null && mAlertDialogAddLocation.isShowing()) {
            mAlertDialogAddLocation.dismiss();
        }

        if (mAlertDialogGPS != null && mAlertDialogGPS.isShowing()) {
            mAlertDialogGPS.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
    }
}
