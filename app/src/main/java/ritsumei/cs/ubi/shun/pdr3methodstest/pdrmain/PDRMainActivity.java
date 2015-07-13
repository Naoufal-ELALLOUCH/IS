package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jp.ac.ritsumei.cs.ubi.shun.library.pdr.DirectionCalculator;
import jp.ac.ritsumei.cs.ubi.shun.library.pdr.PDRPositionCalculator;
import jp.ac.ritsumei.cs.ubi.shun.library.step.PeakStepDetector;
import jp.ac.ritsumei.cs.ubi.shun.library.step.StepListener;
import ritsumei.cs.ubi.shun.pdr3methodstest.R;
import ubilabmapmatchinglibrary.mapmatching.CollisionDetectMatching;
import ubilabmapmatchinglibrary.mapmatching.Point;
import ubilabmapmatchinglibrary.mapmatching.SkeletonMatching;
import ubilabmapmatchinglibrary.mapmatching.TrackPoint;
import ubilabmapmatchinglibrary.mapmatching.Trajectory;
import ubilabmapmatchinglibrary.mapmatching.TrajectoryTransformedListener;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;


public class PDRMainActivity extends FloorMapActivity implements StepListener, TrajectoryTransformedListener, SensorEventListener, OnClickListener , GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    public static final String DB_QUERY_FILE = "cc5f_network_sql.txt";
//    public static final String DB_QUERY_FILE = "cc_psn.txt";

    private SensorManager manager;
    private SharedPreferences pref;
    private AlertDialog.Builder alertDialog;

    private AlertDialog mInitializePDRDialog;
    private EditText mStartLatitudeEditText;
    private EditText mStartLongitudeEditText;
    private EditText mDirectionLatitudeEditText;
    private EditText mDirectionLongitudeEditText;

    private Button startButton;
    //  private Button resetButton;
    private Button setupButton;
//    private Button resultButton;
    private Button timeButton;
    // private Button simulationModeButton;
    private Button selectStartPinButton;
    private Button selectDirectionPinButton;
    // private TextView directionTextView;

    private boolean isStart = false;

    private final int startMarkerId = 0;
    private final int directionMarkerId = 1;
    private final int pdrMarkerId = 2;
	private final int skeletonMatchedMarkerId = 3;
    private final int collisionDetectMatchingMarkerId = 4;

    private double startDirection;

    private enum Status {
        READY,
        SETTING_START_POINT,
        SETTING_DIRECTION_POINT,
        POSITIONING
    }

    private Status flag;

    private PeakStepDetector stepDetector;

    private DatabaseHelper db;
    //PDRのみ
    private PDRPositionCalculator pdrPositionCalculator;
    private DirectionCalculator directionCalculator;

    //PDR + スケルトンマッチング
    private PDRPositionCalculator skeletonMatchingPdrPositionCalculator;
    private DirectionCalculator skeletonMatchingDirectionCalculator;
    private SkeletonMatching mSkeletonMatching;

    //PDR + 当たり判定マッチング
    private PDRPositionCalculator collisionDetectMatchingPdrPositionCalculator;
    private DirectionCalculator collisionDetectMatchingDirectionCalculator;
    private CollisionDetectMatching mCollisionDetectMatching;

    private boolean isCollisionDetectSucMatchingSuccess = false;

    double startLat = 34.97948739467665;
    double startLng = 135.96445966511965;
    double directionLat = 34.97947695565969;
    double directionLng = 135.96473459154367;
//    double directionLat = 34.9794887;
//    double directionLng = 135.9643983;

    private DecimalFormat df = new DecimalFormat("0.00");

    private EnginePrefConfig enginePrefConfig;

    private android.os.Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.initialize(this);

        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        //  resetButton = (Button)findViewById(R.id.resetButton);
        //  resetButton.setOnClickListener(this);

        setupButton = (Button)findViewById(R.id.setupButton);
        setupButton.setOnClickListener(this);

        //  simulationModeButton = (Button)findViewById(R.id.simulationModeButton);
        //   simulationModeButton.setOnClickListener(this);

        //   directionTextView = (TextView)findViewById(R.id.directionText);
        //   directionTextView.setText("ready");

        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);

        alertDialog = new AlertDialog.Builder(this);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        double offset[] = {pref.getFloat(SettingsActivity.GYRO_OFFSET_X, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Y, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Z, 0.0f)};

        enginePrefConfig = new EnginePrefConfig(this);

        db = new DatabaseHelper(this, 1);

        accRawData = new ArrayList<>();
        gyroRawData = new ArrayList<>();
