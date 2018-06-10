package com.example.android.trackingtest2;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonObject;
import com.hypertrack.lib.GeofenceTransitionService;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.HyperTrackMapAdapter;
import com.hypertrack.lib.HyperTrackService;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.internal.common.util.HTTextUtils;
import com.hypertrack.lib.internal.transmitter.models.HyperTrackEvent;
import com.hypertrack.lib.models.Action;
import com.hypertrack.lib.models.ActionParamsBuilder;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.GeoJSONLocation;
import com.hypertrack.lib.models.HyperTrackLocation;
import com.hypertrack.lib.models.Place;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.MapProvider.MapFragmentView;
import com.hypertrack.lib.tracking.UseCase.LocationUpdater;
import com.hypertrack.lib.tracking.UseCase.OrderTracking.OrderTrackingMvp;
import com.hypertrack.lib.tracking.model.MarkerModel;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.example.android.trackingtest2.LoginActivity.HT_QUICK_START_SHARED_PREFS_KEY;
import static com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;


public class MainActivity extends FragmentActivity {

    private final String TAG = "hi";
    private HyperTrackMapFragment hyperTrackMapFragment;
    private HyperTrackMapAdapter mapAdapter;
    private MapFragmentCallback mapFragmentCallback;
    private HyperTrackCallback hyperTrackCallback;
    private Action currentAction;
    private String collectionId;
    private LocationUpdater mLocationUpdate;
    private GeofenceTransitionService geofenceTransitionService;
    private Geofence geofence;
    private GeofencingClient geofencingClient;
    private GeofencingRequest geofencingRequest;
    public static String trackingUrl;
    public static TextView textView;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        completeDeliveryAction();
        completeVisitAction();
        HyperTrack.removeActions(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar(getString(R.string.app_name));
        Button sos = (Button)findViewById(R.id.SOS);
        sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,NavigatingMap.class);
                startActivity(intent);
            }
        });

        Button btn = (Button)findViewById(R.id.sos);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        btn.setBackgroundColor(getResources().getColor(R.color.red));

        textView = (TextView)findViewById(R.id.danger_msg);

        instantiateMap();
        createVisitAction();
        trackAction();
    }

    private void instantiateGeoFence(){
        geofence = new Geofence.Builder().setCircularRegion(12.994832, 77.660922, 50)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL)
                .setRequestId("hi")
                .setLoiteringDelay(10000)
                .setExpirationDuration(50000)
                .build();
        geofencingClient = LocationServices.getGeofencingClient(this);

        geofencingRequest = new GeofencingRequest.Builder().addGeofence(geofence).build();
        Intent intent = new Intent(MainActivity.this,MyProximityService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this,1,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent);
    }

    private void instantiateMap(){
        hyperTrackMapFragment = (HyperTrackMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);

        mapAdapter = new HyperTrackMapAdapter(this);
        hyperTrackMapFragment.setMapAdapter(mapAdapter);
        mapFragmentCallback = new MapFragmentCallback();
        hyperTrackMapFragment.setMapCallback(mapFragmentCallback);
    }

    private void trackAction(){
        HyperTrack.trackActionByCollectionId(collectionId, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                hyperTrackMapFragment.setUseCaseType(MapFragmentView.Type.ORDER_TRACKING);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

            }
        });
    }

    private void initToolbar(String string){
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(string);
    }

//    private void initUIViews() {
//        // Initialize AssignAction Button
//        Button logoutButton = (Button) findViewById(R.id.logout_btn);
//
//            logoutButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    createVisitAction();
//                    trackAction();
//                }
//            });
//
//        Button stop = (Button) findViewById(R.id.stop);
//            stop.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    completeVisitAction();
//                    //completeDeliveryAction();
//                    HyperTrack.removeActions(null);
//                }
//            });
//    }

    public void createDeliveryAction() {
        Place expectedPlace = new Place();
        expectedPlace.setLocation(new GeoJSONLocation(12.9, 77.6));

        ActionParamsBuilder actionParamsBuilder = new ActionParamsBuilder();
        actionParamsBuilder.setType(Action.TYPE_DELIVERY);
        actionParamsBuilder.setExpectedPlace(expectedPlace);

        hyperTrackCallback = new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Action action = (Action) response.getResponseObject();
                currentAction = action;
                trackingUrl = action.getTrackingURL();
                Toast.makeText(MainActivity.this,trackingUrl,Toast.LENGTH_LONG).show();
                saveDeliveryAction(action);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

            }
        };
        HyperTrack.createAction(actionParamsBuilder.build(), hyperTrackCallback);
    }

    public void completeDeliveryAction() {
        HyperTrack.completeAction(getDeliveryActionId());
        HyperTrack.completeAction(currentAction.getId());
        HyperTrack.removeActions(Collections.singletonList(getDeliveryActionId()));
    }

    public void createVisitAction() {
        ActionParamsBuilder actionParamsBuilder = new ActionParamsBuilder();
        collectionId = UUID.randomUUID().toString();
        actionParamsBuilder.setCollectionId(collectionId);

        HyperTrack.createAction(actionParamsBuilder.build(), new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Action action = (Action) response.getResponseObject();
                currentAction = action;
                trackingUrl = action.getTrackingURL();
                instantiateGeoFence();
                //Send SMS to emergency contacts

                //Toast.makeText(MainActivity.this,trackingUrl,Toast.LENGTH_LONG).show();
                //Toast.makeText(MainActivity.this,"Action created",Toast.LENGTH_LONG).show();
                saveVisitAction(action);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

            }
        });
    }

    public void completeVisitAction() {
        HyperTrack.completeAction(getVisitActionId());
    }

    private String getDeliveryActionId() {
        SharedPreferences sharedPreferences = getSharedPreferences(HT_QUICK_START_SHARED_PREFS_KEY,
                Context.MODE_PRIVATE);
        return sharedPreferences.getString("delivery_action_id", null);
    }

    private String getVisitActionId() {
        SharedPreferences sharedPreferences = getSharedPreferences(HT_QUICK_START_SHARED_PREFS_KEY,
                Context.MODE_PRIVATE);
        return sharedPreferences.getString("visit_action_id",null);
    }

    private void saveDeliveryAction(Action action) {
        SharedPreferences sharedPreferences = getSharedPreferences(HT_QUICK_START_SHARED_PREFS_KEY,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("delivery_action_id", action.getId());
        editor.apply();
    }

    private void saveVisitAction(Action action) {
        SharedPreferences sharedPreferences = getSharedPreferences(HT_QUICK_START_SHARED_PREFS_KEY,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("visit_action_id", action.getId());
        editor.apply();
    }

    public void clearUser() {
        SharedPreferences sharedPreferences = getSharedPreferences(HT_QUICK_START_SHARED_PREFS_KEY,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }


}
