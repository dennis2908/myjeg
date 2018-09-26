package com.talagasoft.gojekdriver.model;

/**
 * Created by compaq on 03/13/2016.
 */
public class PenumpangRecord {
    private long id;
    private String name;
    private double lat;
    private double lng;
    private String phone;
    private int rate;
    private double to_lat, to_lng;

    public PenumpangRecord(){}
    public PenumpangRecord(long id, String name, double lat, double lng, String phone,
                           int rate,double to_lat, double to_lng){
        super();
        this.id=id;
        this.name=name;
        this.lat=lat;
        this.lng=lng;
        this.phone=phone;
        this.rate=rate;
        this.to_lat=to_lat; this.to_lng=to_lng;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public double getTo_lat() {
        return to_lat;
    }

    public void setTo_lat(double to_lat) {
        this.to_lat = to_lat;
    }

    public double getTo_lng() {
        return to_lng;
    }

    public void setTo_lng(double to_lng) {
        this.to_lng = to_lng;
    }
}
