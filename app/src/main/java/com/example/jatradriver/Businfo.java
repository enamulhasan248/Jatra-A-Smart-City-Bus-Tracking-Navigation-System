package com.example.jatradriver;




public class Businfo {


    private Double lat;
    private Double lon;

    private float speed;
    public long timestamp;

    // Required empty constructor for Firebase
    public Businfo() {
    }
    public Businfo(Double lat, Double lon,float speed) {
        this.lat = lat;
        this.lon = lon;
        this.speed = speed;
        this.timestamp = System.currentTimeMillis();
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }

    public  float getSpeed(){
        return speed;
    }

    public void setSpeed(float speed){
        this.speed = speed;
    }

}
