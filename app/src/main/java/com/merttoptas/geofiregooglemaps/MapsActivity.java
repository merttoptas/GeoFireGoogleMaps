package com.merttoptas.geofiregooglemaps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;


import javax.annotation.Nonnull;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener {

    private GoogleMap mMap;
    Marker myCurrent;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    FirebaseFirestore mDb;
    private FusedLocationProviderClient mFusedLocationClient;
    GeoFire geoFire;
    double radius = 500;
    GeoQuery geoQuery;
    DatabaseReference ref;
    Circle mapCircle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDb = FirebaseFirestore.getInstance();
        //firebase
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //sample of database referance name
        ref = FirebaseDatabase.getInstance().getReference("offerAvailable");
        geoFire = new GeoFire(ref);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

         mMap = googleMap;

         mlocationRequest();

         if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);


            }else{
                checkLocationPermission();
            }

        }

    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(final Location location : locationResult.getLocations()){

                mLastLocation = location;

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("offerAvailable");
                GeoFire geoFire = new GeoFire(ref);

                if(mLastLocation !=null){

                    final double latitude = mLastLocation.getLatitude();
                    final double longitude = mLastLocation.getLongitude();

                    geoFire.setLocation("You",new GeoLocation(latitude, longitude),
                             new GeoFire.CompletionListener() {
                                 @Override
                                 public void onComplete(String key, DatabaseError error) {

                                     //ad marker
                                     if(myCurrent !=null){
                                         myCurrent.remove();
                                         myCurrent = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(latitude,longitude))
                                                .title("You'are"));

                                         //move camera
                                         mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude), 15.0f));
                                     }

                                 }
                             });
                }
                setCircle(location);
                geoQuery.addGeoQueryEventListener(MapsActivity.this);
            }
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @Nonnull String[] permissions, @Nonnull int[] grantResults)     {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
            } else {

                Toast.makeText(getApplicationContext(), "Please grant permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void checkLocationPermission() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Permission")
                        .setMessage("Location Allow Use!")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                            }
                        })
                        .create()
                        .show();
            }
            else {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            }
        }
    }


    public void mlocationRequest(){
        mLocationRequest = new LocationRequest();
        //ınterval for active location updates, in milliseconds
        mLocationRequest.setInterval(5000);
        //This controls the fastest rate at which your application will receive location updates,
        mLocationRequest.setFastestInterval(5000);
        //the most acccurate locations available
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {

        //Users in the circle.

        myCurrent= mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude))
                                    .title("Users"));
        //list the all users location in firebase database
        Log.d("DbLocation", key +" :"  + location.latitude + " " +location.longitude);

    }

    @Override
    public void onKeyExited(String key) {

        //users outside the circle.

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }

    public void setCircle(Location location){

        mLastLocation = location;

        final double latitude = mLastLocation.getLatitude();
        final double longitude = mLastLocation.getLongitude();

        // Lists the last location in firebase.
        Log.d("mlastLocation", "location: " + location.getLatitude() + " " + location.getLongitude());

        //current location
        LatLng currentLocation = new LatLng(latitude,longitude);

        //old circle is deleted as the location refreshed.
        if(mapCircle!=null)
        {
            mapCircle.remove();
        }
        //Create a new circle.
        mapCircle=mMap.addCircle(new CircleOptions()
                .center(currentLocation)
                .radius(radius)  //500 metres
                .strokeColor(Color.BLUE)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));

        //0.5f = 0.5 km = 500m
        //In this code, it sends the location that will search within the radius range.
        geoQuery = geoFire.queryAtLocation(new GeoLocation(currentLocation.latitude, currentLocation.longitude), 0.5);


    }

}
