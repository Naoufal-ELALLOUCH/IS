package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;

public class MarkerInfoObject {
	private Marker marker;
	private int id;
	private int color;
	private BitmapDescriptor icon;
	private int polylineColor; //ex)0xff000000
	private ArrayList<LatLng> points = new ArrayList<LatLng>();
    private ArrayList<Integer> polylineColors = new ArrayList<>();
	private Polyline polyline;
	
	static final int RED = 0;
	static final int ORANGE = 1;
	static final int YELLOW = 2;
	static final int GREEN = 3;
	static final int CYAN = 4;
	static final int AZURE = 5;
	static final int BLUE = 6;
	static final int VIOLET = 7;
	static final int MAGENTA = 8;
	static final int ROSE = 9;
	
	MarkerInfoObject(int id, Marker marker, int color) {
		this.id = id;
		this.marker = marker;
		this.color = color;
		this.icon = BitmapDescriptorFactory.defaultMarker(getMarkerColor(color));
		this.polylineColor = getARGB(color);
		this.points.add(marker.getPosition());
		this.polyline = null;
	}

	public int getId() {
		return this.id;
	}
	public void setMarker(Marker marker) {
		this.marker = marker;
		return;
	}

	public Marker getMarker() {
		return this.marker;
	}

	public void setColor(int color) {
		this.icon = BitmapDescriptorFactory.defaultMarker(getMarkerColor(color));
		this.polylineColor = getARGB(color);
		return;
	}

	public int getColor() {
		return this.color;
	}

	public BitmapDescriptor getIcon() {
		return this.icon;
	}

	public int getPolylineColor() {
		return this.polylineColor;
	}
	
	public void removeLastPoint() {
		this.points.remove(this.points.size() - 1);
	}
	
	public void addPoint(LatLng point) {
		this.points.add(point);
	}

	public LatLng getPoint(int i) {
		return this.points.get(i);
	}
	
	public ArrayList<LatLng> getPoints() {
		return this.points;
	}
	
	public LatLng getLastPoint() {
		return this.points.get(this.points.size()-1);
	}

	public void changePoints(LatLng point) {
		this.points.remove(this.points.size()-1);
		this.points.add(point);
	}

    public ArrayList<Integer> getPolylineColors() {
        return this.polylineColors;
    }
    public int getPolylineColor(int i) {
        return this.polylineColors.get(i);
    }
    public void addPolylineColor(int color) {
        this.polylineColors.add(color);
    }

    public void setPolyline(Polyline polyline) {
		this.polyline = polyline;
	}
	
	public Polyline getPolyline() {
		return this.polyline;
	}


	public static float getMarkerColor(int color) {
		float mColor = 0;
		switch (color){
		case RED:
			mColor = BitmapDescriptorFactory.HUE_RED;
			break;
		case ORANGE:
			mColor = BitmapDescriptorFactory.HUE_ORANGE;
			break;
		case YELLOW:
			mColor = BitmapDescriptorFactory.HUE_YELLOW;
			break;
		case GREEN:
			mColor = BitmapDescriptorFactory.HUE_GREEN;
			break;
		case CYAN:
			mColor = BitmapDescriptorFactory.HUE_CYAN;
			break;
		case AZURE:
			mColor = BitmapDescriptorFactory.HUE_AZURE;
			break;
		case BLUE:
			mColor = BitmapDescriptorFactory.HUE_BLUE;
			break;
		case VIOLET:
			mColor = BitmapDescriptorFactory.HUE_VIOLET;
			break;
		case MAGENTA:
			mColor = BitmapDescriptorFactory.HUE_MAGENTA;
			break;
		case ROSE:
			mColor = BitmapDescriptorFactory.HUE_ROSE;
			break;
		default:
			break;
		}
		return mColor;
	}

	public static int getARGB(int color) {
		int argb = 0;
		switch (color){
		case RED:
			argb = Color.RED;
			break;
		case ORANGE:
			argb = 0xffffa500; //ORANGE
			break;
		case YELLOW:
			argb = Color.YELLOW;
			break;
		case GREEN:
			argb = Color.GREEN;
			break;
		case CYAN:
			argb = Color.CYAN;
			break;
		case AZURE:
			argb = 0xfff0ffff; //AZURE;
			break;
		case BLUE:
			argb = Color.BLUE;
			break;
		case VIOLET:
			argb = 0xffee82ee; //VIOLET;
			break;
		case MAGENTA:
			argb = Color.MAGENTA;
			break;
		case ROSE:
			argb = 0xffffe4e1; //ROSE;
			break;
		default:
			break;
		}
		return argb;
	}
}