//        checkPointStepNum = new ArrayList<>();
        rawObjectArrayList = new ArrayList<>();

        this.handler = new Handler();
        /**
         * PDRの初期化
         */
        stepDetector = new PeakStepDetector(pref.getFloat(SettingsActivity.STEP_DIFF_THRESH_KEY, 1.0f), 0.4f);
        stepDetector.addListener(this);

        directionCalculator = new DirectionCalculator(offset);
        pdrPositionCalculator = new PDRPositionCalculator();

        skeletonMatchingDirectionCalculator = new DirectionCalculator(offset);
        skeletonMatchingPdrPositionCalculator = new PDRPositionCalculator();
        mSkeletonMatching = new SkeletonMatching(this, db);

        collisionDetectMatchingDirectionCalculator = new DirectionCalculator(offset);
        collisionDetectMatchingPdrPositionCalculator = new PDRPositionCalculator();
        mCollisionDetectMatching = new CollisionDetectMatching(this, db);
        mCollisionDetectMatching.addListener(this);
        isCollisionDetectSucMatchingSuccess = true;

        this.floorMap = BitmapDescriptorFactory.fromResource(R.drawable.floormap_cc5f);
        options = new GroundOverlayOptions();
        options.image(floorMap);
        options.anchor(0, 1);
        options.bearing(3f);
        options.position(new LatLng(34.979389,135.963716), 101.385f, 43.795f);
    }
    BitmapDescriptor floorMap;
    GroundOverlayOptions options = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Settings");
        menu.add(0, 1, 0, "Select Method");
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureID, MenuItem item){
        super.onMenuItemSelected(featureID, item);
        switch (item.getItemId()) {
            case 0:
                //オプション画面へ
                gotoSettingsActivity();
                return true;
            case 1:
                //PDRメソッド選択画面へ
                gotoSelectMethodActivity();
                return true;
        }
        return false;

    }

    protected void gotoSettingsActivity(){
        startActivity(new Intent(this, SettingsActivity.class));
    }

    protected void gotoSelectMethodActivity(){
        startActivity(new Intent(this, SelectMethodActivity.class));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(flag == Status.SETTING_START_POINT) {
            if(marker.getId().equals(getMarker(startMarkerId).getId())){
                startLat = marker.getPosition().latitude;
                startLng = marker.getPosition().longitude;
                showStartPDRDialog();
            }

        } else if(flag == Status.SETTING_DIRECTION_POINT) {
            if(marker.getId().equals(getMarker(directionMarkerId).getId())) {
                directionLat = marker.getPosition().latitude;
                directionLng = marker.getPosition().longitude;
                showStartPDRDialog();
            }
        }
        return false;
    }

    @Override
    public void onMapClick(LatLng point) {

        if (flag == Status.SETTING_START_POINT) {
			/*Start地点のマーカー*/
            if(markerList.size() > 0) {
                if(searchIndex(startMarkerId) != -1) {
                    removeMarker(startMarkerId);
                }
            }
            createMarker(startMarkerId, point, MarkerInfoObject.BLUE);

            if(markerList.size() > 0) {
                if(searchIndex(directionMarkerId) != -1) {
                    removePolyline(directionMarkerId);
                    drawPolyline2Points(directionMarkerId, point, markerList.get(searchIndex(directionMarkerId)).getLastPoint(), markerList.get(searchIndex(directionMarkerId)).getPolylineColor());
                    startDirection = -calc2PointDist(point, markerList.get(searchIndex(directionMarkerId)).getLastPoint())[1] + 90;
                }
            }

        } else if (flag == Status.SETTING_DIRECTION_POINT){
			/*方角を定めるマーカー*/
            if(markerList.size() > 0) {
                if(searchIndex(directionMarkerId) != -1) {
                    removeMarker(directionMarkerId);
                }
            }
            createMarker(directionMarkerId, point, MarkerInfoObject.GREEN);

            if(markerList.size() > 0) {
                if(searchIndex(startMarkerId) != -1) {
                    drawPolyline2Points(directionMarkerId, markerList.get(searchIndex(startMarkerId)).getLastPoint(), point, markerList.get(searchIndex(directionMarkerId)).getPolylineColor());
                    startDirection = -calc2PointDist(markerList.get(searchIndex(startMarkerId)).getLastPoint(), point)[1] + 90;
                }
            }

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(manager != null) {
            stopSensor();
        }
    }


    TrackPoint cmPdrTrackPoint = null;
    TrackPoint collisionDetectMatchingTrackPoint = null;
    List<Link> linkList;
    List<List<LatLng>> wallInfo;
    LatLng location = new LatLng(0,0);
    int polylineColor = Color.RED;
    Float stepLength = 75.0f;
    Float stepRate = 37500.0f;

    ArrayList<LatLng> latLngArrayList = new ArrayList<>();

    private void calculatePDR(long time) {
        pdrPositionCalculator.calculatePosition(directionCalculator.getRadiansDirection(),
                pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f),
                time,
                pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f));
        moveMarkerDefaultPolylineColor(pdrMarkerId, new LatLng(pdrPositionCalculator.getLat(), pdrPositionCalculator.getLng()));
    }

    private void calculateSkeletonMatch(long time) {
        skeletonMatchingPdrPositionCalculator.calculatePosition(skeletonMatchingDirectionCalculator.getRadiansDirection(),
                pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f),
                time,
                pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f));
        TrackPoint smPdrTrackPoint = new TrackPoint(time,
                new LatLng(skeletonMatchingPdrPositionCalculator.getLat(), skeletonMatchingPdrPositionCalculator.getLng()),
                skeletonMatchingPdrPositionCalculator.getDegreesCalibratedDirection(),
                skeletonMatchingPdrPositionCalculator.getCalibratedStepLength(),
                skeletonMatchingPdrPositionCalculator.getIsStraight(),
                "null");
        TrackPoint skeletonMatchingTrackPoint = mSkeletonMatching.calculateSkeletonMatchingPosition(smPdrTrackPoint);
        moveMarkerDefaultPolylineColor(skeletonMatchedMarkerId, skeletonMatchingTrackPoint.getLocation());
    }

