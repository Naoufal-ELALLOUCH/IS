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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
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
import ubilabmapmatchinglibrary.mapmatching.TrajectoryTransedListener;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;


public class PDRMainActivity extends FloorMapActivity implements StepListener, TrajectoryTransedListener, SensorEventListener, OnClickListener , GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
//    public static final String DB_QUERY_FILE = "cc5f_network_sql.txt";
    public static final String DB_QUERY_FILE = "cc_psn.txt";

    private SensorManager manager;
    private SharedPreferences pref;
    private AlertDialog.Builder alertDialog;

    private AlertDialog mInitializePDRDialog;
    private EditText mStartLatitudeEditText;
    private EditText mStartLongitudeEditText;
    private EditText mDirectionLatitudeEditText;
    private EditText mDirectionLongitudeEditText;

    private Button startButton;
    private Button resetButton;
    private Button setupButton;
    private Button selectStartPinButton;
    private Button selectDirectionPinButton;
    private TextView directionTextView;

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
        POSITIONING;
    }

    private Status flag;

    private PeakStepDetector stepDetector;

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

    private DecimalFormat df = new DecimalFormat("0.00");

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.initialize(this);

        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        resetButton = (Button)findViewById(R.id.resetButton);
        resetButton.setOnClickListener(this);

        setupButton = (Button)findViewById(R.id.setupButton);
        setupButton.setOnClickListener(this);

        directionTextView = (TextView)findViewById(R.id.directionText);
        directionTextView.setText("ready");

        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);

        alertDialog = new AlertDialog.Builder(this);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        double offset[] = {pref.getFloat(SettingsActivity.GYRO_OFFSET_X, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Y, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Z, 0.0f)};

        db = new DatabaseHelper(this);
        try {

            db.createEmptyDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }


        //db = new DatabaseHelper(this, DatabaseHelper.DATABASE_VERSION);
        //db.execQueryList(getQueryFromFile(DB_QUERY_FILE));

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

    }


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


    public void onStep(long time) {

        if(pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
            pdrPositionCalculator.calculatePosition(directionCalculator.getRadiansDirection(), pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), time, pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f));
            moveMarkerDefaultPolylineColor(pdrMarkerId, new LatLng(pdrPositionCalculator.getLat(), pdrPositionCalculator.getLng()));
        }

        if(pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
            skeletonMatchingPdrPositionCalculator.calculatePosition(skeletonMatchingDirectionCalculator.getRadiansDirection(), pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), time, pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f));
            TrackPoint smPdrTrackPoint = new TrackPoint(time, new LatLng(skeletonMatchingPdrPositionCalculator.getLat(), skeletonMatchingPdrPositionCalculator.getLng()), skeletonMatchingPdrPositionCalculator.getDegreesCalibratedDirection(), skeletonMatchingPdrPositionCalculator.getCalibratedStepLength(), skeletonMatchingPdrPositionCalculator.getIsStraight(), -1);
            TrackPoint skeletonMatchingTrackPoint = mSkeletonMatching.calculateSkeletonMatchingPosition(smPdrTrackPoint);
            moveMarkerDefaultPolylineColor(skeletonMatchedMarkerId, skeletonMatchingTrackPoint.getLocation());
