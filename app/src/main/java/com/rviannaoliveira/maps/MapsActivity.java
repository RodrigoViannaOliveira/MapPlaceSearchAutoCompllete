package com.rviannaoliveira.maps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Criado por rodrigo on 30/07/16.
 */
public class MapsActivity extends FragmentActivity implements MapsView,OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient googleApiClient;
    public static final LatLng SAO_PAULO = new LatLng(-23.586950299999998, -46.682218999999996);
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final int MARKER_COARSE = 123;
    private static final String TAG = ">>>>>>";
    private Geocoder geo;
    private GoogleMap map;
    private AutoCompleteTextView autocompleteView;
    private PlaceAutocompleteAdapter adapter;
    private Marker marker;
    private ViewEventHelper viewEventHelper;
    private MapsPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        presenter = new MapsPresenterImpl(this);
        viewEventHelper = new ViewEventHelper(this);
        presenter.setup();
        MapsUtil.permissionLocationNear(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsEventHelper mapsHelper = new MapsEventHelper(this, googleMap);
        map = googleMap;
        map.setOnMarkerDragListener(mapsHelper.eventDrag);
        map.setOnInfoWindowClickListener(mapsHelper.eventSaveMarker);
        map.setOnMyLocationChangeListener(mapsHelper.myLocationChangeListener);
        map.setOnMyLocationButtonClickListener(mapsHelper.eventMyLocationButton);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.markerDefault();
        }
        autocompleteView.setOnItemClickListener(viewEventHelper.autocompleteClickListener(adapter, mapsHelper,autocompleteView,googleApiClient));
    }

    @Override
    public void setupMap() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, null)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        geo = new Geocoder(this.getApplicationContext(), Locale.getDefault());

        SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void setupView() {
        autocompleteView = (AutoCompleteTextView) this.findViewById(R.id.autoCompleteTextView);
        adapter = new PlaceAutocompleteAdapter(this, googleApiClient, null, null);
        autocompleteView.setAdapter(adapter);
        Button clearButton = (Button) this.findViewById(R.id.button_clear);
        Button save        = (Button) this.findViewById(R.id.confirm_local_place);
        save.setOnClickListener(viewEventHelper.eventSave);
        clearButton.setOnClickListener(viewEventHelper.eventClearSearch(autocompleteView));
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)  {
            return;
        }
        if (marker == null) {
            presenter.setupMarker(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
        }
    }
    @Override
    public void onConnectionSuspended(int i) {
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        this.markerDefault();
    }

    @Override
    public void markerLastLocation(Location mLastLocation) {
        LatLng latLngCurrent = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        marker = map.addMarker(new MarkerOptions().position(latLngCurrent).draggable(true));
        this.configureMarker(latLngCurrent,this.getResources().getInteger(R.integer.nvl_zoom_start));
    }


    @Override
    public void configureMarker(LatLng latLngCurrent, int nvlZoom){
        try {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCurrent,nvlZoom));
            List<Address> addresses = geo.getFromLocation(latLngCurrent.latitude, latLngCurrent.longitude, 1);
            marker.setTitle(MapsUtil.formatLocalityAutoComplete(addresses.get(0).getLocality(), addresses.get(0).getSubLocality()));
            marker.setSnippet(MapsUtil.formatAddressAutoComplete(addresses.get(0).getThoroughfare(), addresses.get(0).getSubThoroughfare()));
            marker.showInfoWindow();
        } catch (IOException e) {
            Log.i(TAG,e.getMessage());
        }
    }

    @Override
    public void markerDefault(){
        if(map != null){
            marker = map.addMarker(new MarkerOptions().position(MapsActivity.SAO_PAULO).draggable(true));
            this.settingsGoogleMapDefault();
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(MapsActivity.SAO_PAULO, this.getResources().getInteger(R.integer.nvl_zoom_start)));
        }
    }

    @Override
    public void settingsGoogleMapDefault(){
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(true);
        this.locationButton();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MARKER_COARSE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission
                    .ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                    .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            this.locationButton();
        }
    }

    void locationButton() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            return;
        }

        map.setMyLocationEnabled(true);
        View locationButton = ((View) this.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.setMargins(0, 250, 180, 0);
        locationButton.setLayoutParams(rlp);
    }

    void save(){
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(MapsActivity.LATITUDE, String.valueOf(marker.getPosition().latitude));
        bundle.putString(MapsActivity.LONGITUDE, String.valueOf(marker.getPosition().longitude));
        intent.putExtras(bundle);
        this.setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker){
        this.marker = marker;
    }
}
