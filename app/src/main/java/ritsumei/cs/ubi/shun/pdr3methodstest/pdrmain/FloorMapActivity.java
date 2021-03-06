package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ritsumei.cs.ubi.shun.pdr3methodstest.R;
import ubilabmapmatchinglibrary.mapmatching.Trajectory;


public class FloorMapActivity extends FragmentActivity implements SensorEventListener {
	public GoogleMap map;
	public List<MarkerInfoObject> markerList;

    public TreeMap<String, LatLng> trajectoryMap = new TreeMap<>();
    public TreeMap<String, Integer> polylineColorMap = new TreeMap<>();

	private SensorManager mSensorManager;
	public TextView tvHeading;
	public float orDegrees;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_pdr_map);

		markerList = new ArrayList<MarkerInfoObject>();

		tvHeading = (TextView) findViewById(R.id.tvHeading);


		// initialize your android device sensor capabilities
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


		setMap();
	}


	@Override
	protected void onResume() {
		super.onResume();

		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);


	}

	@Override
	protected void onPause() {
		//super.onPause();

		// to stop the listener and save battery
		//mSensorManager.unregisterListener(this);

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// get the angle around the z-axis rotated
			float degree = Math.round(event.values[0]);
			tvHeading.setText("Heading: " + Float.toString(degree) + " degrees FLRMAPACTIVITY");

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}




	protected void setMap(){
		map = ((SupportMapFragment)
		      getSupportFragmentManager().findFragmentById(R.id.map))
		      .getMap();

        MapsInitializer.initialize(this);

        moveToCreationCore();
        floorMapOverlay();
	}

	protected void moveToCreationCore() {

		CameraUpdate cull =
//				CameraUpdateFactory.newLatLngZoom(new LatLng(34.979561, 135.964429), 19); //cc
//                CameraUpdateFactory.newLatLngZoom(new LatLng(34.979571, 135.964540), 20.5f); //cc
                CameraUpdateFactory.newLatLngZoom(new LatLng(34.979571, 135.964540), 20); //cc
//                CameraUpdateFactory.newLatLngZoom(new LatLng(34.702283947, 135.49986183), 19); //umechika

		map.moveCamera(cull);

	}

	protected void floorMapOverlay() {
		BitmapDescriptor floormap = BitmapDescriptorFactory.fromResource(R.drawable.floormap_cc5f);

//        BitmapDescriptor floormap3 = BitmapDescriptorFactory.fromResource(R.drawable.nizyuubashi_1);
//        BitmapDescriptor floormap4 = BitmapDescriptorFactory.fromResource(R.drawable.nizyuubashi_5);

		GroundOverlayOptions options = new GroundOverlayOptions();
		options.image(floormap);
		options.anchor(0, 1);
		options.bearing(3f);
		options.position(new LatLng(34.979389,135.963716), 101.385f, 43.795f);
   		// マップに貼り付け・アルファを設定
		GroundOverlay overlay = map.addGroundOverlay(options);
		overlay.setTransparency(0.1f);

//        options.position(new LatLng(34.978389,135.9634616), 101.385f, 101.385f);
//
//        GroundOverlayOptions options3 = new GroundOverlayOptions();
//        options3.image(floormap3);
//        options3.anchor(0, 1);
//        options3.bearing(3f);
//        //options.position(new LatLng(34.979389,135.963716), 101.385f, 43.795f);
//
//        options3.position(new LatLng(34.979389,135.964721), 101.385f, 101.385f);
//
//        GroundOverlayOptions options4 = new GroundOverlayOptions();
//        options4.image(floormap4);
//        options4.anchor(0, 1);
//        options4.bearing(3f);
//        options4.position(new LatLng(34.978394,135.963716), 101.385f, 101.385f);
//
//
//        // マップに貼り付け・アルファを設定
//        GroundOverlay overlay3 = map.addGroundOverlay(options3);
//        overlay3.setTransparency(0.1f);
//
//        // マップに貼り付け・アルファを設定
//        GroundOverlay overlay4 = map.addGroundOverlay(options4);
//        overlay4.setTransparency(0.1f);
//
//        GroundOverlayOptions options7 = new GroundOverlayOptions();
//        options7.image(floormap4);
//        options7.anchor(0, 1);
//        options7.bearing(3f);
//        options7.position(new LatLng(34.978494,135.963716), 101.385f, 101.385f);
//        GroundOverlay overlay7 = map.addGroundOverlay(options7);
//        overlay7.setTransparency(0.1f);
//
//        GroundOverlayOptions options5 = new GroundOverlayOptions();
//        options5.image(floormap4);
//        options5.anchor(0, 1);
//        options5.bearing(3f);
//        options5.position(new LatLng(34.978494,135.963516), 101.385f, 101.385f);
//        GroundOverlay overlay5 = map.addGroundOverlay(options5);
//        overlay5.setTransparency(0.1f);
//
//        GroundOverlayOptions options6 = new GroundOverlayOptions();
//        options6.image(floormap4);
//        options6.anchor(0, 1);
//        options6.bearing(3f);
//        options6.position(new LatLng(34.978394,135.963516), 101.385f, 101.385f);
//        GroundOverlay overlay6 = map.addGroundOverlay(options6);
//        overlay6.setTransparency(0.1f);
	}

	public void createMarker(int id, LatLng point, int markerColor) {
		MarkerOptions options = new MarkerOptions();
		options.position(point);
		BitmapDescriptor icon1,icon2,icon3,icon4;

		icon1 = BitmapDescriptorFactory.defaultMarker(MarkerInfoObject.getMarkerColor(markerColor));
		icon2 = BitmapDescriptorFactory.fromResource(R.drawable.start);
		icon3 = BitmapDescriptorFactory.fromResource(R.drawable.arrival);
		icon4 = BitmapDescriptorFactory.fromResource(R.drawable.navigation);

		if(markerColor == MarkerInfoObject.BLUE)
		options.icon(icon2);
		else if(markerColor == MarkerInfoObject.GREEN)
			options.icon(icon3);
		else
		options.icon(icon4);


		int index = searchIndex(id);

		if(index == -1) {
			markerList.add(new MarkerInfoObject(id, map.addMarker(options), markerColor));
		} else {
			markerList.get(index).setMarker(map.addMarker(options));
			markerList.get(index).addPoint(point);
		}

//		//Log.v("map","CreateMaeker:"+ markerId + ","+ point + "," + markerColor + "," + markerList.get(0).getPolylineColor());
	}

	public void moveMarkerDefaultPolylineColor(int id, LatLng point) {
		moveMarkerWithPolyline(id, point, markerList.get(searchIndex(id)).getPolylineColor());
	}

	public void moveMarkerWithPolyline(int id, LatLng point, int color) {
		tvHeading.setText("Heading: " + Float.toString(orDegrees) + " degrees FFFFF");


		BitmapDescriptor icon4;
		icon4 = BitmapDescriptorFactory.fromResource(R.drawable.navigation);

		int index = searchIndex(id);

		LatLng lastPoint = markerList.get(index).getLastPoint();
		Location L1 = new Location("starting point");
		L1.setLatitude(lastPoint.latitude);
		L1.setLatitude(lastPoint.longitude);

		Location L2 = new Location("ending point");
		L2.setLatitude(point.latitude);
		L2.setLatitude(point.longitude);

		//float bearing = L1.bearingTo(L2) ;
		float bearing = orDegrees;
		CameraPosition cameraPosition = CameraPosition.builder()
				.target(point)
				.zoom(21)
				.bearing(bearing)
				.build();

		markerList.get(index).addPoint(point);

		removePolyline(id);
		drawPolylineAllPoints(id, color);

		markerList.get(index).getMarker().remove();

		markerList.get(index).setMarker(map.addMarker(new MarkerOptions().position(point).icon(icon4)
						.flat(true)
						.rotation(bearing)
		));

		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
				2000, null);
	}

    public void moveMarkerWithPolylineColorColorful(int id, LatLng point, int color) {
		BitmapDescriptor icon4;
		icon4 = BitmapDescriptorFactory.fromResource(R.drawable.navigation);

		int index = searchIndex(id);

		LatLng lastPoint = markerList.get(index).getLastPoint();
		Location L1 = new Location("starting point");
		L1.setLatitude(lastPoint.latitude);
		L1.setLatitude(lastPoint.longitude);

		Location L2 = new Location("ending point");
		L2.setLatitude(point.latitude);
		L2.setLatitude(point.longitude);

		//float bearing = L1.bearingTo(L2) ;
		float bearing = orDegrees;

		CameraPosition cameraPosition = CameraPosition.builder()
				.target(point)
				.zoom(21)
				.bearing(bearing)
				.build();

        markerList.get(index).addPoint(point);
        markerList.get(index).addPolylineColor(color);

        removePolyline(id);
        drawPolylineAllPointsColorful(id);

        markerList.get(index).getMarker().remove();
        markerList.get(index).setMarker(map.addMarker(new MarkerOptions().position(point).icon(icon4)
						.flat(true)
						.rotation(bearing)
		));

		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
				2000, null);
    }

    public void moveMarker(int id, LatLng point) {
		BitmapDescriptor icon4;
		icon4 = BitmapDescriptorFactory.fromResource(R.drawable.navigation);

		int index = searchIndex(id);

		LatLng lastPoint = markerList.get(index).getLastPoint();
		Location L1 = new Location("starting point");
		L1.setLatitude(lastPoint.latitude);
		L1.setLatitude(lastPoint.longitude);

		Location L2 = new Location("ending point");
		L2.setLatitude(point.latitude);
		L2.setLatitude(point.longitude);

		//float bearing = L1.bearingTo(L2) ;
		float bearing = orDegrees;

		CameraPosition cameraPosition = CameraPosition.builder()
				.target(point)
				.zoom(21)
				.bearing(bearing)
				.build();

		markerList.get(index).getMarker().remove();

        markerList.get(index).setMarker(map.addMarker(new MarkerOptions().position(point).icon(icon4)
						.flat(true)
						.rotation(bearing)

		));

		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
				2000, null);
    }

    public void addPolylinePoint(int id, LatLng point) {
        int index = searchIndex(id);

        markerList.get(index).addPoint(point);
    }

	/*
	 * 指定したidのMarkerInfoObjectに格納されている全てのpointを結ぶPolylineを書く
	 */
	public void drawPolylineAllPoints(int id, int color) {
//		//Log.v("map","polyline:"+ id + ","+ lastPoint +"," + point + "," + color);
		int i = searchIndex(id);

		PolylineOptions po = new PolylineOptions()
	    .addAll(markerList.get(i).getPoints())
		.color(color)
		.width(9.0f);

		markerList.get(i).setPolyline(map.addPolyline(po));
	}

    public void drawPolylineAllPoints2(int id, int color) {
//		//Log.v("map","polyline:"+ id + ","+ lastPoint +"," + point + "," + color);
        int i = searchIndex(id);

        markerList.get(i).getPoints().clear();
        for(Map.Entry<String, LatLng> e : trajectoryMap.entrySet()) {
            markerList.get(i).addPoint(e.getValue());
        }
        PolylineOptions po = new PolylineOptions()
                .addAll(markerList.get(i).getPoints())
                .color(color)
                .width(9.0f);

        markerList.get(i).setPolyline(map.addPolyline(po));
    }

    public void drawPolylineAllPointsColorful(int id) {
        int i = searchIndex(id);
        markerList.get(i).getPoints().clear();
        markerList.get(i).getPolylineColors().clear();
        for(Map.Entry<String, LatLng> e : trajectoryMap.entrySet()) {
            markerList.get(i).addPoint(e.getValue());
        }
        for(Map.Entry<String, Integer> e : polylineColorMap.entrySet()) {
            markerList.get(i).addPolylineColor(e.getValue());
        }

        PolylineOptions po = new PolylineOptions()
                .addAll(markerList.get(i).getPoints())
                .color(Color.RED)
                .width(9.0f);
        markerList.get(i).setPolyline(map.addPolyline(po));


        for(int j=1; j<markerList.get(i).getPolylineColors().size(); j++) {
            if (markerList.get(i).getPolylineColor(j) != Color.RED) {
                LatLng latLng1 = markerList.get(i).getPoint(j-1);
                LatLng latLng2 = markerList.get(i).getPoint(j);

                PolylineOptions po2 = new PolylineOptions()
                        .add(latLng1, latLng2)
                        .color(markerList.get(i).getPolylineColor(j))
                        .width(4.0f);
                map.addPolyline(po2);
            }
        }
    }

	/*
	 * 2点間のPolylineを書く
	 * Polyline情報はidのMarkerInfoObjectに格納する
	 */
	public void drawPolyline2Points(int id, LatLng point1, LatLng point2, int color) {
		int index = searchIndex(id);

		PolylineOptions po = new PolylineOptions()
	    .add(point1, point2)
		.color(color)
		.width(9.0f);

		markerList.get(index).setPolyline(map.addPolyline(po));
	}

	/*
	 * 指定したLatLngにマップを移動する
	 */
	public void moveMap(LatLng point) {
		CameraUpdate cull =
				CameraUpdateFactory.newLatLng(point);
		map.moveCamera(cull);

	}
	/*
	 * 指定したidのMarkerを消す
	 */
	public void removeMarker(int id) {
		int index = searchIndex(id);
		markerList.get(index).getMarker().remove();
		removePolyline(id);
	}

	/*
	 * 指定したidのPolylineを消す
	 */
	public void removePolyline(int id) {
		int index = searchIndex(id);
//        if(index  > -1) {
            if (markerList.get(index).getPolyline() != null) {
                markerList.get(index).getPolyline().remove();
            }
//        }
	}

	/**
	 * ２点間距離と角度
	 * @return result[0] 距離
	 * @return result[1] 角度　北が0度で時計回りに+-180度
	 * @return result[2] ３点以のときの角度
	 */
	public float[] calc2PointDist(LatLng point1, LatLng point2) {
		float[] result = new float[3];
		Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, result);
		return result;

	}

	/*
	 * 指定したidがmarkerListの何番目にあるのかを返す
	 * 指定したidが無ければ-1を返す
	 */
	public int searchIndex (int id) {
		int index = -1;
		for (int i = 0; i < markerList.size(); i++) {
			if(markerList.get(i).getId() == id) {
				 index = i;
				 break;
			}
		}
		return index;
	}
	
	/**
	 * 補正した軌跡に描画しなおす
	 */
	public void reDrawTrajectory(int id, Trajectory transTrajectory) {

		int index = searchIndex(id);
		
		for(int i = 0; i < transTrajectory.size(); i++) {
			markerList.get(index).removeLastPoint();
		}
		
		for(int i = 0; i < transTrajectory.size(); i++) {
			markerList.get(index).addPoint(transTrajectory.get(i).getLocation());
		}

		markerList.get(index).getMarker().remove();

		markerList.get(index).setMarker(map.addMarker(new MarkerOptions().position(transTrajectory.get(transTrajectory.size() - 1).getLocation()).icon(markerList.get(index).getIcon())));

	}

	public Marker getMarker(int id) {
		int index = searchIndex(id);
		return markerList.get(index).getMarker();
		
	}
}