//            directionTextView.setText("" + df.format(skeletonMatchingTrackPoint.getDirection()) + "°, straight:" + skeletonMatchingTrackPoint.getIsStraight());
            directionTextView.setText("" + df.format(skeletonMatchingTrackPoint.getDirection()) + "°, linkId:" + skeletonMatchingTrackPoint.getLinkId());
        }

        LatLng location = new LatLng(0,0);
        if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
            collisionDetectMatchingPdrPositionCalculator.calculatePosition(collisionDetectMatchingDirectionCalculator.getRadiansDirection(), pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), time, pref.getFloat(SettingsActivity.STEP_RATE_KEY, 37500.0f));
            if(isCollisionDetectSucMatchingSuccess) {

                TrackPoint cmPdrTrackPoint = new TrackPoint(time, new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng()), collisionDetectMatchingPdrPositionCalculator.getDegreesCalibratedDirection(), collisionDetectMatchingPdrPositionCalculator.getCalibratedStepLength(), collisionDetectMatchingPdrPositionCalculator.getIsStraight(), -1);
                TrackPoint collisionDetectMatchingTrackPoint = mCollisionDetectMatching.calculateCollisionDetectMatchingPosition(cmPdrTrackPoint);

                if (collisionDetectMatchingTrackPoint != null) {
                    moveMarker(collisionDetectMatchingMarkerId, collisionDetectMatchingTrackPoint.getLocation());
                    location = collisionDetectMatchingTrackPoint.getLocation();

                    directionTextView.setText("" + df.format(collisionDetectMatchingTrackPoint.getDirection()) + "°, linkId:" + collisionDetectMatchingTrackPoint.getLinkId());
                } else {
                    isCollisionDetectSucMatchingSuccess = false;
                    moveMarker(collisionDetectMatchingMarkerId, new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng()));
                    location = new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng());

                }
            } else {
                moveMarkerDefaultPolylineColor(collisionDetectMatchingMarkerId, new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng()));
                location = new LatLng(collisionDetectMatchingPdrPositionCalculator.getLat(), collisionDetectMatchingPdrPositionCalculator.getLng());
            }
        }

        removePolyline(collisionDetectMatchingMarkerId);
        trajectoryMap.put(Long.toString(time), location);
        drawPolylineAllPoints2(collisionDetectMatchingMarkerId, markerList.get(searchIndex(collisionDetectMatchingMarkerId)).getPolylineColor());

    }

    /*マップマッチングが成功した時、PDRの初期値と較正係数を更新する*/
    @Override
    public void onTrajectoryTransed(Point rate, Trajectory trajectory, TrackPoint newTrackPoint) {
        Log.v("CM", "TrajectoryTransed");

        if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
            collisionDetectMatchingPdrPositionCalculator.setPoint(newTrackPoint.getLocation().latitude, newTrackPoint.getLocation().longitude, newTrackPoint.getDirection());
            //MapMatching用のPDRクラスに較正係数を反映
            collisionDetectMatchingDirectionCalculator.setDirectionRate(rate.getX());
            collisionDetectMatchingPdrPositionCalculator.setDistanceRate(rate.getY());

//            removePolyline(collisionDetectMatchingMarkerId);
//            markerList.get(searchIndex(collisionDetectMatchingMarkerId)).getPoints().clear();
//            for (TrackPoint polylineTrackPoint : trajectory.getTrajectory()) {
//                addPolylinePoint(collisionDetectMatchingMarkerId, polylineTrackPoint.getLocation());
//            }
//            drawPolylineAllPoints(collisionDetectMatchingMarkerId, markerList.get(searchIndex(collisionDetectMatchingMarkerId)).getPolylineColor());


            removePolyline(collisionDetectMatchingMarkerId);
            for(TrackPoint trackPoint : trajectory.getTrajectory()) {
                trajectoryMap.put(Long.toString(trackPoint.getTime()), trackPoint.getLocation());
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

                    int startMarkerIndex = searchIndex(startMarkerId);
                    int directionMarkerIndex = searchIndex(directionMarkerId);

                    LatLng rawPoint = markerList.get(startMarkerIndex).getLastPoint();
                    TrackPoint rawTrackPoint = new TrackPoint(0, rawPoint, startDirection, pref.getFloat(SettingsActivity.STEP_LENGTH_KEY, 75.0f), true, -1);

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
                        directionTextView.setText("" + df.format(startDirection) + "°, link:" + skeletonMatchingTrackPoint.getLinkId());
                    }

                    if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                        TrackPoint collisionDetectMatchingTrackPoint = mCollisionDetectMatching.calculateCollisionDetectMatchingPosition(rawTrackPoint);
                        LatLng collisionDetectMatchingPoint = collisionDetectMatchingTrackPoint.getLocation();
                        collisionDetectMatchingPdrPositionCalculator.setPoint(startLat, startLng, -3);
                        // collisionDetectMatchingPdrPositionCalculator.setPoint(rawPoint.latitude, rawPoint.longitude, startDirection);
                        collisionDetectMatchingDirectionCalculator.setDegreesDirection(startDirection);
                        createMarker(collisionDetectMatchingMarkerId, collisionDetectMatchingPoint, MarkerInfoObject.RED);
                    }


                    startSensor();
                    startButton.setText("Pause");

                    markerList.get(startMarkerIndex).getMarker().remove();
                    markerList.get(directionMarkerIndex).getMarker().remove();
                    markerList.get(directionMarkerIndex).getPolyline().remove();

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
            }
        } else if (v.getId() == R.id.resetButton) {

            List<Link> linkList = new ArrayList<>();
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(314));
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(316));
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(306));
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(177));
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(179));
            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(181));

            List<List<LatLng>> wallInfo = mCollisionDetectMatching.mCollisionDetectMatchingHelper.getLinksWallInfo(linkList);

            for(List<LatLng> wall : wallInfo) {
                PolylineOptions po = new PolylineOptions()
                        .color(Color.BLUE)
                        .width(3)
                        .addAll(wall);
                Polyline polyline = map.addPolyline(po);
            }


            for(Link link : linkList) {
                PolylineOptions po = new PolylineOptions()
                        .width(3)
                        .color(Color.RED)
                        .add(db.getNodeById(link.getNode1Id()).getLatLng())
                        .add(db.getNodeById(link.getNode2Id()).getLatLng());
                Polyline polyline = map.addPolyline(po);
            }

            /**
             * 軌跡、現在地をリセットする
             */
            if (isStart) {
                stopSensor();
            }

