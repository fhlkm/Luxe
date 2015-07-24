package demo.maps.com.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import demo.maps.com.adapter.AutocompletePlaceAdapter;

public class MainActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "MainActivity";
    private boolean  is_Gps_Just_Turned_on= false;
    private Location myLocation= null;
    private ProgressDialog dialog= null;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private boolean isOnPause = false;
    /**
     * GoogleApiClient wraps our service connection to Google Play Services and provides access
     * to the user's sign in state as well as the Google's APIs.
     */
    protected GoogleApiClient mGoogleApiClient;
    private AutocompletePlaceAdapter mAdapter;
    private Marker mapMarker;
    private static LatLngBounds BOUNDS_CURRENT_LOCATION = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        isOnPause= false;
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnPause = true;

    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a PlaceAutocomplete object from which we
             read the place ID.
              */
            final AutocompletePlaceAdapter.AutocompletePlace item = mAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(TAG, "Autocomplete item selected: " + item.description);
            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(TAG, "Called getPlaceById to get Place details for " + item.placeId);
        }
    };

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated..
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
       // After we turn on gps, we should do localization again
        } else if (is_Gps_Just_Turned_on) {
            setUpMap();
            is_Gps_Just_Turned_on = false;
        }

    }

    /**
     * This is where we create map, open search function and get current location
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Get Current Location
        if(null == myLocation)
        myLocation = getLocation(locationManager);
        if (null != myLocation) {
            findCurrentLocation(myLocation);
            openSearchFunction();
            // ask the user to open gps
        } else if(!isLocationAccess(locationManager)){
            displayPromptForEnablingGPS(this);
        }else{// we are loading location
            loadingLocation();
        }
    }

    /**
     *Move camera to current location and add Marker
     * @param myLocation Current Location
     */
    public void findCurrentLocation(Location myLocation) {
        if (mMap != null) {
            // set map type
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            // Get latitude of the current location
            double latitude = myLocation.getLatitude();
            // Get longitude of the current location
            double longitude = myLocation.getLongitude();
            // Create a LatLng object for the current location
            LatLng latLng = new LatLng(latitude, longitude);
            BOUNDS_CURRENT_LOCATION = new LatLngBounds(new LatLng(latitude - 1, longitude - 1), new LatLng(latitude + 1, longitude + 1));
            // Show the current location in Google Map
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            // Zoom in the Google Map
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
            mapMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You are here!").snippet("Consider yourself located"));
        }
    }

    /**
     * Open auto complete place search function
     */
    public void openSearchFunction() {
            mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, 0 /* clientId */, this).addApi(Places.GEO_DATA_API).build();
            AutoCompleteTextView mAutocompleteView = (AutoCompleteTextView)findViewById(R.id.autocomplete_places);
            mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);
            mAdapter = new AutocompletePlaceAdapter(this, R.layout.item, null, mGoogleApiClient, BOUNDS_CURRENT_LOCATION, null);
            mAutocompleteView.setAdapter(mAdapter);
    }

    /**
     * Prompt user to open GPS
     *
     * @param activity  current Activity
     */
    public void displayPromptForEnablingGPS(final Activity activity) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "Do you want to open GPS setting?";
        builder.setMessage(message).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int id) {
                        activity.startActivity(new Intent(action));
                        d.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int id) {
                Toast.makeText(MainActivity.this, R.string.open_location_accesss, Toast.LENGTH_SHORT).show();
                d.cancel();
            }
        });
        builder.create().show();
    }

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);
            LatLng latLng = place.getLatLng();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
            if (mapMarker != null) {
                mapMarker.remove();
            }
            mapMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName().toString()).snippet(place.getAddress().toString()));
            Log.i(TAG, "Place details received: " + place.getName());
            places.release();
        }
    };


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        Toast.makeText(this, "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),Toast.LENGTH_SHORT).show();
    }


    /**
     * Return current Location based on GPS,if GPS is turned off , it will return location based on Network
     *
     * @return Current Location
     */
    public Location getLocation(LocationManager locationManager ) {
        Location location = null;
        // getting GPS status
        if(null != locationManager) {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 1000, 1,locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1,locationListener);
            if (!isGPSEnabled && !isNetworkEnabled) {
                return null;
            } else {
                if (isGPSEnabled) {

                    Log.d("GPS", "GPS Enabled");
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (isNetworkEnabled&& location== null) {
                    Log.d("NetWork", "NetWork Enabled");
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        }
        return location;
    }

    public boolean isLocationAccess(LocationManager locationManager){
        if(null != locationManager) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return false;
    }

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "new Location");
            is_Gps_Just_Turned_on= true;
            if (myLocation == null) {
                myLocation = location;
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    // we can only execute onResume when the activity is on foreground
                    if(!isOnPause){
                        onResume();
                    }
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG,"ProviderEnable");
            is_Gps_Just_Turned_on= true;

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

    };

    private void loadingLocation(){
        dialog = new ProgressDialog(this);
        dialog.setMessage("loading...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.show();
    }
}
