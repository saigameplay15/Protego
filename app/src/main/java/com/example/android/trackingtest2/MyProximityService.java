package com.example.android.trackingtest2;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;


import static com.example.android.trackingtest2.MainActivity.trackingUrl;

public class MyProximityService extends IntentService {



    public MyProximityService() {
        super("MyProximityService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent!=null){
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
                //Toast.makeText(this,"GeoFence Successful",Toast.LENGTH_LONG).show();
                SmsManager smsManager = SmsManager.getDefault();
                MainActivity.textView.setText(R.string.msg_danger);
                try{
                    String str = trackingUrl.toString();
                    smsManager.sendTextMessage("+917433053793",null, str,null,null);
                } catch (Exception e){
                    Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