//    private void

    public void onStep(long time) {
        System.gc();

        if (pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, false)) {
            calculatePDR(time);
            // directionTextView.setText("Raw PDR");
            return;
        }
        if (pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
            calculateSkeletonMatch(time);
            // directionTextView.setText("Skeleton Match");
            return;
        }

        if (pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
            // directionTextView.setText("PDR + MapMatch");
            collisionDetectMatchingPdrPositionCalculator.calculatePosition(
                    collisionDetectMatchingDirectionCalculator.getRadiansDirection(), //ラジアン形式な進行方向
//                    pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), //歩幅
                    stepLength,
                    time,
//                    pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f) //歩調
                    stepRate
            );

            if (isCollisionDetectSucMatchingSuccess) {
                cmPdrTrackPoint = new TrackPoint(
                        time,
                        new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng()),
                        collisionDetectMatchingPdrPositionCalculator.getDegreesCalibratedDirection(), //補正後のラジアン形式な進行方向
                        collisionDetectMatchingPdrPositionCalculator.getCalibratedStepLength(), //補正後の歩幅 //歩調を元にした補正
                        collisionDetectMatchingPdrPositionCalculator.getIsStraight(), //直進中かどうか //PDR的な細かなブレを吸収
                        "null");
            }
            collisionDetectMatchingTrackPoint = mCollisionDetectMatching.calculateCollisionDetectMatchingPosition(cmPdrTrackPoint);
            polylineColor = Color.BLACK;

            if (collisionDetectMatchingTrackPoint != null) {
                moveMarker(collisionDetectMatchingMarkerId, collisionDetectMatchingTrackPoint.getLocation());
                location = collisionDetectMatchingTrackPoint.getLocation();
                polylineColor = collisionDetectMatchingTrackPoint.polylineColor;

                this.latLngArrayList.add(location);

                map.clear();
                map.addGroundOverlay(options).setTransparency(0.1f);
//                    List<Link> linkList = mCollisionDetectMatching.getLinkList();
                if (pref.getBoolean(SelectMethodActivity.WALL_LINK_DRAW, false)) {
                    linkList = mCollisionDetectMatching.getLinkList();
                    if (linkList.size() > 0) {
                        wallInfo = mCollisionDetectMatching.mCollisionDetectMatchingHelper.getLinksWallInfo(linkList);

                        for (List<LatLng> wall : wallInfo) {
                            PolylineOptions po = new PolylineOptions()
                                    .color(Color.BLUE)
                                    .width(3)
                                    .addAll(wall);
//                            Polyline polyline = map.addPolyline(po);
                            map.addPolyline(po);
                        }


                        for (Link link : linkList) {
                            PolylineOptions po = new PolylineOptions()
                                    .width(3)
                                    .color(Color.GREEN)
                                    .add(db.getNodeById(link.getNode1Id()).getLatLng())
                                    .add(db.getNodeById(link.getNode2Id()).getLatLng());
//                            Polyline polyline = map.addPolyline(po);
                            map.addPolyline(po);
                        }
                    }
                }
//                    directionTextView.setText("" + df.format(collisionDetectMatchingTrackPoint.getDirection()) + "°, linkId:" + collisionDetectMatchingTrackPoint.getLinkId());
            } else {
                polylineColor = Color.YELLOW;
                //  directionTextView.setText("Raw PDR_CM");
//                isCollisionDetectSucMatchingSuccess = false;
                moveMarker(collisionDetectMatchingMarkerId, new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng()));
                location = new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng());
            }
        } else {
            location = new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng());
        }
        trajectoryMap.put(Long.toString(time), location);
        polylineColorMap.put(Long.toString(time), polylineColor);

        if (pref.getBoolean(SelectMethodActivity.COLORFUL_POLYLINE, false)) {
            moveMarkerWithPolylineColorColorful(collisionDetectMatchingMarkerId,
                    new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(),
                            collisionDetectMatchingPdrPositionCalculator.getLng()),
                    polylineColor
            );
            for (int i = 0; i < mCollisionDetectMatching.originLatLngArray.size(); i++) {
                LatLng origin = mCollisionDetectMatching.originLatLngArray.get(i);
                LatLng adjusted = mCollisionDetectMatching.adjustedLatLngArray.get(i);
                if (origin != null && adjusted != null) {
                    drawPolyline2Points(collisionDetectMatchingMarkerId, origin, adjusted, Color.BLUE);
                }
            }
        } else {
            moveMarkerWithPolyline(collisionDetectMatchingMarkerId,
                    new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(),
                            collisionDetectMatchingPdrPositionCalculator.getLng()),
                    Color.RED
            );
        }
    }

