package com.example.riderapplication.ui.home;

import android.Manifest;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.riderapplication.Callback.IFirebaseDriverInfoListner;
import com.example.riderapplication.Callback.IFirebaseFailedListener;
import com.example.riderapplication.Common.Common;
import com.example.riderapplication.Model.AnimationModel;
import com.example.riderapplication.Model.DriverGeoModel;
import com.example.riderapplication.Model.DriverInfoModel;
import com.example.riderapplication.Model.GeoQueryModel;
import com.example.riderapplication.Remote.IGoogleApi;
import com.example.riderapplication.R;

import com.example.riderapplication.Remote.RetrofitClient;
import com.example.riderapplication.databinding.FragmentHomeBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListener,  IFirebaseDriverInfoListner {
    private SupportMapFragment mapFragment;
    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private String cityName;
    private  CompositeDisposable compositeDisposable;
    private  AutocompleteSupportFragment autocompleteSupportFragment;
    private  boolean firstTime = true  ;
    private boolean isFirstTime = true;
    private Location previousLocation,currentLocation;
    private static  final double  lIMIT_RANGE =  100.0;
    private  static  double  distance = 1.0;

    @BindView(R.id.activity_main)
    SlidingUpPanelLayout slidingUpPanelLayout;
    @BindView(R.id.txt_welcome)
    TextView textView_welcome;
    private IGoogleApi iGoogleApi;


    //Listner
    IFirebaseDriverInfoListner iFirebaseDriverInfoListner;
    IFirebaseFailedListener iFirebaseFailedListener;

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();

    }

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider((ViewModelStoreOwner) this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        initiate();
        intiate2(root);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return root;
    }

    private void intiate2(View root) {
        ButterKnife.bind(this,root);
        Common.setWelomeMessage(textView_welcome);
    }

    private void initiate() {
        Places.initialize(getContext(),getString(R.string.google_maps_key));
        autocompleteSupportFragment =(AutocompleteSupportFragment)getChildFragmentManager().findFragmentById(R.id.autocomplete_id);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.ADDRESS,Place.Field.NAME,Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint(getString(R.string.where_to));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                    Snackbar.make(getView()," "+status.getStatusMessage(),Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Snackbar.make(getView(), " "+place.getLatLng(),Snackbar.LENGTH_LONG).show();

            }
        });
        iGoogleApi = RetrofitClient.getInstance().create(IGoogleApi.class);

        iFirebaseFailedListener = this;
        iFirebaseDriverInfoListner = this;

        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(50f);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                LatLng newPostion = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPostion, 18f));

                //if user  has change location, calculate  and load driver again
                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;
                setRestrictionPlacesCountry(locationResult.getLastLocation());
                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }
                if (previousLocation.distanceTo(currentLocation) / 1000 <= lIMIT_RANGE) {
                    loadDriverAvailable();
                } else {
                    // Do Nothing
                }
            }
        };
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          // Snackbar.make(getView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        loadDriverAvailable();
    }

    private void setRestrictionPlacesCountry(Location lastLocation) {
        try{
            Geocoder geocoder = new Geocoder(getContext(),Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(),1);
            if(addressList.size() > 0){
                autocompleteSupportFragment.setCountry(addressList.get(0).getCountryCode());

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void loadDriverAvailable() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        //Load all drivers in the city
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addressList;

                        try {

                            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if(addressList.size() > 0){
                                cityName = addressList.get(0).getLocality();
                            }
                            if(!TextUtils.isEmpty(cityName)){

                            //Query
                            DatabaseReference database_Reference = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
                                    .child(cityName);
                            GeoFire geoFire = new GeoFire(database_Reference);
                            GeoQuery query = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(),location.getLongitude()),distance);
                            query.removeAllListeners();
                            query.addGeoQueryEventListener(new GeoQueryEventListener() {
                                @Override
                                public void onKeyEntered(String key, GeoLocation location) {
                                    Common.DRIVER_FOUND.add(new DriverGeoModel(key,location));
                                }

                                @Override
                                public void onKeyExited(String key) {

                                }

                                @Override
                                public void onKeyMoved(String key, GeoLocation location) {

                                }

                                @Override
                                public void onGeoQueryReady() {
                                    if(distance <= lIMIT_RANGE){
                                        distance++;
                                        loadDriverAvailable();
                                    }
                                    else  {
                                        distance =1.0;
                                        addMarkerToDriver();
                                        }

                                }

                                @Override
                                public void onGeoQueryError(DatabaseError error) {
                                    Snackbar.make(getView(),error.getMessage(),Snackbar.LENGTH_SHORT).show();

                                }
                            });
                            //Listen to a new driver in city and range
                            database_Reference.addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                    //have a new driver
                                    GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                    GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0
                                    ), geoQueryModel.getL().get(1));
                                    DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(),geoLocation);
                                    Location newDriverLocation = new Location("");
                                    newDriverLocation.setLatitude(geoLocation.latitude);
                                    newDriverLocation.setLongitude(geoLocation.longitude);
                                    float newDistance = location.distanceTo(newDriverLocation)/1000;
                                    if(newDistance <= lIMIT_RANGE){
                                        findDriverByKey(driverGeoModel); // if driver in range, add to map
                                    }

                                }

                                @Override
                                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                }

                                @Override
                                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                                }

                                @Override
                                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                            else {
                                Snackbar.make(getView(),getString(R.string.City_Empty),Snackbar.LENGTH_SHORT).show();
                            }

                        }
                            catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void addMarkerToDriver() {
        if(Common.DRIVER_FOUND.size() > 0){
            Observable.fromIterable(Common.DRIVER_FOUND)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {

                        //on next
                        findDriverByKey(driverGeoModel);
                    },throwable -> {
                        Snackbar.make(getView(),throwable.getMessage(),Snackbar.LENGTH_SHORT).show();
                    },()->{});
        }
        else{
            Snackbar.make(getView(),getString(R.string.DRIVER_NOT_FOUND),Snackbar.LENGTH_SHORT).show();
        }
    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {
        FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCE)
        .child(driverGeoModel.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChildren()) {
                    driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel.class));
                    iFirebaseDriverInfoListner.nDriverInfoSuccess(driverGeoModel);


                }
                else{
                    iFirebaseFailedListener.onFailedloadFailed(getString(R.string.KEY_NOT_FOUND)+driverGeoModel.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            iFirebaseFailedListener.onFailedloadFailed(error.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //Request for Permission for current location
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);

                        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                            @Override
                            public boolean onMyLocationButtonClick() {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                        ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                                    return false;
                                }
                                fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {

                                        LatLng userlatng = new LatLng(location.getLatitude(),location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userlatng,18f));

                                    }
                                });
                                return true;
                            }
                        });

                        // layout of button
                        View locationButton = ((View)mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        //right button
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
                        params.setMargins(0,0,0,250);


                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Snackbar.make(getView(),permissionDeniedResponse.getPermissionName()+" need enable",Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(),R.raw.uber_maps_style));
        }
        catch (Exception e){
            Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onFailedloadFailed(String message) {
        Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();

    }

    @Override
    public void nDriverInfoSuccess(DriverGeoModel driverGeoModel) {
        if(!Common.markerlist.containsKey(driverGeoModel.getKey()))
            Common.markerlist.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(driverGeoModel.getGeolocation().latitude
                            ,driverGeoModel.getGeolocation().longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstname(),
                            driverGeoModel.getDriverInfoModel().getLastname()))
                    .snippet(driverGeoModel.getDriverInfoModel().getPhonenumber())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));
    if(!TextUtils.isEmpty(cityName)){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                .getReference(Common.DRIVERS_LOCATION_REFERENCE)
                .child(cityName)
                .child(driverGeoModel.getKey());
        driverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.hasChildren()){
                    if(Common.markerlist.get(driverGeoModel.getKey()) != null)
                        Common.markerlist.get(driverGeoModel.getKey()).remove(); // Remove marker
                    Common.markerlist.remove(driverGeoModel.getKey()); //Remove marker info from hash Map
                    driverLocation.removeEventListener(this); //Remove event Listner
                }
                else{
                    if(Common.markerlist.get(driverGeoModel.getKey()) != null){
                        GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                        AnimationModel animationModel = new AnimationModel(false,geoQueryModel);
                        if(Common.driverLocationSuscriber.get(driverGeoModel.getKey()) != null){
                            Marker currentMarker = Common.markerlist.get(driverGeoModel.getKey() != null);
                            AnimationModel oldPosition  = Common.driverLocationSuscriber.get(driverGeoModel.getKey());

                            String from = new StringBuilder()
                                    .append(oldPosition.getGeoQueryModel().getL().get(0))
                                    .append(",")
                                    .append(oldPosition.getGeoQueryModel().getL().get(1))
                                    .toString();

                            String to = new StringBuilder()
                                    .append(animationModel.getGeoQueryModel().getL().get(0))
                                    .append(",")
                                    .append(animationModel.getGeoQueryModel().getL().get(1))
                                    .toString();

                            moveMarkerAnimation(driverGeoModel.getKey(),animationModel,currentMarker,from,to);

                        }else{
                            Common.driverLocationSuscriber.put(driverGeoModel.getKey(),animationModel);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        if(!animationModel.isRun()){
            //Request API
            compositeDisposable.add(iGoogleApi.getDirections("driving","less_driving",from,to,
       //check here its needs to be google_API_KEY
                    getString(R.string.google_maps_key))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(returnResult ->{
                Log.d("API RETURN",returnResult);

                try{
                    JSONObject jsonObject = new JSONObject(returnResult);
                    JSONArray jsonArray = new JSONArray("routes");
                    for(int i = 0; i < jsonArray.length(); i++)
                    {

                        JSONObject route = jsonArray.getJSONObject(i);
                        JSONObject   poly = route.getJSONObject("overview_polyline");
                        String polyline = poly.getString("points");
                        animationModel.setPolyLinelist(Common.decodePoly(polyline));
                    }
                    //Moving car
                    //handler = new Handler();
                 animationModel.setIndex(-1);
                    //index = -1;
                   // next = 1;
                    animationModel.setNext(1);

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if(animationModel.getPolyLinelist() != null && animationModel.getPolyLinelist().size() > 1){
                                if(animationModel.getIndex() <animationModel.getPolyLinelist().size() -2){
                                    animationModel.setIndex(animationModel.getIndex()+1);
                                    animationModel.setNext( animationModel.getIndex() +1);
                                  //  start = polyLinelist.get(index);
                                    animationModel.setStart(animationModel.getPolyLinelist().get(animationModel.getIndex()));
                                  //  end = polyLinelist.get(next);
                                    animationModel.setEnd(animationModel.getPolyLinelist().get(animationModel.getNext()));
                                }
                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                                valueAnimator.setDuration(3000);
                               valueAnimator.setInterpolator(new LinearInterpolator());
                               valueAnimator.addUpdateListener(value -> {
                                 //  v = value.getAnimatedFraction();
                                   animationModel.setV(value.getAnimatedFraction());
                                  // lat = v*end.latitude + (1-v) *start.latitude;
                                   animationModel.setLat(animationModel.getV() * animationModel.getEnd().latitude
                                   + (1 - animationModel.getV() *animationModel.getStart().latitude));
                                 //  lng = v*end.longitude + (1-v) *start.longitude;
                                   animationModel.setLng(animationModel.getV() * animationModel.getEnd().longitude
                                   +(1 - animationModel.getV()) * animationModel.getStart().longitude );
                                   LatLng newPos = new LatLng(animationModel.getLat(),animationModel.getLng());
                                   currentMarker.setPosition(newPos);
                                   currentMarker.setAnchor(0.5f,0.5f);
                                   currentMarker.setRotation(Common.getBearing(animationModel.getStart(),newPos));
                               });
                               valueAnimator.start();
                               if(animationModel.getIndex() < animationModel.getPolyLinelist().size()  - 2){
                                   animationModel.getHandler().postDelayed(this,1500);

                               }
                               else  if (animationModel.getIndex() < animationModel.getPolyLinelist().size() -1){
                                   animationModel.setRun(false);
                                   Common.driverLocationSuscriber.put(key,animationModel);
                               }
                            }
                        }
                    };

                    // run handler
                    animationModel.getHandler().postDelayed(runnable,1500);


                }
                catch (Exception e){
                    Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_LONG).show();
                }

            }));
        }
    }
}