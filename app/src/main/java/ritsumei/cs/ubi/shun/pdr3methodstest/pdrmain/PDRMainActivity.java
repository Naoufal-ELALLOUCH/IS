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
    private Button resultButton;
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

    private DecimalFormat df = new DecimalFormat("0.00");

    private EnginePrefConfig enginePrefConfig;

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

        resultButton = (Button)findViewById(R.id.resultButton);
        resultButton.setOnClickListener(this);

        directionTextView = (TextView)findViewById(R.id.directionText);
        directionTextView.setText("ready");

        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);

        alertDialog = new AlertDialog.Builder(this);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        double offset[] = {pref.getFloat(SettingsActivity.GYRO_OFFSET_X, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Y, 0.0f), pref.getFloat(SettingsActivity.GYRO_OFFSET_Z, 0.0f)};

        enginePrefConfig = new EnginePrefConfig(this);

        db = new DatabaseHelper(this, 1);

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

                    map.clear();
                    List<Link> linkList = mCollisionDetectMatching.getLinkList();
                    if(linkList.size() > 0) {
                        List<List<LatLng>> wallInfo = mCollisionDetectMatching.mCollisionDetectMatchingHelper.getLinksWallInfo(linkList);

                        for (List<LatLng> wall : wallInfo) {
                            PolylineOptions po = new PolylineOptions()
                                    .color(Color.BLUE)
                                    .width(3)
                                    .addAll(wall);
                            Polyline polyline = map.addPolyline(po);
                        }


                        for (Link link : linkList) {
                            PolylineOptions po = new PolylineOptions()
                                    .width(3)
                                    .color(Color.GREEN)
                                    .add(db.getNodeById(link.getNode1Id()).getLatLng())
                                    .add(db.getNodeById(link.getNode2Id()).getLatLng());
                            Polyline polyline = map.addPolyline(po);
                        }
                    }
                    directionTextView.setText("" + df.format(collisionDetectMatchingTrackPoint.getDirection()) + "°, linkId:" + collisionDetectMatchingTrackPoint.getLinkId());
                } else {

                    //Log.e("Activity", "change raw PDR");
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
        //Log.v("CM", "TrajectoryTransed");

        if(pref.getBoolean(SelectMethodActivity.METHOD_CM_KEY, false)) {
            Log.v("direction", "linkDirection:" + newTrackPoint.getDirection());
            collisionDetectMatchingPdrPositionCalculator.setPoint(newTrackPoint.getLocation().latitude, newTrackPoint.getLocation().longitude, newTrackPoint.getDirection());
                        directionCalculator.setDegreesDirection(newTrackPoint.getDirection());
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
//                        collisionDetectMatchingPdrPositionCalculator.setPoint(startLat, startLng, -3);
                        collisionDetectMatchingPdrPositionCalculator.setPoint(rawPoint.latitude, rawPoint.longitude, startDirection);
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

//            List<Link> linkList = new ArrayList<>();
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(314));
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(316));
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(306));
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(177));
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(179));
//            linkList.add(mCollisionDetectMatching.mCollisionDetectMatchingHelper.db.getLinkById(181));
//
//            List<List<LatLng>> wallInfo = mCollisionDetectMatching.mCollisionDetectMatchingHelper.getLinksWallInfo(linkList);
//
//            for(List<LatLng> wall : wallInfo) {
//                PolylineOptions po = new PolylineOptions()
//                        .color(Color.BLUE)
//                        .width(3)
//                        .addAll(wall);
//                Polyline polyline = map.addPolyline(po);
//            }
//
//
//            for(Link link : linkList) {
//                PolylineOptions po = new PolylineOptions()
//                        .width(3)
//                        .color(Color.RED)
//                        .add(db.getNodeById(link.getNode1Id()).getLatLng())
//                        .add(db.getNodeById(link.getNode2Id()).getLatLng());
//                Polyline polyline = map.addPolyline(po);
//            }

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
//                        //Log.v("PDR","Stop");
//                        startButton.setText("Start");
//                        isStart = false;
//                    }
//                }
//            });
//            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//
//                public void onClick(DialogInterface dialog, int which) {
//                    if(isStart){
//                        //Log.v("PDR","Stop");
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
        } else if(v.getId() == R.id.resultButton){


//            //rootA
//            double[] correctLat = {
//                    34.70163123,
//                    34.7017335,
//                    34.701841,
//                    34.70191128,
//                    34.70195539,
//                    34.70197661,
//                    34.7019846,
//                    34.70198984};
//          double[] correctLng = {
//                  135.4993422,
//                  135.499314,
//                  135.4993425,
//                  135.4994065,
//                  135.4994776,
//                  135.4996285,
//                  135.4997797,
//                  135.4999832};
//
//            double[] rawPdrLat = {
//                    34.70163724
//                    ,34.70164337
//                    ,34.7016495
//                    ,34.70165563
//                    ,34.70166176
//                    ,34.70166789
//                    ,34.70167402
//                    ,34.70168015
//                    ,34.70168629
//                    ,34.70169242
//                    ,34.70169855
//                    ,34.70170468
//                    ,34.70171081
//                    ,34.70171694
//                    ,34.70172307
//                    ,34.7017292
//                    ,34.70173533
//                    ,34.70174146
//                    ,34.7017476
//                    ,34.70175373
//                    ,34.70175986
//                    ,34.7017661
//                    ,34.70177213
//                    ,34.70177793
//                    ,34.70178361
//                    ,34.70178926
//                    ,34.70179464
//                    ,34.7018
//                    ,34.70180517
//                    ,34.70181031
//                    ,34.70181527
//                    ,34.70182026
//                    ,34.70182525
//                    ,34.70183023
//                    ,34.70183522
//                    ,34.70184021
//                    ,34.7018448
//                    ,34.70184943
//                    ,34.70185391
//                    ,34.70185839
//                    ,34.70186287
//                    ,34.70186736
//                    ,34.70187184
//                    ,34.70187632
//                    ,34.70188081
//                    ,34.70188529
//                    ,34.70188977
//                    ,34.70189412
//                    ,34.70189846
//                    ,34.7019028
//                    ,34.70190714
//                    ,34.70191149
//                    ,34.70191583
//                    ,34.70192017
//                    ,34.70192451
//                    ,34.70192886
//                    ,34.7019332
//                    ,34.70193754
//                    ,34.70194023
//                    ,34.70194287
//                    ,34.70194497
//                    ,34.70194636
//                    ,34.70194637
//                    ,34.70194637
//                    ,34.70194624
//                    ,34.70194629
//                    ,34.70194634
//                    ,34.70194638
//                    ,34.70194643
//                    ,34.70194647
//                    ,34.70194652
//                    ,34.70194656
//                    ,34.70194661
//                    ,34.70194665
//                    ,34.7019467
//                    ,34.70194674
//                    ,34.70194679
//                    ,34.70194683
//                    ,34.70194688
//                    ,34.70194692
//                    ,34.70194697
//                    ,34.70194702
//                    ,34.70194706
//                    ,34.70194606
//                    ,34.70194503
//                    ,34.701944
//                    ,34.70194298
//                    ,34.70194195
//                    ,34.70194148
//                    ,34.701941
//                    ,34.70194053
//                    ,34.70194006
//                    ,34.70193958
//                    ,34.70193911
//                    ,34.70193864
//                    ,34.70193816
//                    ,34.70193769
//                    ,34.70193722
//                    ,34.70193674
//                    ,34.70193627
//                    ,34.7019358
//                    ,34.70193533
//                    ,34.70193485
//                    ,34.70193438
//                    ,34.70193391
//                    ,34.70193343
//                    ,34.70193296
//                    ,34.70193249
//                    ,34.70193201
//                    ,34.70193154
//                    ,34.70193183
//                    ,34.70193212
//                    ,34.70193241
//                    ,34.70193146
//                    ,34.70193054
//                    ,34.70192962
//                    ,34.70192869
//                    ,34.70192848
//                    ,34.70192802
//                    ,34.70192798
//                    ,34.70192794
//                    ,34.7019279
//                    ,34.70192786
//                    ,34.70192782
//                    ,34.70192778
//                    ,34.70192774
//                    ,34.7019277
//                    ,34.70192766
//                    ,34.70192762
//                    ,34.70192758
//                    ,34.70192601
//            };
//            double[] rawPdrLng = {
//                    135.4993405
//                    ,135.4993388
//                    ,135.4993371
//                    ,135.4993354
//                    ,135.4993337
//                    ,135.499332
//                    ,135.4993303
//                    ,135.4993286
//                    ,135.4993269
//                    ,135.4993252
//                    ,135.4993235
//                    ,135.4993218
//                    ,135.4993201
//                    ,135.4993184
//                    ,135.4993167
//                    ,135.499315
//                    ,135.4993133
//                    ,135.4993116
//                    ,135.4993099
//                    ,135.4993082
//                    ,135.4993065
//                    ,135.4993072
//                    ,135.4993093
//                    ,135.4993122
//                    ,135.4993155
//                    ,135.4993189
//                    ,135.4993228
//                    ,135.4993268
//                    ,135.4993312
//                    ,135.4993356
//                    ,135.4993403
//                    ,135.4993449
//                    ,135.4993496
//                    ,135.4993543
//                    ,135.4993589
//                    ,135.4993636
//                    ,135.4993688
//                    ,135.499374
//                    ,135.4993794
//                    ,135.4993848
//                    ,135.4993901
//                    ,135.4993955
//                    ,135.4994009
//                    ,135.4994063
//                    ,135.4994116
//                    ,135.499417
//                    ,135.4994224
//                    ,135.4994279
//                    ,135.4994335
//                    ,135.499439
//                    ,135.4994445
//                    ,135.4994501
//                    ,135.4994556
//                    ,135.4994612
//                    ,135.4994667
//                    ,135.4994722
//                    ,135.4994778
//                    ,135.4994833
//                    ,135.4994902
//                    ,135.4994972
//                    ,135.4995044
//                    ,135.4995118
//                    ,135.4995195
//                    ,135.4995271
//                    ,135.4995348
//                    ,135.4995425
//                    ,135.4995501
//                    ,135.4995578
//                    ,135.4995654
//                    ,135.4995731
//                    ,135.4995808
//                    ,135.4995884
//                    ,135.4995961
//                    ,135.4996038
//                    ,135.4996114
//                    ,135.4996191
//                    ,135.4996267
//                    ,135.4996344
//                    ,135.4996421
//                    ,135.4996497
//                    ,135.4996574
//                    ,135.499665
//                    ,135.4996727
//                    ,135.4996803
//                    ,135.4996878
//                    ,135.4996954
//                    ,135.4997029
//                    ,135.4997105
//                    ,135.4997181
//                    ,135.4997258
//                    ,135.4997334
//                    ,135.499741
//                    ,135.4997487
//                    ,135.4997563
//                    ,135.499764
//                    ,135.4997716
//                    ,135.4997792
//                    ,135.4997869
//                    ,135.4997945
//                    ,135.4998022
//                    ,135.4998098
//                    ,135.4998174
//                    ,135.4998251
//                    ,135.4998327
//                    ,135.4998404
//                    ,135.499848
//                    ,135.4998556
//                    ,135.4998633
//                    ,135.4998709
//                    ,135.4998786
//                    ,135.4998862
//                    ,135.4998938
//                    ,135.4999015
//                    ,135.4999091
//                    ,135.4999166
//                    ,135.4999242
//                    ,135.4999318
//                    ,135.4999394
//                    ,135.4999471
//                    ,135.4999547
//                    ,135.4999624
//                    ,135.4999701
//                    ,135.4999777
//                    ,135.4999854
//                    ,135.499993
//                    ,135.5000007
//                    ,135.5000084
//                    ,135.500016
//                    ,135.5000237
//                    ,135.5000313
//                    ,135.5000387
//
//            };
//
//            double[] smLat = {
//                    34.70164701
//                    ,34.70165295
//                    ,34.70165901
//                    ,34.70166507
//                    ,34.70167113
//                    ,34.70167719
//                    ,34.70168325
//                    ,34.70168931
//                    ,34.70169537
//                    ,34.70170143
//                    ,34.7017075
//                    ,34.70171356
//                    ,34.70171962
//                    ,34.70172568
//                    ,34.70173174
//                    ,34.7017378
//                    ,34.70174386
//                    ,34.70174992
//                    ,34.70175598
//                    ,34.70176204
//                    ,34.7017681
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.70177071
//                    ,34.7018404
//                    ,34.70184629
//                    ,34.70185217
//                    ,34.70185806
//                    ,34.70184844
//                    ,34.70185416
//                    ,34.7018598
//                    ,34.70188075
//                    ,34.7018864
//                    ,34.70189204
//                    ,34.70189768
//                    ,34.70190333
//                    ,34.70190897
//                    ,34.70191461
//                    ,34.70192132
//                    ,34.70192949
//                    ,34.70192421
//                    ,34.70192566
//                    ,34.7019271
//                    ,34.70192855
//                    ,34.70193
//                    ,34.70193144
//                    ,34.70193289
//                    ,34.70193433
//                    ,34.70193578
//                    ,34.70194003
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70193719
//                    ,34.70194395
//                    ,34.70194473
//                    ,34.7019455
//                    ,34.70194627
//                    ,34.70194704
//                    ,34.70194781
//                    ,34.70194858
//                    ,34.70194936
//                    ,34.70195013
//                    ,34.7019509
//                    ,34.70195167
//                    ,34.70195244
//                    ,34.70195321
//                    ,34.70195399
//                    ,34.70195476
//                    ,34.70195553
//                    ,34.7019563
//                    ,34.70196018
//                    ,34.70196093
//                    ,34.70195855
//                    ,34.7019593
//                    ,34.70196005
//                    ,34.70196395
//                    ,34.70196158
//                    ,34.70196234
//                    ,34.70196311
//                    ,34.70196387
//                    ,34.70196464
//                    ,34.7019654
//                    ,34.70196616
//                    ,34.70196693
//                    ,34.70196769
//                    ,34.70196846
//                    ,34.70196922
//                    ,34.70196999
//                    ,34.70197075
//                    ,34.70197151
//                    ,34.70197228
//                    ,34.70197304
//                    ,34.70197374
//                    ,34.70197429
//                    ,34.70197483
//                    ,34.70197538
//                    ,34.70197593
//                    ,34.70197771
//                    ,34.70197703
//                    ,34.70197758
//                    ,34.70197771
//                    ,34.70197771
//                    ,34.70197808
//                    ,34.70197821
//                    ,34.70197896
//                    ,34.7019791
//                    ,34.70197923
//                    ,34.70197875
//                    ,34.70197889
//                    ,34.70197903
//                    ,34.70197916
//                    ,34.7019793
//                    ,34.70197943
//                    ,34.70197957
//                    ,34.7019797
//                    ,34.70197984
//                    ,34.70197997
//                    ,34.70198072
//            };
//
//            double[] smLng = {
//                    135.4993713
//                    ,135.4993695
//                    ,135.4993675
//                    ,135.4993656
//                    ,135.4993637
//                    ,135.4993618
//                    ,135.4993598
//                    ,135.4993579
//                    ,135.499356
//                    ,135.4993541
//                    ,135.4993522
//                    ,135.4993502
//                    ,135.4993483
//                    ,135.4993464
//                    ,135.4993445
//                    ,135.4993425
//                    ,135.4993406
//                    ,135.4993387
//                    ,135.4993368
//                    ,135.4993348
//                    ,135.4993329
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993321
//                    ,135.4993552
//                    ,135.4993571
//                    ,135.4993591
//                    ,135.499361
//                    ,135.4993578
//                    ,135.4993597
//                    ,135.4993616
//                    ,135.4993685
//                    ,135.4993704
//                    ,135.4993723
//                    ,135.4993741
//                    ,135.499376
//                    ,135.4993779
//                    ,135.4993797
//                    ,135.4993846
//                    ,135.4994197
//                    ,135.499397
//                    ,135.4994032
//                    ,135.4994094
//                    ,135.4994156
//                    ,135.4994219
//                    ,135.4994281
//                    ,135.4994343
//                    ,135.4994405
//                    ,135.4994467
//                    ,135.499452
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4995193
//                    ,135.4995268
//                    ,135.4995344
//                    ,135.499542
//                    ,135.4995496
//                    ,135.4995572
//                    ,135.4995648
//                    ,135.4995724
//                    ,135.49958
//                    ,135.4995875
//                    ,135.4995951
//                    ,135.4996027
//                    ,135.4996103
//                    ,135.4996179
//                    ,135.4996255
//                    ,135.4996331
//                    ,135.4996407
//                    ,135.4996788
//                    ,135.4996862
//                    ,135.4996628
//                    ,135.4996702
//                    ,135.4996775
//                    ,135.4997158
//                    ,135.4996926
//                    ,135.4997001
//                    ,135.4997076
//                    ,135.4997151
//                    ,135.4997226
//                    ,135.4997301
//                    ,135.4997376
//                    ,135.4997451
//                    ,135.4997527
//                    ,135.4997602
//                    ,135.4997677
//                    ,135.4997752
//                    ,135.4997827
//                    ,135.4997902
//                    ,135.4997977
//                    ,135.4998053
//                    ,135.4998128
//                    ,135.4998204
//                    ,135.4998279
//                    ,135.4998355
//                    ,135.4998431
//                    ,135.4998679
//                    ,135.4998583
//                    ,135.499866
//                    ,135.4998679
//                    ,135.4998679
//                    ,135.4998886
//                    ,135.4998962
//                    ,135.4999385
//                    ,135.4999462
//                    ,135.4999538
//                    ,135.4999268
//                    ,135.4999344
//                    ,135.4999421
//                    ,135.4999497
//                    ,135.4999574
//                    ,135.4999651
//                    ,135.4999727
//                    ,135.4999804
//                    ,135.499988
//                    ,135.4999957
//                    ,135.5000378
//
//            };
//
//            double[] mmLat = {
//                    34.70163724
//                    ,34.70164337
//                    ,34.7016495
//                    ,34.70165563
//                    ,34.70166176
//                    ,34.70166789
//                    ,34.70167402
//                    ,34.70168015
//                    ,34.70168629
//                    ,34.70169242
//                    ,34.70169855
//                    ,34.70170468
//                    ,34.70171081
//                    ,34.70171694
//                    ,34.70172307
//                    ,34.7017292
//                    ,34.70173533
//                    ,34.70174146
//                    ,34.7017476
//                    ,34.70175373
//                    ,34.70175986
//                    ,34.7017661
//                    ,34.70177213
//                    ,34.70177793
//                    ,34.70178361
//                    ,34.70178926
//                    ,34.70179464
//                    ,34.7018
//                    ,34.70180517
//                    ,34.70181031
//                    ,34.70181527
//                    ,34.70182026
//                    ,34.70182509
//                    ,34.70183087
//                    ,34.70183667
//                    ,34.70184248
//                    ,34.70184828
//                    ,34.70185408
//                    ,34.70185988
//                    ,34.70186568
//                    ,34.70187148
//                    ,34.70187728
//                    ,34.70188308
//                    ,34.70188889
//                    ,34.70189469
//                    ,34.70190049
//                    ,34.70190629
//                    ,34.70191061
//                    ,34.70191496
//                    ,34.70191919
//                    ,34.70192343
//                    ,34.70192767
//                    ,34.7019319
//                    ,34.70193614
//                    ,34.70194038
//                    ,34.70194462
//                    ,34.70194885
//                    ,34.70195309
//                    ,34.7019558
//                    ,34.70195847
//                    ,34.70196063
//                    ,34.70196214
//                    ,34.70196237
//                    ,34.7019626
//                    ,34.70196271
//                    ,34.70196298
//                    ,34.70196325
//                    ,34.70196352
//                    ,34.70196379
//                    ,34.70196406
//                    ,34.70196432
//                    ,34.70196459
//                    ,34.70196486
//                    ,34.70196513
//                    ,34.7019654
//                    ,34.70196567
//                    ,34.70196593
//                    ,34.7019662
//                    ,34.70196647
//                    ,34.70196674
//                    ,34.70196701
//                    ,34.70196728
//                    ,34.70196755
//                    ,34.70196684
//                    ,34.70196611
//                    ,34.70196538
//                    ,34.70196465
//                    ,34.70196392
//                    ,34.70196371
//                    ,34.70196349
//                    ,34.70196328
//                    ,34.70196307
//                    ,34.70196285
//                    ,34.70196264
//                    ,34.70196243
//                    ,34.70196221
//                    ,34.701962
//                    ,34.70196179
//                    ,34.70196157
//                    ,34.70196136
//                    ,34.70196115
//                    ,34.70196093
//                    ,34.70196072
//                    ,34.70196051
//                    ,34.70196029
//                    ,34.70196008
//                    ,34.70195987
//                    ,34.70195965
//                    ,34.70195944
//                    ,34.70195923
//                    ,34.70195901
//                    ,34.7019588
//                    ,34.70195859
//                    ,34.70195837
//                    ,34.70195816
//                    ,34.70195795
//                    ,34.70195773
//                    ,34.70195756
//                    ,34.70195716
//                    ,34.70195715
//                    ,34.70195713
//                    ,34.70195712
//                    ,34.70195711
//                    ,34.70195709
//                    ,34.70195708
//                    ,34.70195707
//                    ,34.70195705
//                    ,34.70195704
//                    ,34.70195703
//                    ,34.70195702
//                    ,34.70195558
//            };
//            double[] mmLng = {
//                    135.4993405
//                    ,135.4993388
//                    ,135.4993371
//                    ,135.4993354
//                    ,135.4993337
//                    ,135.499332
//                    ,135.4993303
//                    ,135.4993286
//                    ,135.4993269
//                    ,135.4993252
//                    ,135.4993235
//                    ,135.4993218
//                    ,135.4993201
//                    ,135.4993184
//                    ,135.4993167
//                    ,135.499315
//                    ,135.4993133
//                    ,135.4993116
//                    ,135.4993099
//                    ,135.4993082
//                    ,135.4993065
//                    ,135.4993072
//                    ,135.4993093
//                    ,135.4993122
//                    ,135.4993155
//                    ,135.4993189
//                    ,135.4993228
//                    ,135.4993268
//                    ,135.4993312
//                    ,135.4993356
//                    ,135.4993403
//                    ,135.4993449
//                    ,135.4993501
//                    ,135.499353
//                    ,135.499356
//                    ,135.499359
//                    ,135.4993619
//                    ,135.4993649
//                    ,135.4993678
//                    ,135.4993708
//                    ,135.4993737
//                    ,135.4993767
//                    ,135.4993796
//                    ,135.4993826
//                    ,135.4993856
//                    ,135.4993885
//                    ,135.4993915
//                    ,135.499397
//                    ,135.4994025
//                    ,135.4994079
//                    ,135.4994133
//                    ,135.4994187
//                    ,135.4994241
//                    ,135.4994296
//                    ,135.499435
//                    ,135.4994404
//                    ,135.4994458
//                    ,135.4994512
//                    ,135.4994579
//                    ,135.4994646
//                    ,135.4994716
//                    ,135.4994788
//                    ,135.4994863
//                    ,135.4994937
//                    ,135.4995012
//                    ,135.4995087
//                    ,135.4995161
//                    ,135.4995236
//                    ,135.4995311
//                    ,135.4995385
//                    ,135.499546
//                    ,135.4995535
//                    ,135.4995609
//                    ,135.4995684
//                    ,135.4995759
//                    ,135.4995834
//                    ,135.4995908
//                    ,135.4995983
//                    ,135.4996058
//                    ,135.4996132
//                    ,135.4996207
//                    ,135.4996282
//                    ,135.4996356
//                    ,135.499643
//                    ,135.4996505
//                    ,135.4996579
//                    ,135.4996653
//                    ,135.4996727
//                    ,135.4996802
//                    ,135.4996877
//                    ,135.4996951
//                    ,135.4997026
//                    ,135.4997101
//                    ,135.4997176
//                    ,135.499725
//                    ,135.4997325
//                    ,135.49974
//                    ,135.4997474
//                    ,135.4997549
//                    ,135.4997624
//                    ,135.4997698
//                    ,135.4997773
//                    ,135.4997848
//                    ,135.4997923
//                    ,135.4997997
//                    ,135.4998072
//                    ,135.4998147
//                    ,135.4998221
//                    ,135.4998296
//                    ,135.4998371
//                    ,135.4998446
//                    ,135.499852
//                    ,135.4998595
//                    ,135.499867
//                    ,135.4998744
//                    ,135.4998819
//                    ,135.4998894
//                    ,135.4998969
//                    ,135.4999043
//                    ,135.4999118
//                    ,135.4999193
//                    ,135.4999267
//                    ,135.4999342
//                    ,135.4999417
//                    ,135.4999492
//                    ,135.4999566
//                    ,135.4999641
//                    ,135.4999716
//                    ,135.4999791
//                    ,135.4999865
//                    ,135.4999938
//            };

            //rootB
//           double[] correctLat = {
//                    34.70196889,
//                    34.7019598,
//                    34.70198268,
//                    34.70193306,
//                    34.70207639,
//                    34.70225142,
//                    34.702427,
//                    34.70255187};
//            double[] correctLng = {
//                    135.5000848,
//                    135.4998877,
//                    135.4996925,
//                    135.4994977,
//                    135.4993743,
//                    135.4994028,
//                    135.4995202,
//                    135.4996794};
//
//            double[] rawPdrLat = {
//                    34.70196855
//                    ,34.7019682
//                    ,34.70196785
//                    ,34.7019675
//                    ,34.70196715
//                    ,34.7019668
//                    ,34.70196645
//                    ,34.7019661
//                    ,34.70196575
//                    ,34.7019654
//                    ,34.70196505
//                    ,34.7019647
//                    ,34.70196435
//                    ,34.70196399
//                    ,34.70196443
//                    ,34.70196547
//                    ,34.7019665
//                    ,34.70196754
//                    ,34.70196507
//                    ,34.70196272
//                    ,34.70196161
//                    ,34.70196123
//                    ,34.70196141
//                    ,34.70196259
//                    ,34.70196433
//                    ,34.70196619
//                    ,34.70196791
//                    ,34.70196963
//                    ,34.70197134
//                    ,34.70197306
//                    ,34.70197478
//                    ,34.70197649
//                    ,34.70197821
//                    ,34.70197993
//                    ,34.70198164
//                    ,34.70198336
//                    ,34.70198508
//                    ,34.70198679
//                    ,34.70198851
//                    ,34.70198857
//                    ,34.70198642
//                    ,34.7019836
//                    ,34.70198092
//                    ,34.70197894
//                    ,34.70197697
//                    ,34.70197499
//                    ,34.70197301
//                    ,34.70197104
//                    ,34.70196906
//                    ,34.70196708
//                    ,34.70196548
//                    ,34.70196412
//                    ,34.70196272
//                    ,34.70196152
//                    ,34.70196036
//                    ,34.7019592
//                    ,34.70195804
//                    ,34.70195688
//                    ,34.70195572
//                    ,34.70195456
//                    ,34.7019534
//                    ,34.70195223
//                    ,34.70195107
//                    ,34.70194991
//                    ,34.70194875
//                    ,34.70194759
//                    ,34.70194643
//                    ,34.70194527
//                    ,34.70194411
//                    ,34.70194317
//                    ,34.70194206
//                    ,34.70194095
//                    ,34.70193985
//                    ,34.70193874
//                    ,34.7019417
//                    ,34.70194581
//                    ,34.70195038
//                    ,34.70195528
//                    ,34.70196024
//                    ,34.70196554
//                    ,34.70197089
//                    ,34.70197626
//                    ,34.7019816
//                    ,34.70198694
//                    ,34.70199228
//                    ,34.70199762
//                    ,34.70200318
//                    ,34.70200888
//                    ,34.70201457
//                    ,34.70202027
//                    ,34.70202597
//                    ,34.70203167
//                    ,34.70203736
//                    ,34.70204306
//                    ,34.70204901
//                    ,34.70205494
//                    ,34.70206089
//                    ,34.70206702
//                    ,34.70207311
//                    ,34.70207923
//                    ,34.70208537
//                    ,34.70209164
//                    ,34.70209785
//                    ,34.70210398
//                    ,34.70211015
//                    ,34.70211628
//                    ,34.70212241
//                    ,34.70212854
//                    ,34.70213467
//                    ,34.70214081
//                    ,34.70214694
//                    ,34.70215307
//                    ,34.7021592
//                    ,34.70216533
//                    ,34.70217146
//                    ,34.70217759
//                    ,34.70218372
//                    ,34.70218985
//                    ,34.70219598
//                    ,34.70220211
//                    ,34.70220824
//                    ,34.70221437
//                    ,34.7022205
//                    ,34.70222663
//                    ,34.70223277
//                    ,34.7022389
//                    ,34.70224503
//                    ,34.70225116
//                    ,34.70225729
//                    ,34.70226342
//                    ,34.70226955
//                    ,34.70227548
//                    ,34.70228144
//                    ,34.70228741
//                    ,34.70229337
//                    ,34.70229933
//                    ,34.7023053
//                    ,34.70231126
//                    ,34.7023162
//                    ,34.70232099
//                    ,34.70232569
//                    ,34.70233038
//                    ,34.70233507
//                    ,34.70233976
//                    ,34.70234446
//                    ,34.70234915
//                    ,34.70235384
//                    ,34.70235809
//                    ,34.70236234
//                    ,34.70236659
//                    ,34.70237084
//                    ,34.7023751
//                    ,34.70237935
//                    ,34.7023836
//                    ,34.70238785
//                    ,34.7023921
//                    ,34.70239636
//                    ,34.70240061
//                    ,34.70240486
//                    ,34.70240911
//                    ,34.70241336
//                    ,34.70241762
//                    ,34.70242187
//                    ,34.70242612
//                    ,34.70243037
//                    ,34.70243462
//                    ,34.70243827
//                    ,34.70244146
//                    ,34.7024445
//                    ,34.70244743
//                    ,34.70245025
//                    ,34.70245306
//                    ,34.70245588
//                    ,34.7024587
//                    ,34.70246152
//                    ,34.70246434
//                    ,34.70246716
//                    ,34.70246998
//                    ,34.7024728
//                    ,34.70247562
//                    ,34.70247844
//                    ,34.70248126
//                    ,34.70248408
//                    ,34.7024869
//                    ,34.70248971
//                    ,34.70249253
//                    ,34.70249535
//                    ,34.70249817
//                    ,34.70250099
//                    ,34.70250381
//                    ,34.70250663
//                    ,34.70250945
//            };
//            double[] rawPdrLng = {
//                    135.5000774
//                    ,135.5000698
//                    ,135.5000621
//                    ,135.5000545
//                    ,135.5000468
//                    ,135.5000392
//                    ,135.5000315
//                    ,135.5000239
//                    ,135.5000162
//                    ,135.5000086
//                    ,135.5000009
//                    ,135.4999933
//                    ,135.4999856
//                    ,135.499978
//                    ,135.4999703
//                    ,135.4999628
//                    ,135.4999552
//                    ,135.4999477
//                    ,135.4999407
//                    ,135.4999336
//                    ,135.4999261
//                    ,135.4999184
//                    ,135.4999108
//                    ,135.4999033
//                    ,135.4998959
//                    ,135.4998886
//                    ,135.4998812
//                    ,135.4998739
//                    ,135.4998665
//                    ,135.4998591
//                    ,135.4998517
//                    ,135.4998444
//                    ,135.499837
//                    ,135.4998296
//                    ,135.4998223
//                    ,135.4998149
//                    ,135.4998075
//                    ,135.4998002
//                    ,135.4997928
//                    ,135.4997851
//                    ,135.499778
//                    ,135.4997711
//                    ,135.4997642
//                    ,135.499757
//                    ,135.4997497
//                    ,135.4997424
//                    ,135.4997351
//                    ,135.4997279
//                    ,135.4997206
//                    ,135.4997133
//                    ,135.4997059
//                    ,135.4996984
//                    ,135.499691
//                    ,135.4996834
//                    ,135.4996759
//                    ,135.4996684
//                    ,135.4996609
//                    ,135.4996533
//                    ,135.4996458
//                    ,135.4996383
//                    ,135.4996307
//                    ,135.4996232
//                    ,135.4996157
//                    ,135.4996082
//                    ,135.4996006
//                    ,135.4995931
//                    ,135.4995856
//                    ,135.499578
//                    ,135.4995705
//                    ,135.4995629
//                    ,135.4995554
//                    ,135.4995479
//                    ,135.4995403
//                    ,135.4995328
//                    ,135.4995261
//                    ,135.4995203
//                    ,135.4995151
//                    ,135.4995103
//                    ,135.4995056
//                    ,135.4995015
//                    ,135.4994974
//                    ,135.4994934
//                    ,135.4994894
//                    ,135.4994853
//                    ,135.4994813
//                    ,135.4994773
//                    ,135.4994737
//                    ,135.4994705
//                    ,135.4994672
//                    ,135.499464
//                    ,135.4994607
//                    ,135.4994575
//                    ,135.4994543
//                    ,135.499451
//                    ,135.4994485
//                    ,135.499446
//                    ,135.4994436
//                    ,135.4994418
//                    ,135.49944
//                    ,135.4994382
//                    ,135.4994366
//                    ,135.4994365
//                    ,135.4994377
//                    ,135.4994394
//                    ,135.4994408
//                    ,135.4994425
//                    ,135.4994442
//                    ,135.4994459
//                    ,135.4994476
//                    ,135.4994493
//                    ,135.499451
//                    ,135.4994527
//                    ,135.4994544
//                    ,135.4994561
//                    ,135.4994578
//                    ,135.4994595
//                    ,135.4994612
//                    ,135.499463
//                    ,135.4994647
//                    ,135.4994664
//                    ,135.4994681
//                    ,135.4994698
//                    ,135.4994715
//                    ,135.4994732
//                    ,135.4994749
//                    ,135.4994766
//                    ,135.4994783
//                    ,135.49948
//                    ,135.4994817
//                    ,135.4994834
//                    ,135.4994851
//                    ,135.4994876
//                    ,135.49949
//                    ,135.4994925
//                    ,135.4994949
//                    ,135.4994973
//                    ,135.4994997
//                    ,135.4995022
//                    ,135.4995069
//                    ,135.4995118
//                    ,135.4995169
//                    ,135.499522
//                    ,135.4995271
//                    ,135.4995322
//                    ,135.4995373
//                    ,135.4995424
//                    ,135.4995475
//                    ,135.4995532
//                    ,135.4995588
//                    ,135.4995644
//                    ,135.4995701
//                    ,135.4995757
//                    ,135.4995814
//                    ,135.499587
//                    ,135.4995927
//                    ,135.4995983
//                    ,135.499604
//                    ,135.4996096
//                    ,135.4996152
//                    ,135.4996209
//                    ,135.4996265
//                    ,135.4996322
//                    ,135.4996378
//                    ,135.4996435
//                    ,135.4996491
//                    ,135.4996548
//                    ,135.499661
//                    ,135.4996676
//                    ,135.4996743
//                    ,135.4996811
//                    ,135.4996879
//                    ,135.4996948
//                    ,135.4997016
//                    ,135.4997085
//                    ,135.4997153
//                    ,135.4997221
//                    ,135.499729
//                    ,135.4997358
//                    ,135.4997427
//                    ,135.4997495
//                    ,135.4997564
//                    ,135.4997632
//                    ,135.4997701
//                    ,135.4997769
//                    ,135.4997838
//                    ,135.4997906
//                    ,135.4997975
//                    ,135.4998043
//                    ,135.4998112
//                    ,135.499818
//                    ,135.4998249
//                    ,135.4998317
//            };
//
//            double[] smLat = {
//                    34.70198186
//                    ,34.70198158
//                    ,34.70198129
//                    ,34.70198101
//                    ,34.70198087
//                    ,34.70198074
//                    ,34.7019806
//                    ,34.70198047
//                    ,34.70198033
//                    ,34.7019802
//                    ,34.70198006
//                    ,34.70197993
//                    ,34.70197979
//                    ,34.70197966
//                    ,34.70197952
//                    ,34.70197939
//                    ,34.70197925
//                    ,34.70197912
//                    ,34.701979
//                    ,34.70197887
//                    ,34.70197874
//                    ,34.7019786
//                    ,34.70197847
//                    ,34.70197833
//                    ,34.7019782
//                    ,34.70197808
//                    ,34.70197795
//                    ,34.70197782
//                    ,34.70197761
//                    ,34.70197709
//                    ,34.70197657
//                    ,34.70197605
//                    ,34.70197553
//                    ,34.70197501
//                    ,34.70197449
//                    ,34.70197397
//                    ,34.70197341
//                    ,34.70197269
//                    ,34.70197196
//                    ,34.70197118
//                    ,34.70197043
//                    ,34.70196972
//                    ,34.70196899
//                    ,34.70196824
//                    ,34.7019675
//                    ,34.70196675
//                    ,34.701966
//                    ,34.70196525
//                    ,34.70196449
//                    ,34.70196374
//                    ,34.70196296
//                    ,34.7019622
//                    ,34.70196143
//                    ,34.70196066
//                    ,34.70195989
//                    ,34.70195914
//                    ,34.70195837
//                    ,34.7019576
//                    ,34.70195683
//                    ,34.70195606
//                    ,34.70195529
//                    ,34.70195452
//                    ,34.70195375
//                    ,34.70195298
//                    ,34.70195221
//                    ,34.70195144
//                    ,34.70195067
//                    ,34.7019499
//                    ,34.70194913
//                    ,34.70194834
//                    ,34.70194757
//                    ,34.70194682
//                    ,34.70194605
//                    ,34.70194528
//                    ,34.70194462
//                    ,34.70194408
//                    ,34.7019436
//                    ,34.70194316
//                    ,34.70194274
//                    ,34.70194238
//                    ,34.70194203
//                    ,34.70194169
//                    ,34.70194133
//                    ,34.7019801
//                    ,34.70198611
//                    ,34.70199212
//                    ,34.70199207
//                    ,34.70199819
//                    ,34.70201044
//                    ,34.70201656
//                    ,34.70202268
//                    ,34.7020288
//                    ,34.70203492
//                    ,34.70204104
//                    ,34.70204104
//                    ,34.70204718
//                    ,34.70205332
//                    ,34.70205942
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70206537
//                    ,34.70211947
//                    ,34.70212572
//                    ,34.70213196
//                    ,34.70213821
//                    ,34.70214446
//                    ,34.7021507
//                    ,34.70215695
//                    ,34.7021632
//                    ,34.70216944
//                    ,34.70217569
//                    ,34.70218194
//                    ,34.70218818
//                    ,34.70219443
//                    ,34.70220068
//                    ,34.70220692
//                    ,34.70221317
//                    ,34.70221942
//                    ,34.70222566
//                    ,34.70223191
//                    ,34.70223816
//                    ,34.7022444
//                    ,34.70225065
//                    ,34.7022569
//                    ,34.70226314
//                    ,34.70226939
//                    ,34.7022814
//                    ,34.70228758
//                    ,34.70228791
//                    ,34.70229408
//                    ,34.70230026
//                    ,34.70230644
//                    ,34.70231262
//                    ,34.70232395
//                    ,34.7023293
//                    ,34.7023346
//                    ,34.70233988
//                    ,34.70235633
//                    ,34.70236062
//                    ,34.70236491
//                    ,34.70236919
//                    ,34.70237348
//                    ,34.70240371
//                    ,34.70238224
//                    ,34.70238662
//                    ,34.70239101
//                    ,34.70239539
//                    ,34.70239977
//                    ,34.70240415
//                    ,34.70240854
//                    ,34.70241292
//                    ,34.7024173
//                    ,34.70242169
//                    ,34.70242607
//                    ,34.70243045
//                    ,34.70243483
//                    ,34.70243922
//                    ,34.7024436
//                    ,34.70244798
//                    ,34.70245236
//                    ,34.70245675
//                    ,34.70248703
//                    ,34.70249147
//                    ,34.7024959
//                    ,34.70250032
//                    ,34.70250474
//                    ,34.7024833
//                    ,34.70248772
//                    ,34.70249214
//                    ,34.70249656
//                    ,34.70250097
//                    ,34.70250539
//                    ,34.70250981
//                    ,34.70251422
//                    ,34.70251864
//                    ,34.70252306
//                    ,34.70252747
//                    ,34.70253189
//                    ,34.70253631
//                    ,34.70254073
//                    ,34.70254514
//                    ,34.70254956
//                    ,34.70255398
//                    ,34.70255839
//                    ,34.70256281
//                    ,34.70256723
//                    ,34.70257164
//
//            };
//            double[] smLng = {
//                    135.5000769
//                    ,135.5000695
//                    ,135.5000619
//                    ,135.5000542
//                    ,135.5000466
//                    ,135.5000389
//                    ,135.5000313
//                    ,135.5000236
//                    ,135.500016
//                    ,135.5000083
//                    ,135.5000007
//                    ,135.499993
//                    ,135.4999854
//                    ,135.4999777
//                    ,135.4999701
//                    ,135.4999626
//                    ,135.499955
//                    ,135.4999475
//                    ,135.4999404
//                    ,135.4999333
//                    ,135.4999258
//                    ,135.4999181
//                    ,135.4999105
//                    ,135.499903
//                    ,135.4998957
//                    ,135.4998884
//                    ,135.4998811
//                    ,135.4998737
//                    ,135.4998665
//                    ,135.4998593
//                    ,135.4998521
//                    ,135.4998449
//                    ,135.4998376
//                    ,135.4998304
//                    ,135.4998232
//                    ,135.499816
//                    ,135.4998089
//                    ,135.4998018
//                    ,135.4997946
//                    ,135.4997869
//                    ,135.4997796
//                    ,135.4997726
//                    ,135.4997654
//                    ,135.499758
//                    ,135.4997508
//                    ,135.4997434
//                    ,135.499736
//                    ,135.4997286
//                    ,135.4997212
//                    ,135.4997138
//                    ,135.4997062
//                    ,135.4996986
//                    ,135.4996911
//                    ,135.4996835
//                    ,135.499676
//                    ,135.4996686
//                    ,135.499661
//                    ,135.4996534
//                    ,135.4996459
//                    ,135.4996383
//                    ,135.4996307
//                    ,135.4996231
//                    ,135.4996156
//                    ,135.499608
//                    ,135.4996004
//                    ,135.4995929
//                    ,135.4995853
//                    ,135.4995777
//                    ,135.4995702
//                    ,135.4995624
//                    ,135.4995548
//                    ,135.4995474
//                    ,135.4995398
//                    ,135.4995323
//                    ,135.4995258
//                    ,135.4995205
//                    ,135.4995157
//                    ,135.4995115
//                    ,135.4995073
//                    ,135.4995038
//                    ,135.4995004
//                    ,135.499497
//                    ,135.4994935
//                    ,135.49944
//                    ,135.4994382
//                    ,135.4994364
//                    ,135.4994364
//                    ,135.4994346
//                    ,135.499431
//                    ,135.4994291
//                    ,135.4994273
//                    ,135.4994255
//                    ,135.4994237
//                    ,135.4994218
//                    ,135.4994218
//                    ,135.49942
//                    ,135.4994182
//                    ,135.4994164
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994146
//                    ,135.4994219
//                    ,135.4994227
//                    ,135.4994236
//                    ,135.4994244
//                    ,135.4994253
//                    ,135.4994261
//                    ,135.4994269
//                    ,135.4994278
//                    ,135.4994286
//                    ,135.4994295
//                    ,135.4994303
//                    ,135.4994312
//                    ,135.499432
//                    ,135.4994328
//                    ,135.4994337
//                    ,135.4994345
//                    ,135.4994354
//                    ,135.4994362
//                    ,135.4994371
//                    ,135.4994379
//                    ,135.4994388
//                    ,135.4994396
//                    ,135.4994404
//                    ,135.4994413
//                    ,135.4994421
//                    ,135.4994437
//                    ,135.4994446
//                    ,135.4994446
//                    ,135.4994455
//                    ,135.4994463
//                    ,135.4994471
//                    ,135.499448
//                    ,135.4994495
//                    ,135.4994502
//                    ,135.4994509
//                    ,135.4994516
//                    ,135.4994572
//                    ,135.4994626
//                    ,135.499468
//                    ,135.4994734
//                    ,135.4994788
//                    ,135.4995171
//                    ,135.4994899
//                    ,135.4994955
//                    ,135.499501
//                    ,135.4995065
//                    ,135.4995121
//                    ,135.4995176
//                    ,135.4995232
//                    ,135.4995287
//                    ,135.4995343
//                    ,135.4995398
//                    ,135.4995453
//                    ,135.4995509
//                    ,135.4995564
//                    ,135.499562
//                    ,135.4995675
//                    ,135.499573
//                    ,135.4995786
//                    ,135.4995841
//                    ,135.4996224
//                    ,135.499628
//                    ,135.4996336
//                    ,135.4996392
//                    ,135.4996448
//                    ,135.4996177
//                    ,135.4996233
//                    ,135.4996289
//                    ,135.4996345
//                    ,135.49964
//                    ,135.4996456
//                    ,135.4996512
//                    ,135.4996568
//                    ,135.4996624
//                    ,135.499668
//                    ,135.4996735
//                    ,135.4996791
//                    ,135.4996847
//                    ,135.4996903
//                    ,135.4996959
//                    ,135.4997015
//                    ,135.4997071
//                    ,135.4997126
//                    ,135.4997182
//                    ,135.4997238
//                    ,135.4997294
//
//            };
//
//            double[] mmLat = {
//                    34.70196855
//                    ,34.70196818
//                    ,34.7019678
//                    ,34.70196743
//                    ,34.70196705
//                    ,34.70196667
//                    ,34.7019663
//                    ,34.70196592
//                    ,34.70196555
//                    ,34.70196517
//                    ,34.7019648
//                    ,34.70196442
//                    ,34.70196405
//                    ,34.70196367
//                    ,34.70196414
//                    ,34.70196524
//                    ,34.70196634
//                    ,34.70196744
//                    ,34.70196481
//                    ,34.7019623
//                    ,34.70196112
//                    ,34.70196071
//                    ,34.7019609
//                    ,34.70196215
//                    ,34.70196401
//                    ,34.70196599
//                    ,34.70196782
//                    ,34.70196964
//                    ,34.70197147
//                    ,34.7019733
//                    ,34.70197512
//                    ,34.70197695
//                    ,34.70197878
//                    ,34.7019806
//                    ,34.70198243
//                    ,34.70198426
//                    ,34.70198608
//                    ,34.70198791
//                    ,34.70198974
//                    ,34.7019898
//                    ,34.70198751
//                    ,34.7019845
//                    ,34.70198164
//                    ,34.70197953
//                    ,34.70197743
//                    ,34.70197532
//                    ,34.70197321
//                    ,34.7019711
//                    ,34.701969
//                    ,34.70196689
//                    ,34.70196517
//                    ,34.70196373
//                    ,34.70196224
//                    ,34.70196096
//                    ,34.70195972
//                    ,34.70195848
//                    ,34.70195724
//                    ,34.701956
//                    ,34.70195476
//                    ,34.70195352
//                    ,34.70195229
//                    ,34.70195105
//                    ,34.70194981
//                    ,34.70194857
//                    ,34.70194733
//                    ,34.70194609
//                    ,34.70194485
//                    ,34.70194361
//                    ,34.70194237
//                    ,34.70194138
//                    ,34.70194019
//                    ,34.70193901
//                    ,34.70193783
//                    ,34.70193664
//                    ,34.7019398
//                    ,34.70194418
//                    ,34.70194905
//                    ,34.70195428
//                    ,34.70195957
//                    ,34.70196523
//                    ,34.70197094
//                    ,34.70197666
//                    ,34.70198236
//                    ,34.70198806
//                    ,34.70199355
//                    ,34.70199904
//                    ,34.70200452
//                    ,34.70201001
//                    ,34.7020155
//                    ,34.70202099
//                    ,34.70202648
//                    ,34.70203197
//                    ,34.70203746
//                    ,34.70204294
//                    ,34.70204843
//                    ,34.70205392
//                    ,34.70205941
//                    ,34.7020649
//                    ,34.70207039
//                    ,34.70207587
//                    ,34.70208215
//                    ,34.70208862
//                    ,34.70209505
//                    ,34.70210144
//                    ,34.70210786
//                    ,34.70211424
//                    ,34.70212063
//                    ,34.70212702
//                    ,34.7021334
//                    ,34.70213979
//                    ,34.70214618
//                    ,34.70215256
//                    ,34.70215895
//                    ,34.70216534
//                    ,34.70217173
//                    ,34.70217811
//                    ,34.7021845
//                    ,34.70219089
//                    ,34.70219727
//                    ,34.70220366
//                    ,34.70221005
//                    ,34.70221643
//                    ,34.70222282
//                    ,34.70222921
//                    ,34.70223559
//                    ,34.70224198
//                    ,34.70224837
//                    ,34.70225475
//                    ,34.70226114
//                    ,34.70226753
//                    ,34.70227391
//                    ,34.70228014
//                    ,34.70228639
//                    ,34.70229265
//                    ,34.7022989
//                    ,34.70230516
//                    ,34.70231142
//                    ,34.70231767
//                    ,34.70232305
//                    ,34.70232829
//                    ,34.70233346
//                    ,34.70233863
//                    ,34.70234379
//                    ,34.70234896
//                    ,34.70235412
//                    ,34.70235928
//                    ,34.70236445
//                    ,34.70236922
//                    ,34.70237399
//                    ,34.70237876
//                    ,34.70238353
//                    ,34.70238831
//                    ,34.70239308
//                    ,34.70239785
//                    ,34.70240263
//                    ,34.7024074
//                    ,34.70241217
//                    ,34.70241694
//                    ,34.70242172
//                    ,34.70242649
//                    ,34.70243126
//                    ,34.70243604
//                    ,34.70244081
//                    ,34.70244558
//                    ,34.70245035
//                    ,34.70245513
//                    ,34.70245935
//                    ,34.70246317
//                    ,34.70246684
//                    ,34.70247042
//                    ,34.70247389
//                    ,34.70247737
//                    ,34.70248084
//                    ,34.70248432
//                    ,34.70248779
//                    ,34.70249127
//                    ,34.70249474
//                    ,34.70249822
//                    ,34.70250169
//                    ,34.70250517
//                    ,34.70250864
//                    ,34.70251212
//                    ,34.70251559
//                    ,34.70251907
//                    ,34.70252254
//                    ,34.70252602
//                    ,34.70252949
//                    ,34.70253297
//                    ,34.70253644
//                    ,34.70253992
//                    ,34.70254339
//                    ,34.70254687
//            };
//            double[] mmLng = {
//                    135.5000774
//                    ,135.5000692
//                    ,135.500061
//                    ,135.5000528
//                    ,135.5000447
//                    ,135.5000365
//                    ,135.5000283
//                    ,135.5000201
//                    ,135.5000119
//                    ,135.5000037
//                    ,135.4999955
//                    ,135.4999873
//                    ,135.4999791
//                    ,135.4999709
//                    ,135.4999627
//                    ,135.4999547
//                    ,135.4999466
//                    ,135.4999385
//                    ,135.499931
//                    ,135.4999234
//                    ,135.4999153
//                    ,135.4999071
//                    ,135.4998989
//                    ,135.4998909
//                    ,135.499883
//                    ,135.4998752
//                    ,135.4998673
//                    ,135.4998594
//                    ,135.4998515
//                    ,135.4998436
//                    ,135.4998357
//                    ,135.4998278
//                    ,135.4998199
//                    ,135.499812
//                    ,135.4998041
//                    ,135.4997962
//                    ,135.4997883
//                    ,135.4997804
//                    ,135.4997725
//                    ,135.4997643
//                    ,135.4997566
//                    ,135.4997493
//                    ,135.4997419
//                    ,135.4997341
//                    ,135.4997263
//                    ,135.4997185
//                    ,135.4997107
//                    ,135.4997029
//                    ,135.4996951
//                    ,135.4996873
//                    ,135.4996794
//                    ,135.4996714
//                    ,135.4996634
//                    ,135.4996553
//                    ,135.4996473
//                    ,135.4996392
//                    ,135.4996312
//                    ,135.4996231
//                    ,135.499615
//                    ,135.499607
//                    ,135.4995989
//                    ,135.4995908
//                    ,135.4995828
//                    ,135.4995747
//                    ,135.4995666
//                    ,135.4995586
//                    ,135.4995505
//                    ,135.4995424
//                    ,135.4995344
//                    ,135.4995263
//                    ,135.4995182
//                    ,135.4995101
//                    ,135.499502
//                    ,135.499494
//                    ,135.4994868
//                    ,135.4994806
//                    ,135.4994749
//                    ,135.4994698
//                    ,135.4994647
//                    ,135.4994602
//                    ,135.4994559
//                    ,135.4994516
//                    ,135.4994472
//                    ,135.4994428
//                    ,135.4994386
//                    ,135.4994344
//                    ,135.4994302
//                    ,135.499426
//                    ,135.4994218
//                    ,135.4994176
//                    ,135.4994134
//                    ,135.4994092
//                    ,135.4994049
//                    ,135.4994007
//                    ,135.4993965
//                    ,135.4993923
//                    ,135.4993881
//                    ,135.4993839
//                    ,135.4993797
//                    ,135.4993755
//                    ,135.4993736
//                    ,135.4993733
//                    ,135.4993742
//                    ,135.4993755
//                    ,135.4993766
//                    ,135.499378
//                    ,135.4993794
//                    ,135.4993807
//                    ,135.4993821
//                    ,135.4993835
//                    ,135.4993849
//                    ,135.4993862
//                    ,135.4993876
//                    ,135.499389
//                    ,135.4993904
//                    ,135.4993917
//                    ,135.4993931
//                    ,135.4993945
//                    ,135.4993958
//                    ,135.4993972
//                    ,135.4993986
//                    ,135.4994
//                    ,135.4994013
//                    ,135.4994027
//                    ,135.4994041
//                    ,135.4994055
//                    ,135.4994068
//                    ,135.4994082
//                    ,135.4994096
//                    ,135.499411
//                    ,135.4994123
//                    ,135.4994145
//                    ,135.4994166
//                    ,135.4994187
//                    ,135.4994208
//                    ,135.4994229
//                    ,135.499425
//                    ,135.499427
//                    ,135.4994314
//                    ,135.4994361
//                    ,135.4994408
//                    ,135.4994456
//                    ,135.4994504
//                    ,135.4994552
//                    ,135.4994599
//                    ,135.4994647
//                    ,135.4994695
//                    ,135.4994748
//                    ,135.4994802
//                    ,135.4994855
//                    ,135.4994909
//                    ,135.4994962
//                    ,135.4995016
//                    ,135.4995069
//                    ,135.4995123
//                    ,135.4995176
//                    ,135.499523
//                    ,135.4995284
//                    ,135.4995337
//                    ,135.4995391
//                    ,135.4995444
//                    ,135.4995498
//                    ,135.4995551
//                    ,135.4995605
//                    ,135.4995658
//                    ,135.4995712
//                    ,135.4995771
//                    ,135.4995835
//                    ,135.49959
//                    ,135.4995966
//                    ,135.4996033
//                    ,135.49961
//                    ,135.4996166
//                    ,135.4996233
//                    ,135.49963
//                    ,135.4996366
//                    ,135.4996433
//                    ,135.49965
//                    ,135.4996567
//                    ,135.4996633
//                    ,135.49967
//                    ,135.4996767
//                    ,135.4996833
//                    ,135.49969
//                    ,135.4996967
//                    ,135.4997034
//                    ,135.49971
//                    ,135.4997167
//                    ,135.4997234
//                    ,135.4997301
//                    ,135.4997367
//                    ,135.4997434
//            };

            //rootC
//            double[] correctLat = {
//            34.70263318,
//            34.70253037,
//            34.70243472,
//            34.70240964,
//            34.70244216,
//            34.70261389,
//            34.70277431,
//            34.70293996};
//            double[] correctLng = {
//            135.4998367,
//            135.4997033,
//            135.4995634,
//            135.4994917,
//            135.4994471,
//            135.4994391,
//            135.4994649,
//            135.4995034};
//
//            double[] rawPdrLat = {
//                    34.70262937
//                    ,34.70262539
//                    ,34.7026214
//                    ,34.70261742
//                    ,34.70261344
//                    ,34.70260945
//                    ,34.70260547
//                    ,34.70260149
//                    ,34.7025975
//                    ,34.70259456
//                    ,34.7025916
//                    ,34.70258865
//                    ,34.7025857
//                    ,34.70258214
//                    ,34.70257859
//                    ,34.70257503
//                    ,34.70257147
//                    ,34.70256791
//                    ,34.70256435
//                    ,34.70256079
//                    ,34.70255723
//                    ,34.70255367
//                    ,34.70255012
//                    ,34.70254606
//                    ,34.70254179
//                    ,34.70253756
//                    ,34.70253332
//                    ,34.70252908
//                    ,34.70252485
//                    ,34.70252061
//                    ,34.70251637
//                    ,34.70251213
//                    ,34.7025079
//                    ,34.70250366
//                    ,34.70249942
//                    ,34.70249519
//                    ,34.70249095
//                    ,34.70248671
//                    ,34.70248247
//                    ,34.70247824
//                    ,34.702474
//                    ,34.70246976
//                    ,34.70246553
//                    ,34.70246129
//                    ,34.70245705
//                    ,34.70245281
//                    ,34.70244858
//                    ,34.70244623
//                    ,34.70244439
//                    ,34.7024436
//                    ,34.70244328
//                    ,34.70244371
//                    ,34.70244372
//                    ,34.70244374
//                    ,34.70244406
//                    ,34.70244574
//                    ,34.70244812
//                    ,34.70245116
//                    ,34.70245454
//                    ,34.70245853
//                    ,34.70246262
//                    ,34.70246736
//                    ,34.70247244
//                    ,34.70247801
//                    ,34.70248378
//                    ,34.7024896
//                    ,34.70249544
//                    ,34.70250127
//                    ,34.70250711
//                    ,34.70251294
//                    ,34.70251878
//                    ,34.70252462
//                    ,34.70253046
//                    ,34.70253629
//                    ,34.70254213
//                    ,34.70254797
//                    ,34.70255381
//                    ,34.70255965
//                    ,34.70256548
//                    ,34.70257132
//                    ,34.70257716
//                    ,34.702583
//                    ,34.70258883
//                    ,34.70259462
//                    ,34.70260042
//                    ,34.70260617
//                    ,34.70261191
//                    ,34.70261762
//                    ,34.70262333
//                    ,34.70262904
//                    ,34.70263475
//                    ,34.70264046
//                    ,34.70264617
//                    ,34.70265188
//                    ,34.70265759
//                    ,34.7026633
//                    ,34.70266901
//                    ,34.70267472
//                    ,34.70268043
//                    ,34.70268614
//                    ,34.70269185
//                    ,34.70269756
//                    ,34.70270327
//                    ,34.70270898
//                    ,34.70271469
//                    ,34.7027204
//                    ,34.70272611
//                    ,34.70273182
//                    ,34.70273753
//                    ,34.70274324
//                    ,34.70274894
//                    ,34.70275465
//                    ,34.70276036
//                    ,34.70276607
//                    ,34.70277178
//                    ,34.70277749
//                    ,34.7027832
//                    ,34.70278891
//                    ,34.70279462
//                    ,34.70280033
//                    ,34.70280604
//                    ,34.70281175
//                    ,34.70281746
//                    ,34.70282317
//                    ,34.70282888
//                    ,34.70283459
//                    ,34.7028403
//                    ,34.70284601
//                    ,34.70285172
//                    ,34.70285743
//                    ,34.70286314
//                    ,34.70286885
//                    ,34.70287456
//                    ,34.70288027
//                    ,34.70288598
//                    ,34.70289169
//                    ,34.7028974
//                    ,34.70290311
//                    ,34.70290882
//                    ,34.70291453
//                    ,34.70292024
//                    ,34.70292595
//                    ,34.70293166
//                    ,34.70293737
//            };
//            double[] rawPdrLng = {
//                    135.4998317
//                    ,135.4998265
//                    ,135.4998213
//                    ,135.4998161
//                    ,135.4998109
//                    ,135.4998057
//                    ,135.4998005
//                    ,135.4997953
//                    ,135.4997901
//                    ,135.499784
//                    ,135.4997779
//                    ,135.4997717
//                    ,135.4997656
//                    ,135.4997599
//                    ,135.4997543
//                    ,135.4997487
//                    ,135.499743
//                    ,135.4997374
//                    ,135.4997317
//                    ,135.4997261
//                    ,135.4997205
//                    ,135.4997148
//                    ,135.4997092
//                    ,135.4997041
//                    ,135.4996992
//                    ,135.4996943
//                    ,135.4996894
//                    ,135.4996845
//                    ,135.4996797
//                    ,135.4996748
//                    ,135.4996699
//                    ,135.499665
//                    ,135.4996601
//                    ,135.4996552
//                    ,135.4996503
//                    ,135.4996454
//                    ,135.4996405
//                    ,135.4996356
//                    ,135.4996307
//                    ,135.4996258
//                    ,135.4996209
//                    ,135.499616
//                    ,135.4996111
//                    ,135.4996062
//                    ,135.4996013
//                    ,135.4995964
//                    ,135.4995915
//                    ,135.4995851
//                    ,135.4995783
//                    ,135.4995713
//                    ,135.4995642
//                    ,135.4995571
//                    ,135.49955
//                    ,135.4995429
//                    ,135.4995358
//                    ,135.499529
//                    ,135.4995225
//                    ,135.4995165
//                    ,135.4995107
//                    ,135.4995055
//                    ,135.4995004
//                    ,135.4994963
//                    ,135.4994928
//                    ,135.4994907
//                    ,135.4994896
//                    ,135.4994896
//                    ,135.4994896
//                    ,135.4994899
//                    ,135.4994898
//                    ,135.4994896
//                    ,135.4994895
//                    ,135.4994894
//                    ,135.4994892
//                    ,135.4994891
//                    ,135.4994889
//                    ,135.4994888
//                    ,135.4994886
//                    ,135.4994885
//                    ,135.4994884
//                    ,135.4994882
//                    ,135.4994881
//                    ,135.4994879
//                    ,135.4994878
//                    ,135.4994886
//                    ,135.4994894
//                    ,135.4994907
//                    ,135.499492
//                    ,135.4994934
//                    ,135.4994949
//                    ,135.4994964
//                    ,135.4994979
//                    ,135.4994994
//                    ,135.4995009
//                    ,135.4995024
//                    ,135.4995039
//                    ,135.4995053
//                    ,135.4995068
//                    ,135.4995083
//                    ,135.4995098
//                    ,135.4995113
//                    ,135.4995128
//                    ,135.4995143
//                    ,135.4995158
//                    ,135.4995172
//                    ,135.4995187
//                    ,135.4995202
//                    ,135.4995217
//                    ,135.4995232
//                    ,135.4995247
//                    ,135.4995262
//                    ,135.4995277
//                    ,135.4995291
//                    ,135.4995306
//                    ,135.4995321
//                    ,135.4995336
//                    ,135.4995351
//                    ,135.4995366
//                    ,135.4995381
//                    ,135.4995396
//                    ,135.499541
//                    ,135.4995425
//                    ,135.499544
//                    ,135.4995455
//                    ,135.499547
//                    ,135.4995485
//                    ,135.49955
//                    ,135.4995515
//                    ,135.4995529
//                    ,135.4995544
//                    ,135.4995559
//                    ,135.4995574
//                    ,135.4995589
//                    ,135.4995604
//                    ,135.4995619
//                    ,135.4995634
//                    ,135.4995648
//                    ,135.4995663
//                    ,135.4995678
//                    ,135.4995693
//                    ,135.4995708
//                    ,135.4995723
//                    ,135.4995738
//                    ,135.4995752
//                    ,135.4995767
//            };
//
//            double[] smLat = {
//                    34.70264275
//                    ,34.70263898
//                    ,34.70263504
//                    ,34.70263109
//                    ,34.70262715
//                    ,34.70262321
//                    ,34.70261926
//                    ,34.70261532
//                    ,34.70261137
//                    ,34.70260719
//                    ,34.70260333
//                    ,34.70259921
//                    ,34.70259509
//                    ,34.70259055
//                    ,34.70258686
//                    ,34.70258275
//                    ,34.70257864
//                    ,34.70257452
//                    ,34.70257041
//                    ,34.70256629
//                    ,34.70256218
//                    ,34.70255807
//                    ,34.70255395
//                    ,34.70254948
//                    ,34.70254548
//                    ,34.70254147
//                    ,34.70253788
//                    ,34.70253387
//                    ,34.70252986
//                    ,34.70252585
//                    ,34.70252183
//                    ,34.70251782
//                    ,34.70251381
//                    ,34.7025098
//                    ,34.70250578
//                    ,34.70250177
//                    ,34.70249776
//                    ,34.70249375
//                    ,34.70248974
//                    ,34.70248572
//                    ,34.70248171
//                    ,34.7024777
//                    ,34.70247369
//                    ,34.70246968
//                    ,34.70246566
//                    ,34.70246165
//                    ,34.70245764
//                    ,34.70245316
//                    ,34.70244917
//                    ,34.70244544
//                    ,34.70244187
//                    ,34.70243859
//                    ,34.70243513
//                    ,34.70243168
//                    ,34.70242835
//                    ,34.70242569
//                    ,34.70242345
//                    ,34.70242167
//                    ,34.70242015
//                    ,34.70241916
//                    ,34.70241827
//                    ,34.70241808
//                    ,34.70241834
//                    ,34.70241949
//                    ,34.70242118
//                    ,34.70242342
//                    ,34.70242565
//                    ,34.70242805
//                    ,34.70243022
//                    ,34.7025219
//                    ,34.70252774
//                    ,34.70253358
//                    ,34.70253942
//                    ,34.70254526
//                    ,34.70255109
//                    ,34.70255693
//                    ,34.70256277
//                    ,34.70256861
//                    ,34.70257445
//                    ,34.70258022
//                    ,34.70258579
//                    ,34.70259135
//                    ,34.70259692
//                    ,34.70260116
//                    ,34.70260688
//                    ,34.70261263
//                    ,34.70261839
//                    ,34.70262416
//                    ,34.7026314
//                    ,34.70263717
//                    ,34.70264294
//                    ,34.70264871
//                    ,34.70265448
//                    ,34.70266025
//                    ,34.70266602
//                    ,34.70267178
//                    ,34.70267755
//                    ,34.70268332
//                    ,34.70268909
//                    ,34.70269486
//                    ,34.70270063
//                    ,34.7027064
//                    ,34.70271217
//                    ,34.70271794
//                    ,34.7027237
//                    ,34.70272947
//                    ,34.70273524
//                    ,34.70274101
//                    ,34.70274678
//                    ,34.70275255
//                    ,34.70275832
//                    ,34.70276409
//                    ,34.70276986
//                    ,34.70277562
//                    ,34.70278139
//                    ,34.70278716
//                    ,34.70279293
//                    ,34.7027987
//                    ,34.70280448
//                    ,34.70281028
//                    ,34.70281608
//                    ,34.70282187
//                    ,34.70282767
//                    ,34.70283347
//                    ,34.70283927
//                    ,34.70284506
//                    ,34.70285086
//                    ,34.70285666
//                    ,34.70286246
//                    ,34.70286825
//                    ,34.70287405
//                    ,34.70287985
//                    ,34.70288565
//                    ,34.70289144
//                    ,34.70289724
//                    ,34.70290304
//                    ,34.70290884
//                    ,34.70291463
//                    ,34.70292043
//                    ,34.70292623
//                    ,34.70293203
//                    ,34.70293782
//                    ,34.70294362
//                    ,34.70294942
//
//            };
//            double[] smLng = {
//                    135.4998216
//                    ,135.4998166
//                    ,135.4998114
//                    ,135.4998062
//                    ,135.4998009
//                    ,135.4997957
//                    ,135.4997905
//                    ,135.4997852
//                    ,135.49978
//                    ,135.4997745
//                    ,135.4997695
//                    ,135.4997642
//                    ,135.499759
//                    ,135.4997533
//                    ,135.4997486
//                    ,135.4997434
//                    ,135.4997382
//                    ,135.499733
//                    ,135.4997278
//                    ,135.4997226
//                    ,135.4997174
//                    ,135.4997122
//                    ,135.499707
//                    ,135.4997014
//                    ,135.4996963
//                    ,135.4996912
//                    ,135.4996867
//                    ,135.4996816
//                    ,135.4996766
//                    ,135.4996715
//                    ,135.4996664
//                    ,135.4996613
//                    ,135.4996563
//                    ,135.4996512
//                    ,135.4996461
//                    ,135.4996411
//                    ,135.499636
//                    ,135.4996309
//                    ,135.4996258
//                    ,135.4996208
//                    ,135.4996157
//                    ,135.4996106
//                    ,135.4996055
//                    ,135.4996005
//                    ,135.4995954
//                    ,135.4995903
//                    ,135.4995853
//                    ,135.4995796
//                    ,135.4995745
//                    ,135.4995698
//                    ,135.4995653
//                    ,135.4995612
//                    ,135.4995568
//                    ,135.4995524
//                    ,135.4995482
//                    ,135.4995449
//                    ,135.499542
//                    ,135.4995398
//                    ,135.4995379
//                    ,135.4995366
//                    ,135.4995355
//                    ,135.4995352
//                    ,135.4995356
//                    ,135.499537
//                    ,135.4995392
//                    ,135.499542
//                    ,135.4995448
//                    ,135.4995478
//                    ,135.4995506
//                    ,135.499453
//                    ,135.499453
//                    ,135.4994529
//                    ,135.4994529
//                    ,135.4994529
//                    ,135.4994529
//                    ,135.4994529
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.4994528
//                    ,135.499453
//                    ,135.4994541
//                    ,135.4994553
//                    ,135.4994565
//                    ,135.4994573
//                    ,135.4994585
//                    ,135.4994597
//                    ,135.4994609
//                    ,135.4994621
//                    ,135.4994637
//                    ,135.4994649
//                    ,135.4994661
//                    ,135.4994673
//                    ,135.4994685
//                    ,135.4994697
//                    ,135.4994709
//                    ,135.4994721
//                    ,135.4994733
//                    ,135.4994745
//                    ,135.4994757
//                    ,135.4994769
//                    ,135.4994781
//                    ,135.4994793
//                    ,135.4994805
//                    ,135.4994817
//                    ,135.4994829
//                    ,135.4994841
//                    ,135.4994853
//                    ,135.4994865
//                    ,135.4994877
//                    ,135.4994889
//                    ,135.4994901
//                    ,135.4994914
//                    ,135.4994926
//                    ,135.4994938
//                    ,135.499495
//                    ,135.4994962
//                    ,135.4994974
//                    ,135.4994986
//                    ,135.4994996
//                    ,135.4995006
//                    ,135.4995015
//                    ,135.4995025
//                    ,135.4995035
//                    ,135.4995044
//                    ,135.4995054
//                    ,135.4995063
//                    ,135.4995073
//                    ,135.4995082
//                    ,135.4995092
//                    ,135.4995101
//                    ,135.4995111
//                    ,135.499512
//                    ,135.499513
//                    ,135.499514
//                    ,135.4995149
//                    ,135.4995159
//                    ,135.4995168
//                    ,135.4995178
//                    ,135.4995187
//                    ,135.4995197
//                    ,135.4995206
//                    ,135.4995216
//                    ,135.4995225
//                    ,135.4995235
//
//            };
//
//            double[] mmLat = {
//                    34.70262937
//                    ,34.70262539
//                    ,34.7026214
//                    ,34.70261742
//                    ,34.70261344
//                    ,34.70260945
//                    ,34.70260547
//                    ,34.70260149
//                    ,34.7025975
//                    ,34.70259456
//                    ,34.7025916
//                    ,34.70258824
//                    ,34.70258488
//                    ,34.70258084
//                    ,34.70257679
//                    ,34.70257273
//                    ,34.70256868
//                    ,34.70256463
//                    ,34.70256058
//                    ,34.70255653
//                    ,34.70255248
//                    ,34.70254843
//                    ,34.70254438
//                    ,34.70253976
//                    ,34.7025349
//                    ,34.70253008
//                    ,34.70252525
//                    ,34.70252043
//                    ,34.70251561
//                    ,34.70251078
//                    ,34.70250596
//                    ,34.70250113
//                    ,34.70249631
//                    ,34.70249149
//                    ,34.70248666
//                    ,34.70248184
//                    ,34.70247701
//                    ,34.70247219
//                    ,34.70246737
//                    ,34.70246254
//                    ,34.70245772
//                    ,34.70245289
//                    ,34.70244807
//                    ,34.70244325
//                    ,34.70243842
//                    ,34.7024336
//                    ,34.70242877
//                    ,34.7024261
//                    ,34.70242401
//                    ,34.70242311
//                    ,34.70242275
//                    ,34.70242325
//                    ,34.70242326
//                    ,34.70242329
//                    ,34.70242365
//                    ,34.70242557
//                    ,34.70242829
//                    ,34.70243176
//                    ,34.70243561
//                    ,34.70244015
//                    ,34.70244482
//                    ,34.70245021
//                    ,34.702456
//                    ,34.70246235
//                    ,34.70246891
//                    ,34.70247555
//                    ,34.70248219
//                    ,34.70248883
//                    ,34.70249547
//                    ,34.70250211
//                    ,34.70250876
//                    ,34.7025154
//                    ,34.70252205
//                    ,34.70252869
//                    ,34.70253534
//                    ,34.70254198
//                    ,34.70254862
//                    ,34.70255527
//                    ,34.70256191
//                    ,34.70256856
//                    ,34.7025752
//                    ,34.70258185
//                    ,34.70258849
//                    ,34.70259508
//                    ,34.70260168
//                    ,34.70260823
//                    ,34.70261476
//                    ,34.70262126
//                    ,34.70262775
//                    ,34.70263425
//                    ,34.70264075
//                    ,34.70264725
//                    ,34.70265375
//                    ,34.70266025
//                    ,34.70266675
//                    ,34.70267324
//                    ,34.70267974
//                    ,34.70268624
//                    ,34.70269274
//                    ,34.70269924
//                    ,34.70270574
//                    ,34.70271224
//                    ,34.70271873
//                    ,34.70272523
//                    ,34.70273173
//                    ,34.70273823
//                    ,34.70274473
//                    ,34.70275123
//                    ,34.70275773
//                    ,34.70276422
//                    ,34.70277072
//                    ,34.70277722
//                    ,34.70278372
//                    ,34.70279022
//                    ,34.70279672
//                    ,34.70280322
//                    ,34.70280971
//                    ,34.70281621
//                    ,34.70282271
//                    ,34.70282921
//                    ,34.70283571
//                    ,34.70284221
//                    ,34.70284871
//                    ,34.7028552
//                    ,34.7028617
//                    ,34.7028682
//                    ,34.7028747
//                    ,34.7028812
//                    ,34.7028877
//                    ,34.7028942
//                    ,34.70290069
//                    ,34.70290719
//                    ,34.70291369
//                    ,34.70292019
//                    ,34.70292669
//                    ,34.70293319
//                    ,34.70293969
//                    ,34.70294618
//                    ,34.70295268
//                    ,34.70295918
//                    ,34.70296568
//                    ,34.70297218
//                    ,34.70297868
//                    ,34.70298518
//            };
//            double[] mmLng = {
//                    135.4998317
//                    ,135.4998265
//                    ,135.4998213
//                    ,135.4998161
//                    ,135.4998109
//                    ,135.4998057
//                    ,135.4998005
//                    ,135.4997953
//                    ,135.4997901
//                    ,135.499784
//                    ,135.4997779
//                    ,135.4997709
//                    ,135.4997639
//                    ,135.4997575
//                    ,135.4997511
//                    ,135.4997446
//                    ,135.4997382
//                    ,135.4997318
//                    ,135.4997254
//                    ,135.499719
//                    ,135.4997125
//                    ,135.4997061
//                    ,135.4996997
//                    ,135.4996939
//                    ,135.4996884
//                    ,135.4996828
//                    ,135.4996772
//                    ,135.4996717
//                    ,135.4996661
//                    ,135.4996605
//                    ,135.499655
//                    ,135.4996494
//                    ,135.4996438
//                    ,135.4996383
//                    ,135.4996327
//                    ,135.4996271
//                    ,135.4996215
//                    ,135.499616
//                    ,135.4996104
//                    ,135.4996048
//                    ,135.4995993
//                    ,135.4995937
//                    ,135.4995881
//                    ,135.4995826
//                    ,135.499577
//                    ,135.4995714
//                    ,135.4995658
//                    ,135.4995585
//                    ,135.4995508
//                    ,135.4995428
//                    ,135.4995347
//                    ,135.4995267
//                    ,135.4995186
//                    ,135.4995105
//                    ,135.4995024
//                    ,135.4994947
//                    ,135.4994873
//                    ,135.4994804
//                    ,135.4994738
//                    ,135.4994679
//                    ,135.4994621
//                    ,135.4994574
//                    ,135.4994535
//                    ,135.4994512
//                    ,135.4994499
//                    ,135.4994499
//                    ,135.4994499
//                    ,135.4994503
//                    ,135.4994502
//                    ,135.49945
//                    ,135.4994499
//                    ,135.4994497
//                    ,135.4994496
//                    ,135.4994495
//                    ,135.4994493
//                    ,135.4994492
//                    ,135.499449
//                    ,135.4994489
//                    ,135.4994487
//                    ,135.4994486
//                    ,135.4994484
//                    ,135.4994483
//                    ,135.4994482
//                    ,135.4994491
//                    ,135.49945
//                    ,135.4994514
//                    ,135.4994529
//                    ,135.4994546
//                    ,135.4994563
//                    ,135.499458
//                    ,135.4994597
//                    ,135.4994614
//                    ,135.4994631
//                    ,135.4994648
//                    ,135.4994665
//                    ,135.4994681
//                    ,135.4994698
//                    ,135.4994715
//                    ,135.4994732
//                    ,135.4994749
//                    ,135.4994766
//                    ,135.4994783
//                    ,135.49948
//                    ,135.4994817
//                    ,135.4994834
//                    ,135.4994851
//                    ,135.4994868
//                    ,135.4994885
//                    ,135.4994902
//                    ,135.4994919
//                    ,135.4994936
//                    ,135.4994953
//                    ,135.499497
//                    ,135.4994986
//                    ,135.4995003
//                    ,135.499502
//                    ,135.4995037
//                    ,135.4995054
//                    ,135.4995071
//                    ,135.4995088
//                    ,135.4995105
//                    ,135.4995122
//                    ,135.4995139
//                    ,135.4995156
//                    ,135.4995173
//                    ,135.499519
//                    ,135.4995207
//                    ,135.4995224
//                    ,135.4995241
//                    ,135.4995258
//                    ,135.4995274
//                    ,135.4995291
//                    ,135.4995308
//                    ,135.4995325
//                    ,135.4995342
//                    ,135.4995359
//                    ,135.4995376
//                    ,135.4995393
//                    ,135.499541
//                    ,135.4995427
//                    ,135.4995444
//                    ,135.4995461
//                    ,135.4995478
//                    ,135.4995495
//            };

//            cc5f
            double[] correctLat = {
            34.97948685,
            34.9794819,
            34.97947613,
            34.97948492,
            34.97952997,
            34.97956816,
            34.97961266,
            34.9796514,
            34.97966623,
            34.97967008,
            34.97966376,
            34.97967887,
            34.97966843,
            34.97967777,
            34.97968244,
            34.9796676,
            34.97959096,
            34.97954838,
            34.9795014,
            34.97948767,
            34.97948685};
            double[] correctLng= {
            135.9644506,
            135.9645737,
            135.9647105,
            135.9647296,
            135.9647322,
            135.9647574,
            135.9647346,
            135.9647383,
            135.9647165,
            135.9646243,
            135.9645884,
            135.9645525,
            135.9645133,
            135.9644791,
            135.9643742,
            135.9643433,
            135.964339,
            135.9643356,
            135.9643809,
            135.964408,
            135.9644506};

            double[] rawPdrLat = {
                   34.97948654
                    ,34.97948623
                    ,34.97948592
                    ,34.97948561
                    ,34.97948529
                    ,34.97948498
                    ,34.97948467
                    ,34.97948436
                    ,34.97948405
                    ,34.97948374
                    ,34.97948343
                    ,34.97948312
                    ,34.9794828
                    ,34.97948249
                    ,34.97948218
                    ,34.97948187
                    ,34.97948156
                    ,34.97948125
                    ,34.97948094
                    ,34.97948063
                    ,34.97948031
                    ,34.97948
                    ,34.97947969
                    ,34.97947938
                    ,34.97947907
                    ,34.97947876
                    ,34.97947845
                    ,34.97947814
                    ,34.97947782
                    ,34.97947751
                    ,34.9794772
                    ,34.97947689
                    ,34.97947819
                    ,34.97948127
                    ,34.97948634
                    ,34.97949237
                    ,34.9794987
                    ,34.97950506
                    ,34.97951143
                    ,34.97951781
                    ,34.97952418
                    ,34.97953056
                    ,34.97953694
                    ,34.97954272
                    ,34.97954867
                    ,34.97955487
                    ,34.97956121
                    ,34.97956758
                    ,34.97957374
                    ,34.97957969
                    ,34.97958566
                    ,34.97959197
                    ,34.97959834
                    ,34.97960471
                    ,34.97961108
                    ,34.97961744
                    ,34.97962381
                    ,34.97963018
                    ,34.97963654
                    ,34.97964291
                    ,34.97964915
                    ,34.97965473
                    ,34.97965868
                    ,34.97966117
                    ,34.97966245
                    ,34.97966369
                    ,34.97966456
                    ,34.97966565
                    ,34.97966673
                    ,34.97966782
                    ,34.9796689
                    ,34.97966999
                    ,34.97967108
                    ,34.97967216
                    ,34.97967325
                    ,34.97967434
                    ,34.97967542
                    ,34.97967651
                    ,34.97967487
                    ,34.97967291
                    ,34.97967114
                    ,34.97966937
                    ,34.9796676
                    ,34.97967103
                    ,34.97967528
                    ,34.97967914
                    ,34.97968146
                    ,34.97968205
                    ,34.97968064
                    ,34.9796782
                    ,34.97967602
                    ,34.97967529
                    ,34.97967612
                    ,34.97967855
                    ,34.97968049
                    ,34.97968163
                    ,34.97968278
                    ,34.97968392
                    ,34.97968472
                    ,34.97968553
                    ,34.97968633
                    ,34.97968713
                    ,34.97968794
                    ,34.97968874
                    ,34.97968954
                    ,34.97969035
                    ,34.97969115
                    ,34.97969195
                    ,34.97969276
                    ,34.97969356
                    ,34.97969436
                    ,34.97969362
                    ,34.97969065
                    ,34.97968602
                    ,34.97967998
                    ,34.97967369
                    ,34.97966733
                    ,34.97966097
                    ,34.9796546
                    ,34.97964824
                    ,34.97964188
                    ,34.97963552
                    ,34.97962916
                    ,34.9796228
                    ,34.97961644
                    ,34.97961008
                    ,34.97960372
                    ,34.97959736
                    ,34.979591
                    ,34.97958464
                    ,34.97957827
                    ,34.97957191
                    ,34.97956555
                    ,34.97955957
                    ,34.97955424
                    ,34.97954954
                    ,34.97954483
                    ,34.97953991
                    ,34.97953501
                    ,34.97953012
                    ,34.97952523
                    ,34.97952034
                    ,34.97951545
                    ,34.97951251
                    ,34.97951122
                    ,34.9795109
                    ,34.97951104
                    ,34.97951114
                    ,34.97951137
                    ,34.9795116
                    ,34.97951183
                    ,34.97951206
                    ,34.97951229
                    ,34.97951252
            };
            double[] rawPdrLng = {
                   135.9644584
                    ,135.9644662
                    ,135.964474
                    ,135.9644817
                    ,135.9644895
                    ,135.9644973
                    ,135.9645051
                    ,135.9645129
                    ,135.9645207
                    ,135.9645285
                    ,135.9645363
                    ,135.964544
                    ,135.9645518
                    ,135.9645596
                    ,135.9645674
                    ,135.9645752
                    ,135.964583
                    ,135.9645908
                    ,135.9645986
                    ,135.9646064
                    ,135.9646141
                    ,135.9646219
                    ,135.9646297
                    ,135.9646375
                    ,135.9646453
                    ,135.9646531
                    ,135.9646609
                    ,135.9646687
                    ,135.9646764
                    ,135.9646842
                    ,135.964692
                    ,135.9646998
                    ,135.9647074
                    ,135.9647142
                    ,135.9647189
                    ,135.9647213
                    ,135.9647221
                    ,135.9647226
                    ,135.9647228
                    ,135.9647231
                    ,135.9647232
                    ,135.9647233
                    ,135.9647234
                    ,135.9647266
                    ,135.9647294
                    ,135.9647311
                    ,135.9647319
                    ,135.9647318
                    ,135.9647299
                    ,135.9647271
                    ,135.9647244
                    ,135.9647233
                    ,135.9647232
                    ,135.9647235
                    ,135.9647236
                    ,135.9647241
                    ,135.9647246
                    ,135.9647251
                    ,135.9647256
                    ,135.9647261
                    ,135.9647246
                    ,135.9647209
                    ,135.9647148
                    ,135.9647076
                    ,135.9647
                    ,135.9646924
                    ,135.9646847
                    ,135.964677
                    ,135.9646693
                    ,135.9646616
                    ,135.9646539
                    ,135.9646462
                    ,135.9646386
                    ,135.9646309
                    ,135.9646232
                    ,135.9646155
                    ,135.9646078
                    ,135.9646001
                    ,135.9645926
                    ,135.9645852
                    ,135.9645777
                    ,135.9645703
                    ,135.9645628
                    ,135.9645563
                    ,135.9645505
                    ,135.9645443
                    ,135.964537
                    ,135.9645293
                    ,135.9645217
                    ,135.9645145
                    ,135.9645072
                    ,135.9644995
                    ,135.9644918
                    ,135.9644846
                    ,135.9644772
                    ,135.9644695
                    ,135.9644618
                    ,135.9644542
                    ,135.9644464
                    ,135.9644387
                    ,135.964431
                    ,135.9644232
                    ,135.9644155
                    ,135.9644078
                    ,135.9644
                    ,135.9643923
                    ,135.9643846
                    ,135.9643768
                    ,135.9643691
                    ,135.9643614
                    ,135.9643536
                    ,135.9643459
                    ,135.964339
                    ,135.9643337
                    ,135.9643313
                    ,135.9643301
                    ,135.9643296
                    ,135.9643291
                    ,135.9643289
                    ,135.9643283
                    ,135.9643277
                    ,135.9643271
                    ,135.9643266
                    ,135.964326
                    ,135.9643254
                    ,135.9643248
                    ,135.9643242
                    ,135.9643237
                    ,135.9643231
                    ,135.9643225
                    ,135.9643219
                    ,135.9643214
                    ,135.9643208
                    ,135.9643234
                    ,135.9643276
                    ,135.9643329
                    ,135.9643381
                    ,135.9643431
                    ,135.9643481
                    ,135.9643531
                    ,135.9643581
                    ,135.9643631
                    ,135.9643681
                    ,135.964375
                    ,135.9643826
                    ,135.9643904
                    ,135.9643981
                    ,135.9644059
                    ,135.9644137
                    ,135.9644215
                    ,135.9644293
                    ,135.9644371
                    ,135.9644449
                    ,135.9644527
            };

            double[] smLat = {
                   34.97949021
                    ,34.97948988
                    ,34.97948955
                    ,34.97948922
                    ,34.9794889
                    ,34.97948857
                    ,34.97948824
                    ,34.97948791
                    ,34.97948758
                    ,34.97948726
                    ,34.97948693
                    ,34.9794866
                    ,34.97948627
                    ,34.97948594
                    ,34.97948561
                    ,34.97948529
                    ,34.97948496
                    ,34.97948463
                    ,34.9794843
                    ,34.97948397
                    ,34.97948365
                    ,34.97948332
                    ,34.97948299
                    ,34.97948266
                    ,34.97948233
                    ,34.979482
                    ,34.97948168
                    ,34.97948135
                    ,34.97948102
                    ,34.97948069
                    ,34.97948036
                    ,34.97948004
                    ,34.97947972
                    ,34.97947944
                    ,34.97947925
                    ,34.97947916
                    ,34.97947913
                    ,34.97947913
                    ,34.97947913
                    ,34.97947913
                    ,34.97947914
                    ,34.97953281
                    ,34.97953917
                    ,34.97954201
                    ,34.9795481
                    ,34.9795544
                    ,34.97956076
                    ,34.9795671
                    ,34.97957311
                    ,34.97957886
                    ,34.97958464
                    ,34.97959086
                    ,34.97959719
                    ,34.97960355
                    ,34.97960992
                    ,34.97961628
                    ,34.97962578
                    ,34.97963215
                    ,34.97963852
                    ,34.97964489
                    ,34.9796479
                    ,34.97965322
                    ,34.97965677
                    ,34.97965881
                    ,34.9796596
                    ,34.97966035
                    ,34.97966074
                    ,34.97966134
                    ,34.97966736
                    ,34.97966766
                    ,34.97966795
                    ,34.97966825
                    ,34.97966855
                    ,34.97966884
                    ,34.97966914
                    ,34.97966944
                    ,34.97966973
                    ,34.97967003
                    ,34.97967101
                    ,34.97967129
                    ,34.97967158
                    ,34.97967116
                    ,34.97967145
                    ,34.9796724
                    ,34.97967263
                    ,34.97967287
                    ,34.97967315
                    ,34.97967345
                    ,34.97967374
                    ,34.97967401
                    ,34.97967429
                    ,34.97967459
                    ,34.97967488
                    ,34.97967516
                    ,34.97967545
                    ,34.97967574
                    ,34.97967534
                    ,34.97967564
                    ,34.97967663
                    ,34.97967623
                    ,34.97967653
                    ,34.97967683
                    ,34.97967712
                    ,34.97967742
                    ,34.97967772
                    ,34.97967802
                    ,34.97967831
                    ,34.97967861
                    ,34.97967891
                    ,34.97967921
                    ,34.97967951
                    ,34.9796805
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97968052
                    ,34.97962725
                    ,34.97962088
                    ,34.9796145
                    ,34.97960813
                    ,34.97960176
                    ,34.97959539
                    ,34.97958901
                    ,34.97958264
                    ,34.97957632
                    ,34.97957001
                    ,34.9795637
                    ,34.97955739
                    ,34.97955108
                    ,34.97955781
                    ,34.97955318
                    ,34.97954933
                    ,34.97954545
                    ,34.97954134
                    ,34.97953726
                    ,34.97951738
                    ,34.97951145
                    ,34.97950552
                    ,34.97949959
                    ,34.9795117
                    ,34.97950771
                    ,34.97950444
                    ,34.97950154
                    ,34.97949861
                    ,34.97949579
                    ,34.97949132
                    ,34.97949099
                    ,34.97949066
                    ,34.97949034
                    ,34.97949001
            };
            double[] smLng = {
                   135.9644585
                    ,135.9644663
                    ,135.9644741
                    ,135.9644819
                    ,135.9644897
                    ,135.9644975
                    ,135.9645053
                    ,135.964513
                    ,135.9645208
                    ,135.9645286
                    ,135.9645364
                    ,135.9645442
                    ,135.964552
                    ,135.9645598
                    ,135.9645676
                    ,135.9645753
                    ,135.9645831
                    ,135.9645909
                    ,135.9645987
                    ,135.9646065
                    ,135.9646143
                    ,135.9646221
                    ,135.9646299
                    ,135.9646376
                    ,135.9646454
                    ,135.9646532
                    ,135.964661
                    ,135.9646688
                    ,135.9646766
                    ,135.9646844
                    ,135.9646921
                    ,135.9646999
                    ,135.9647075
                    ,135.9647141
                    ,135.9647186
                    ,135.9647208
                    ,135.9647213
                    ,135.9647215
                    ,135.9647215
                    ,135.9647214
                    ,135.9647213
                    ,135.9647373
                    ,135.9647377
                    ,135.9647379
                    ,135.9647383
                    ,135.9647387
                    ,135.9647391
                    ,135.9647395
                    ,135.9647399
                    ,135.9647402
                    ,135.9647406
                    ,135.964741
                    ,135.9647414
                    ,135.9647418
                    ,135.9647422
                    ,135.9647426
                    ,135.9647432
                    ,135.9647436
                    ,135.964744
                    ,135.9647444
                    ,135.9647446
                    ,135.9647449
                    ,135.9647451
                    ,135.9647453
                    ,135.9647453
                    ,135.9647454
                    ,135.9647454
                    ,135.9647454
                    ,135.9646876
                    ,135.9646799
                    ,135.9646722
                    ,135.9646644
                    ,135.9646567
                    ,135.964649
                    ,135.9646413
                    ,135.9646336
                    ,135.9646259
                    ,135.9646182
                    ,135.9645925
                    ,135.9645852
                    ,135.9645778
                    ,135.9645886
                    ,135.9645812
                    ,135.9645563
                    ,135.9645504
                    ,135.964544
                    ,135.9645367
                    ,135.964529
                    ,135.9645215
                    ,135.9645144
                    ,135.9645071
                    ,135.9644995
                    ,135.9644917
                    ,135.9644845
                    ,135.964477
                    ,135.9644693
                    ,135.9644798
                    ,135.9644721
                    ,135.9644461
                    ,135.9644566
                    ,135.9644489
                    ,135.9644411
                    ,135.9644334
                    ,135.9644256
                    ,135.9644178
                    ,135.9644101
                    ,135.9644023
                    ,135.9643946
                    ,135.9643868
                    ,135.9643791
                    ,135.9643713
                    ,135.9643454
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.964345
                    ,135.9643419
                    ,135.9643415
                    ,135.9643411
                    ,135.9643407
                    ,135.9643404
                    ,135.96434
                    ,135.9643396
                    ,135.9643392
                    ,135.9643383
                    ,135.9643373
                    ,135.9643364
                    ,135.9643355
                    ,135.9643346
                    ,135.9643356
                    ,135.9643349
                    ,135.9643343
                    ,135.9643338
                    ,135.9643332
                    ,135.9643326
                    ,135.9643706
                    ,135.9643734
                    ,135.9643762
                    ,135.964379
                    ,135.9643733
                    ,135.9643751
                    ,135.9643767
                    ,135.9643781
                    ,135.9643794
                    ,135.9643808
                    ,135.9644322
                    ,135.96444
                    ,135.9644478
                    ,135.9644555
                    ,135.9644633
            };

            double[] mmLat = {
                    34.97948654
                    ,34.97948621
                    ,34.97948589
                    ,34.97948556
                    ,34.97948524
                    ,34.97948491
                    ,34.97948459
                    ,34.97948426
                    ,34.97948393
                    ,34.97948361
                    ,34.97948328
                    ,34.97948296
                    ,34.97948263
                    ,34.97948231
                    ,34.97948198
                    ,34.97948166
                    ,34.97948133
                    ,34.97948101
                    ,34.97948068
                    ,34.97948035
                    ,34.97948003
                    ,34.9794797
                    ,34.97947938
                    ,34.97947905
                    ,34.97947873
                    ,34.9794784
                    ,34.97947808
                    ,34.97947775
                    ,34.97947742
                    ,34.9794771
                    ,34.97947677
                    ,34.97947645
                    ,34.97947777
                    ,34.97948094
                    ,34.97948618
                    ,34.97949243
                    ,34.97949903
                    ,34.97950567
                    ,34.97951233
                    ,34.97951899
                    ,34.97952565
                    ,34.97953232
                    ,34.97953867
                    ,34.97954431
                    ,34.97955014
                    ,34.97955627
                    ,34.97956257
                    ,34.97956893
                    ,34.9795751
                    ,34.97958106
                    ,34.97958705
                    ,34.97959336
                    ,34.97959971
                    ,34.97960606
                    ,34.97961242
                    ,34.97961875
                    ,34.97962509
                    ,34.97963142
                    ,34.97963776
                    ,34.97964409
                    ,34.97965043
                    ,34.97965677
                    ,34.9796631
                    ,34.97966525
                    ,34.97966614
                    ,34.97966698
                    ,34.97966743
                    ,34.97966812
                    ,34.9796688
                    ,34.97966951
                    ,34.97967022
                    ,34.97967093
                    ,34.97967165
                    ,34.97967236
                    ,34.97967307
                    ,34.97967378
                    ,34.9796745
                    ,34.97967521
                    ,34.97967333
                    ,34.9796711
                    ,34.97966908
                    ,34.97966706
                    ,34.97966504
                    ,34.97966848
                    ,34.97967278
                    ,34.97967668
                    ,34.97967895
                    ,34.9796794
                    ,34.97967774
                    ,34.97967502
                    ,34.97967258
                    ,34.97967164
                    ,34.97967233
                    ,34.97967471
                    ,34.97967657
                    ,34.97967759
                    ,34.97967862
                    ,34.97967964
                    ,34.9796803
                    ,34.97968097
                    ,34.97968163
                    ,34.9796823
                    ,34.97968296
                    ,34.97968363
                    ,34.97968429
                    ,34.97968495
                    ,34.97968562
                    ,34.97968628
                    ,34.97968695
                    ,34.97968761
                    ,34.97968828
                    ,34.97968764
                    ,34.97968467
                    ,34.97967993
                    ,34.97967367
                    ,34.97966713
                    ,34.97966051
                    ,34.97965387
                    ,34.97964723
                    ,34.9796406
                    ,34.97963398
                    ,34.97962735
                    ,34.97962073
                    ,34.9796141
                    ,34.97960747
                    ,34.97960085
                    ,34.97959422
                    ,34.97958759
                    ,34.97958096
                    ,34.97957434
                    ,34.97956771
                    ,34.97956108
                    ,34.97955446
                    ,34.97954834
                    ,34.97954296
                    ,34.9795383
                    ,34.97953362
                    ,34.97952871
                    ,34.97952383
                    ,34.97951895
                    ,34.97951368
                    ,34.97950842
                    ,34.97950315
                    ,34.97949995
                    ,34.97949846
                    ,34.97949796
                    ,34.97949793
                    ,34.97949787
                    ,34.97949793
                    ,34.979498
                    ,34.97949807
                    ,34.97949814
                    ,34.97949821
                    ,34.97949827
            };
            double[] mmLng = {
                    135.9644584
                    ,135.9644665
                    ,135.9644747
                    ,135.9644828
                    ,135.964491
                    ,135.9644991
                    ,135.9645073
                    ,135.9645154
                    ,135.9645236
                    ,135.9645317
                    ,135.9645398
                    ,135.964548
                    ,135.9645561
                    ,135.9645643
                    ,135.9645724
                    ,135.9645806
                    ,135.9645887
                    ,135.9645969
                    ,135.964605
                    ,135.9646132
                    ,135.9646213
                    ,135.9646294
                    ,135.9646376
                    ,135.9646457
                    ,135.9646539
                    ,135.964662
                    ,135.9646702
                    ,135.9646783
                    ,135.9646865
                    ,135.9646946
                    ,135.9647028
                    ,135.9647109
                    ,135.9647189
                    ,135.964726
                    ,135.964731
                    ,135.9647338
                    ,135.9647348
                    ,135.9647355
                    ,135.964736
                    ,135.9647365
                    ,135.9647368
                    ,135.9647372
                    ,135.9647375
                    ,135.964741
                    ,135.9647441
                    ,135.9647461
                    ,135.9647472
                    ,135.9647473
                    ,135.9647455
                    ,135.9647428
                    ,135.9647402
                    ,135.9647393
                    ,135.9647394
                    ,135.9647399
                    ,135.9647403
                    ,135.964741
                    ,135.9647417
                    ,135.9647425
                    ,135.9647432
                    ,135.9647439
                    ,135.9647446
                    ,135.9647453
                    ,135.9647461
                    ,135.9647389
                    ,135.9647312
                    ,135.9647235
                    ,135.9647157
                    ,135.964708
                    ,135.9647002
                    ,135.9646922
                    ,135.9646841
                    ,135.964676
                    ,135.9646679
                    ,135.9646598
                    ,135.9646517
                    ,135.9646436
                    ,135.9646355
                    ,135.9646274
                    ,135.9646197
                    ,135.964612
                    ,135.9646043
                    ,135.9645965
                    ,135.9645888
                    ,135.9645819
                    ,135.9645757
                    ,135.9645691
                    ,135.9645615
                    ,135.9645534
                    ,135.9645456
                    ,135.9645381
                    ,135.9645306
                    ,135.9645225
                    ,135.9645145
                    ,135.9645069
                    ,135.9644991
                    ,135.9644911
                    ,135.964483
                    ,135.964475
                    ,135.9644669
                    ,135.9644588
                    ,135.9644507
                    ,135.9644426
                    ,135.9644345
                    ,135.9644264
                    ,135.9644183
                    ,135.9644102
                    ,135.9644021
                    ,135.9643941
                    ,135.964386
                    ,135.9643779
                    ,135.9643698
                    ,135.9643617
                    ,135.9643544
                    ,135.9643488
                    ,135.9643461
                    ,135.9643448
                    ,135.964344
                    ,135.9643434
                    ,135.964343
                    ,135.9643423
                    ,135.9643415
                    ,135.9643408
                    ,135.96434
                    ,135.9643393
                    ,135.9643385
                    ,135.9643378
                    ,135.9643371
                    ,135.9643363
                    ,135.9643356
                    ,135.9643348
                    ,135.9643341
                    ,135.9643333
                    ,135.9643326
                    ,135.9643357
                    ,135.9643404
                    ,135.9643462
                    ,135.964352
                    ,135.9643575
                    ,135.964363
                    ,135.9643686
                    ,135.9643745
                    ,135.9643805
                    ,135.9643865
                    ,135.9643943
                    ,135.9644029
                    ,135.9644116
                    ,135.9644204
                    ,135.9644292
                    ,135.964438
                    ,135.9644467
                    ,135.9644555
                    ,135.9644643
                    ,135.9644731
                    ,135.9644819
            };




            PolylineOptions correctPolylineOptions = new PolylineOptions().color(Color.BLUE).width(3);
            for(int i = 0; i < correctLat.length; i++) {
                correctPolylineOptions.add(new LatLng(correctLat[i], correctLng[i]));
            }
            Polyline correctPolyline =  map.addPolyline(correctPolylineOptions);

            PolylineOptions rawPdrPolylineOptions = new PolylineOptions().color(Color.RED).width(3);
            for(int i = 0; i < rawPdrLat.length; i++) {
                rawPdrPolylineOptions.add(new LatLng(rawPdrLat[i], rawPdrLng[i]));
            }
            Polyline rawPdrPolyline =  map.addPolyline(rawPdrPolylineOptions);

            PolylineOptions smPolylineOptions = new PolylineOptions().color(Color.GREEN).width(3);
            for(int i = 0; i < smLat.length; i++) {
                smPolylineOptions.add(new LatLng(smLat[i], smLng[i]));
            }
            Polyline smPolyline =  map.addPolyline(smPolylineOptions);

            PolylineOptions mmPolylineOptions = new PolylineOptions().color(Color.MAGENTA).width(3);
            for(int i = 0; i < mmLat.length; i++) {
                mmPolylineOptions.add(new LatLng(mmLat[i], mmLng[i]));
            }
            Polyline mmPolyline =  map.addPolyline(mmPolylineOptions);

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
