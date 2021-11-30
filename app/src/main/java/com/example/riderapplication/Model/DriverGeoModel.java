package com.example.riderapplication.Model;

import com.firebase.geofire.GeoLocation;

public class DriverGeoModel {
    private String key;


    private GeoLocation geolocation;
    private DriverInfoModel driverInfoModel;

    public DriverGeoModel() {
    }

    public DriverGeoModel(String key, GeoLocation geolocation) {
        this.key = key;
        this.geolocation = geolocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(GeoLocation geolocation) {
        this.geolocation = geolocation;
    }

    public DriverInfoModel getDriverInfoModel() {
        return driverInfoModel;
    }

    public void setDriverInfoModel(DriverInfoModel driverInfoModel) {
        this.driverInfoModel = driverInfoModel;
    }
}
