package com.example.riderapplication.Common;

import android.widget.TextView;

import com.example.riderapplication.Model.AnimationModel;
import com.example.riderapplication.Model.DriverGeoModel;
import com.example.riderapplication.Model.RiderModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Common {
    public static final String RIDER_INFO_REFERNCE="Riders";
    public static final String DRIVERS_LOCATION_REFERENCE ="DriversLocation" ;
    public static final Set<DriverGeoModel> DRIVER_FOUND = new HashSet<DriverGeoModel>();
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static RiderModel currentRider;
    public static HashMap<String, Marker> markerlist = new HashMap<>();
    public static HashMap<String, AnimationModel> driverLocationSuscriber =new HashMap<String,AnimationModel>();

    public static String buildName(String firstname, String lastname) {
        return new StringBuilder(firstname).append(" ").append(lastname).toString();

    }

    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index = 0,len=encoded.length();
        int lat = 0, lng = 0;
                while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    public static float getBearing(LatLng start, LatLng end) {
        double lat = Math.abs(start.latitude - end.longitude);
        double lng = Math.abs(start.longitude -end.longitude);

        if(start.latitude < end.latitude && start.longitude < end.longitude){
            return (float)  (Math.toDegrees(Math.atan(lng / lat)));
        }
        else  if(start.latitude >= end.latitude && start.longitude < end.longitude){
            return (float)  ((90 - Math.toDegrees(Math.atan(lng / lat)))+90);
        }
        else if(start.latitude >= end.latitude && start.longitude >= end.longitude){
            return (float) ((90-Math.toDegrees(Math.atan(lng / lat)))+180);
        }
        else if(start.latitude < end.latitude && start.longitude >= end.longitude) {
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        }
        return -1;

    }

    public static void setWelomeMessage(TextView textView_welcome) {
        int hourOftheDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if(hourOftheDay >= 1 && hourOftheDay < 12){
            textView_welcome.setText(new StringBuilder("Good Morning"));
        }
        else if (hourOftheDay >= 13 && hourOftheDay < 17){
            textView_welcome.setText(new StringBuilder("Good afternoon"));
        }
        else{
            textView_welcome.setText(new StringBuilder("Good evening"));
        }
    }
}