//            alertDialog.setTitle("注意");
//            alertDialog.setMessage("これまでの移動をリセットしますか?");
//            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    map.clear();
//                    markerList.clear();// = new ArrayList();
//                    setMap();
//                    flag = Status.READY;
//
//                    if(isStart){
//                        Log.v("PDR","Stop");
//                        startButton.setText("Start");
//                        isStart = false;
//                    }
//                }
//            });
//            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//
//                public void onClick(DialogInterface dialog, int which) {
//                    if(isStart){
//                        Log.v("PDR","Stop");
//                        startButton.setText("Start");
//                        isStart = false;
//                        startSensor();
//                    }
//                }
//            });
//
//            // ダイアログの作成と表示
//            alertDialog.create();
//            alertDialog.show();

        } else if(v.getId() == R.id.select_start_from_map_button) {
            mInitializePDRDialog.dismiss();
            flag = Status.SETTING_START_POINT;
        } else if(v.getId() == R.id.select_direction_from_map_button) {
            mInitializePDRDialog.dismiss();
            flag = Status.SETTING_DIRECTION_POINT;
        } else if (v.getId() == R.id.setupButton){
            showStartPDRDialog();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            stepDetector.detectStepAndNotify(event.values, event.timestamp);

            if(pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                directionCalculator.calculateLean(event.values);
            }

            if(pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                skeletonMatchingDirectionCalculator.calculateLean(event.values);
            }

            if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                collisionDetectMatchingDirectionCalculator.calculateLean(event.values);
            }

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if(pref.getBoolean(SelectMethodActivity.METHOD_PDR_KEY, true)) {
                directionCalculator.calculateDirection(event.values, event.timestamp);
            }

            if(pref.getBoolean(SelectMethodActivity.METHOD_SM_KEY, false)) {
                skeletonMatchingDirectionCalculator.calculateDirection(event.values, event.timestamp);
            }

            if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
                collisionDetectMatchingDirectionCalculator.calculateDirection(event.values, event.timestamp);
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