//        moveMarkerDefaultPolylineColorで既にPolylineしてるから無駄？
//        drawPolylineAllPoints2(collisionDetectMatchingMarkerId,
//                markerList.get(searchIndex(collisionDetectMatchingMarkerId)).getPolylineColor());

//        複数アルゴリズム併用の場合？
//        int index = searchIndex(collisionDetectMatchingMarkerId);
//        if(index > -1) {
//            removePolyline(collisionDetectMatchingMarkerId);
////            drawPolylineAllPoints2(collisionDetectMatchingMarkerId, 0);
////            drawPolylineAllPoints2(collisionDetectMatchingMarkerId, markerList.get(index).getPolylineColor());
//        }

    /*マップマッチングが成功した時、PDRの初期値と較正係数を更新する*/
    @Override
    public void onTrajectoryTransed(Point rate, Trajectory trajectory, TrackPoint newTrackPoint) {
        //Log.v("CM", "TrajectoryTransed");

        if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
//            Log.v("direction", "linkDirection:" + newTrackPoint.getDirection());
            collisionDetectMatchingPdrPositionCalculator.setPoint(newTrackPoint.getLocation().latitude, newTrackPoint.getLocation().longitude, newTrackPoint.getDirection());
                        directionCalculator.setDegreesDirection(newTrackPoint.getDirection());
            //MapMatching用のPDRクラスに較正係数を反映
            collisionDetectMatchingDirectionCalculator.setDirectionRate(rate.getX());
            collisionDetectMatchingPdrPositionCalculator.setDistanceRate(rate.getY());

//            if (pref.getBoolean(SelectMethodActivity.STRAIGHT_STEP_AUTO_ADJUST, false)) {
//                if (!newTrackPoint.isSkeletonMatch) {
//                    if (!mCollisionDetectMatching.originLatLngArray.isEmpty() && !mCollisionDetectMatching.adjustedLatLngArray.isEmpty()) {
//                        LatLng origin = mCollisionDetectMatching.originLatLngArray.get(mCollisionDetectMatching.originLatLngArray.size() - 1);
//                        LatLng adjusted = mCollisionDetectMatching.adjustedLatLngArray.get(mCollisionDetectMatching.adjustedLatLngArray.size() - 1);
//                        if (origin != null && adjusted != null) {
//                            float calc[] = calc2PointDist(origin, adjusted);
//                            int stepCount = mCollisionDetectMatching.getPassageFinishStepCount().get(mCollisionDetectMatching.getPassageFinishStepCount().size() - 1);
//                            float distance = stepCount * stepLength / 100;
//                            float coe;
//                            if (mCollisionDetectMatching.straightStepRatePlusMinus.get(mCollisionDetectMatching.straightStepRatePlusMinus.size() - 1)) {
//                                coe = (distance + calc[0]) / distance;
//                            } else {
//                                coe = (distance - calc[0]) / distance;
//                            }
//                            Toast.makeText(getApplicationContext(), "直進時歩幅を" + stepLength + "から" + stepLength * coe, Toast.LENGTH_SHORT).show();
//                            stepLength *= coe;
//                        }
//                    }
//                }
//            }

            removePolyline(collisionDetectMatchingMarkerId);
            for(TrackPoint trackPoint : trajectory.getTrajectory()) {
                trajectoryMap.put(Long.toString(trackPoint.getTime()), trackPoint.getLocation());
                polylineColorMap.put(Long.toString(trackPoint.getTime()), trackPoint.polylineColor);
            }
            //mCollisionDetectMatching.mSkeletonMatching.setFirst();
            drawPolylineAllPoints2(collisionDetectMatchingMarkerId, markerList.get(searchIndex(collisionDetectMatchingMarkerId)).getPolylineColor());
        }
    }

    @Override
    public void onClick(View v) {
/**
 * PDRを開始する
 */
        if (v.getId() == R.id.startButton) {

            if(!isStart) {
                if(flag == Status.POSITIONING) {
                    stepLength = pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f); //歩幅
                    stepRate = pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f); //歩調

                    int startMarkerIndex = searchIndex(startMarkerId);
                    int directionMarkerIndex = searchIndex(directionMarkerId);

                    LatLng rawPoint = markerList.get(startMarkerIndex).getLastPoint();
                    TrackPoint rawTrackPoint = new TrackPoint(0, rawPoint, startDirection, pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), true, "null");

                    if(pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                        pdrPositionCalculator.setPoint(rawPoint.latitude, rawPoint.longitude, startDirection);
                        directionCalculator.setDegreesDirection(startDirection);
                        createMarker(pdrMarkerId, rawPoint, MarkerInfoObject.RED);
                    }


                    if(pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                        TrackPoint skeletonMatchingTrackPoint = mSkeletonMatching.calculateSkeletonMatchingPosition(rawTrackPoint);
                        LatLng skeletonMatchingPoint = skeletonMatchingTrackPoint.getLocation();
                        skeletonMatchingPdrPositionCalculator.setPoint(rawPoint.latitude, rawPoint.longitude, startDirection);
                        skeletonMatchingDirectionCalculator.setDegreesDirection(startDirection);
                        createMarker(skeletonMatchedMarkerId, skeletonMatchingPoint, MarkerInfoObject.RED);
//                        directionTextView.setText("" + df.format(startDirection) + "°, link:" + skeletonMatchingTrackPoint.getLinkId());
                    }

                    if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                        TrackPoint collisionDetectMatchingTrackPoint = mCollisionDetectMatching.calculateCollisionDetectMatchingPosition(rawTrackPoint);
                        LatLng collisionDetectMatchingPoint = collisionDetectMatchingTrackPoint.getLocation();
//                        collisionDetectMatchingPdrPositionCalculator.setPoint(startLat, startLng, -3);
                        collisionDetectMatchingPdrPositionCalculator.setPoint(rawPoint.latitude, rawPoint.longitude, startDirection);
                        collisionDetectMatchingDirectionCalculator.setDegreesDirection(startDirection);
                        createMarker(collisionDetectMatchingMarkerId, collisionDetectMatchingPoint, MarkerInfoObject.RED);
//                        directionTextView.setText("" + df.format(startDirection) + "°, link:" + collisionDetectMatchingTrackPoint.getLinkId());
                    }

                    if (pref.getBoolean(SelectMethodActivity.RAW_DATA_MEASURE, false)) {
                        this.isRawDataMeasure = true;

                        try {
                            String name = "/RawData_" + new Date().toString() + ".txt";
                            this.printWriter = new PrintWriter(new BufferedWriter(new FileWriter(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + name)));
                            Toast.makeText(getApplicationContext(), "RawDataは " + name, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            this.isRawDataMeasure = false;
                        }
                    }
                    markerList.get(startMarkerIndex).getMarker().remove();
                    markerList.get(directionMarkerIndex).getMarker().remove();
                    markerList.get(directionMarkerIndex).getPolyline().remove();

                    isStart = true;

                    if (isRawObjectsLoaded) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                long lastTimestamp = rawObjectArrayList.get(0).timestamp;
                                long timestamp;

                                for (final RawObject object : rawObjectArrayList) {
                                    timestamp = object.timestamp;

                                    try {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                onSensorChangedSimulator(object);
                                            }
                                        });
                                        Thread.sleep((timestamp - lastTimestamp) / 1000000, 0);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    lastTimestamp = timestamp;
                                }
                            }
                        }).start();
                    } else {
                        startSensor();
                    }

                } else  {
                    alertDialog.setTitle("注意");
                    alertDialog.setMessage("先にスタート位置のセットアップをしてください");
                    alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    // ダイアログの作成と表示
                    alertDialog.create();
                    alertDialog.show();
                }
            } else {
                isStart = false;
                stopSensor();
                startButton.setText("Start");
            }
        }  else if(v.getId() == R.id.select_start_from_map_button) {
            mInitializePDRDialog.dismiss();
            flag = Status.SETTING_START_POINT;
        } else if(v.getId() == R.id.select_direction_from_map_button) {
            mInitializePDRDialog.dismiss();
            flag = Status.SETTING_DIRECTION_POINT;
        } else if (v.getId() == R.id.setupButton){
            showStartPDRDialog();
        }
    }

    private boolean loadRawData(String fileName) {
        try {
            this.rawObjectArrayList.clear();

            String fileFullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;
            File file = new File(fileFullPath);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String line = bufferedReader.readLine();
            while (line != null) {
                if (!line.equals("#")) {
                    String[] splitLine = line.split(",");
                    if(splitLine.length < 5) {
                        this.checkPointTimes_loaded.add(Long.parseLong(splitLine[0]));
                    } else {
                        float[] values = {Float.parseFloat(splitLine[2]), Float.parseFloat(splitLine[3]), Float.parseFloat(splitLine[4])};
                        this.rawObjectArrayList.add(new RawObject(Integer.parseInt(splitLine[0]), Long.parseLong(splitLine[1]), values));
                    }
                }
                line = bufferedReader.readLine();
            }
            bufferedReader.close();

            Collections.sort(this.rawObjectArrayList, new RawObjectComparator());
            this.isRawObjectsLoaded = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            this.isRawObjectsLoaded = false;
            this.rawObjectArrayList.clear();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            this.isRawObjectsLoaded = false;
            this.rawObjectArrayList.clear();
            return false;
        }
        return true;
    }


    ArrayList<RawObject> rawObjectArrayList;
    boolean isRawObjectsLoaded = false;

    PrintWriter printWriter;
    ArrayList<String> accRawData;
    ArrayList<String> gyroRawData;
    ArrayList<Long> checkPointTimes_loaded = new ArrayList<>();
    boolean isRawDataMeasure = false;

    public void onSensorChangedSimulator(RawObject object) {
        if (object.getType() == Sensor.TYPE_ACCELEROMETER) {

            stepDetector.detectStepAndNotify(object.values, object.timestamp);

            if (pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                directionCalculator.calculateLean(object.values);
            }

            if (pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                skeletonMatchingDirectionCalculator.calculateLean(object.values);
            }

            if (pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                collisionDetectMatchingDirectionCalculator.calculateLean(object.values);
            }

        } else if (object.getType() == Sensor.TYPE_GYROSCOPE) {

            if (pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                directionCalculator.calculateDirection(object.values, object.timestamp);
            }

            if (pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                skeletonMatchingDirectionCalculator.calculateDirection(object.values, object.timestamp);
            }

            if (pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                collisionDetectMatchingDirectionCalculator.calculateDirection(object.values, object.timestamp);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRawDataMeasure) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accRawData.add(String.valueOf(event.sensor.getType()) + "," +
                                    String.valueOf(event.timestamp) + "," +
                                    String.valueOf(event.values[0]) + "," +
                                    String.valueOf(event.values[1]) + "," +
                                    String.valueOf(event.values[2])
                    );
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroRawData.add(String.valueOf(event.sensor.getType()) + "," +
                                    String.valueOf(event.timestamp) + "," +
                                    String.valueOf(event.values[0]) + "," +
                                    String.valueOf(event.values[1]) + "," +
                                    String.valueOf(event.values[2])
                    );
                    break;
            }
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                stepDetector.detectStepAndNotify(event.values, event.timestamp);

                if (pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                    directionCalculator.calculateLean(event.values);
                }

                if (pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                    skeletonMatchingDirectionCalculator.calculateLean(event.values);
                }

                if (pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                    collisionDetectMatchingDirectionCalculator.calculateLean(event.values);
                }

            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

                if (pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                    directionCalculator.calculateDirection(event.values, event.timestamp);
                }

                if (pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                    skeletonMatchingDirectionCalculator.calculateDirection(event.values, event.timestamp);
                }

                if (pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                    collisionDetectMatchingDirectionCalculator.calculateDirection(event.values, event.timestamp);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startSensor() {
        manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopSensor() {
        manager.unregisterListener(this);

    }

    private void showStartPDRDialog() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.view_initial_pdr, (ViewGroup) this.findViewById(R.id.alert_dialog));

        mStartLatitudeEditText = (EditText)layout.findViewById(R.id.start_latitude_edit_text);
        mStartLatitudeEditText.setText(Double.toString(startLat));
        mStartLongitudeEditText = (EditText)layout.findViewById(R.id.start_longitude_edit_text);
        mStartLongitudeEditText.setText(Double.toString(startLng));
        mDirectionLatitudeEditText = (EditText)layout.findViewById(R.id.direction_latitude_edit_text);
        mDirectionLatitudeEditText.setText(Double.toString(directionLat));
        mDirectionLongitudeEditText = (EditText)layout.findViewById(R.id.direction_longitude_edit_text);
        mDirectionLongitudeEditText.setText(Double.toString(directionLng));

        mInitializePDRDialog = new AlertDialog.Builder(this)
                .setTitle("初期位置・初期方向設定")
                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mInitializePDRDialog.dismiss();
                        flag = Status.POSITIONING;

                        startLat = Double.valueOf(mStartLatitudeEditText.getText().toString());
                        startLng = Double.valueOf(mStartLongitudeEditText.getText().toString());
                        directionLat = Double.valueOf(mDirectionLatitudeEditText.getText().toString());
                        directionLng = Double.valueOf(mDirectionLongitudeEditText.getText().toString());

                        if(markerList.size() > 0) {
                            if(searchIndex(startMarkerId) != -1) {
                                removeMarker(startMarkerId);
                            }
                            if(searchIndex(directionMarkerId) != -1) {
                                removeMarker(directionMarkerId);
                            }
                        }

                        LatLng startPoint = new LatLng(startLat, startLng);
                        LatLng directionPoint = new LatLng(directionLat, directionLng);
                        createMarker(startMarkerId, startPoint, MarkerInfoObject.BLUE);
                        createMarker(directionMarkerId, directionPoint, MarkerInfoObject.GREEN);
                        drawPolyline2Points(directionMarkerId, startPoint, directionPoint, markerList.get(searchIndex(directionMarkerId)).getPolylineColor());
                        startDirection = -calc2PointDist(startPoint, directionPoint)[1] + 90;
                    }
                })
                .show();

        selectStartPinButton = (Button)layout.findViewById(R.id.select_start_from_map_button);
        selectStartPinButton.setOnClickListener(this);

        selectDirectionPinButton = (Button)layout.findViewById(R.id.select_direction_from_map_button);
        selectDirectionPinButton.setOnClickListener(this);
    }

    /**
     * ファイルに書かれたqueryを実行する
     * @param queryFile
     */
    public List<String> getQueryFromFile(String queryFile) {
        AssetManager as = this.getResources().getAssets();
        InputStream is = null;
        BufferedReader br = null;

        List<String> queryList = new ArrayList<>();
        try {
            try {
                // assetsフォルダ内の sample.txt をオープンする
                is = as.open(queryFile);
                br = new BufferedReader(new InputStreamReader(is));

                String query;
                while ((query = br.readLine()) != null) {
                    queryList.add(query);
                }
            } finally {
                if (is != null) is.close();
                if (br != null) br.close();
            }
        } catch (Exception e){
            // エラー発生時の処理
        }

        return  queryList;
    }
}
