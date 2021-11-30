package com.example.riderapplication.Model;

import android.os.Handler;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class AnimationModel {
    private boolean isRun;
    private GeoQueryModel geoQueryModel;
    private Handler handler;
    private int index, next;
    private LatLng start,end;
    private double lng,lat;
    private float v;
    private List<LatLng> polyLinelist;

    public List<LatLng> getPolyLinelist() {
        return polyLinelist;
    }

    public void setPolyLinelist(List<LatLng> polyLinelist) {
        this.polyLinelist = polyLinelist;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getNext() {
        return next;
    }

    public void setNext(int next) {
        this.next = next;
    }

    public LatLng getStart() {
        return start;
    }

    public void setStart(LatLng start) {
        this.start = start;
    }

    public LatLng getEnd() {
        return end;
    }

    public void setEnd(LatLng end) {
        this.end = end;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public float getV() {
        return v;
    }

    public void setV(float v) {
        this.v = v;
    }

    public AnimationModel(boolean isRun, GeoQueryModel geoQueryModel) {
        this.isRun = isRun;
        this.geoQueryModel = geoQueryModel;
        this.handler = new Handler();
    }

    public boolean isRun() {
        return isRun;
    }

    public void setRun(boolean run) {
        isRun = run;
    }

    public GeoQueryModel getGeoQueryModel() {
        return geoQueryModel;
    }

    public void setGeoQueryModel(GeoQueryModel geoQueryModel) {
        this.geoQueryModel = geoQueryModel;
    }
}
