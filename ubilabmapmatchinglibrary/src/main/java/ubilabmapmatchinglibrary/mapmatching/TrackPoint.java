package ubilabmapmatchinglibrary.mapmatching;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;

public class TrackPoint {
	private long time;
	private LatLng location;
	private double direction;
	private double distance;
    private boolean isStraight;
	private String linkId;
    public Integer polylineColor = Color.RED;
    public Boolean isSkeletonMatch = false;
	
	public TrackPoint() { 
		this.time = 0;
		this.location = null;
		this.direction = 0;
		this.distance = 0;
        this.isStraight = true;
		this.linkId = "null";
	}
	
	public TrackPoint (long time, double lat, double lng, double direction, double distance, boolean isStraight, String linkId) {
		this.time = time;
		this.location = new LatLng(lat, lng);
		this.direction = direction;
		this.distance = distance;
        this.isStraight = isStraight;
		this.linkId = linkId;
	}

	public TrackPoint (long time, LatLng location, double direction, double distance, boolean isStraight, String linkId) {
		this.time = time;
		this.location = location;
		this.direction = direction;
		this.distance = distance;
        this.isStraight = isStraight;
		this.linkId = linkId;
	}
	
	public TrackPoint(TrackPoint track) {
		this.time = track.getTime();
		this.location = track.getLocation();
		this.direction = track.getDirection();
		this.distance = track.getDistance();
		this.isStraight = track.getIsStraight();
		this.linkId = track.getLinkId();
	}
	
	public void setTrackPoint(TrackPoint track) {
		this.time = track.getTime();
		this.location = track.getLocation();
		this.direction = track.getDirection();
		this.distance = track.getDistance();
		this.isStraight = track.getIsStraight();
		this.linkId = track.getLinkId();
	}

    public void setTrackPoint (long time, double lat, double lng, double direction, double distance, boolean isStraight, String linkId) {
        this.time = time;
        this.location = new LatLng(lat, lng);
        this.direction = direction;
        this.distance = distance;
        this.isStraight = isStraight;
        this.linkId = linkId;
    }

    public void setTrackPoint (long time, LatLng location, double direction, double distance, boolean isStraight, String linkId) {
        this.time = time;
        this.location = location;
        this.direction = direction;
        this.distance = distance;
        this.isStraight = isStraight;
        this.linkId = linkId;
    }

	public long getTime() {
		return time;
	}
	
	public LatLng getLocation() {
		return location;
	}

	public void setLocation(double lat, double lng) {
		this.location = new LatLng(lat, lng);
	}
	
	public void setLocation(LatLng point) {
		this.location = point;
	}

    public double getLat() {
        return location.latitude;
    }

    public double getLng() {
        return location.longitude;
    }

	public double getDirection() {
		return direction;
	}

	public void setDirection(double direction) {
		this.direction = direction;
	}

	public double getDistance() {
		return distance;
	}

    public boolean getIsStraight() {
		return isStraight;
	}
	
	public String getLinkId() {
		return linkId;
	}

    public void setLinkId(String linkId) { this.linkId = linkId; }

}
