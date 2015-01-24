package ubilabmapmatchinglibrary.mapmatching;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import ubilabmapmatchinglibrary.calculate.Calculator2D;

/**
 * Created by shun on 2014/12/10.
 * 軌跡クラス
 * ポイントのリスト
 */
public class Trajectory {

    /**
     * 座標変換用(地球の円周)
     */
    final static double RX = 40076500;
    final static double RY = 40008600;

    private ArrayList<TrackPoint> trajectory;

    public Trajectory() {
        trajectory = new ArrayList<TrackPoint>();
    }

    public ArrayList<TrackPoint> getTrajectory() {
        return trajectory;
    }

    public void setTrajectory(ArrayList<TrackPoint> trajectory) {
        this.trajectory = trajectory;
    }

    public void add(TrackPoint point) {
        trajectory.add(point);
        return;
    }

    public void addAll(Trajectory trajectory) {
        this.trajectory.addAll(trajectory.getTrajectory());
        return;
    }

    public void clear() {
        trajectory.clear();
    }

    public TrackPoint get(int index) {
        return trajectory.get(index);
    }

    public int size() {
        return trajectory.size();
    }

    public void remove(int index) {
        trajectory.remove(index);
        return;
    }

    /**
     * 生の軌跡を{Rd,Rs}で補正する
     * @param rate {Rd,Rs}の組
     * @return true
     */
    public boolean transTrack(Point rate) {
        transTrack(rate.getX(), rate.getY());
        return true;
    }

    /**
     * 生の軌跡を{Rd,Rs}で補正する
     * @param directionTransRate 進行方向変化量の補正レート(Rd)
     * @param directionTransRate 歩幅の補正レート(Rs)
     * @return true
     */
    public boolean transTrack(double directionTransRate, double distanceTransRate) {

        Trajectory transTrajectory = new Trajectory();
        LatLng point = null;

        LatLng lastPoint = null;
        double lastDirection = 0;


        long time = 0;
        double direction = 0;
        double distance = 0;

        for (int i = 0; i < trajectory.size(); i++) {
            TrackPoint rawTrack = trajectory.get(i);

            if(i == 0) {
                lastPoint = rawTrack.getLocation();

                lastDirection = rawTrack.getDirection();

                transTrajectory.add(rawTrack);
            } else {

                time = rawTrack.getTime();
                double changedDirection = rawTrack.getDirection() - lastDirection;
                if(changedDirection > 180) {
                    changedDirection -= 360;
                } else if (changedDirection < -180){
                    changedDirection += 360;
                }

                if(!rawTrack.getIsStraight()) {

                    direction = transTrajectory.get(i - 1).getDirection() + (changedDirection * directionTransRate);

                    distance = rawTrack.getDistance() * distanceTransRate;

                    double lat = lastPoint.latitude + (distance * Math.sin(Math.toRadians(direction)) / 100/  (RX / 360));
                    double lng = lastPoint.longitude + (distance * Math.cos(Math.toRadians(direction)) / 100/ (RY * Math.cos(Math.toRadians(lastPoint.latitude)) / 360));
                    point = new LatLng(lat, lng);
                } else {

                    direction = transTrajectory.get(i - 1).getDirection() + changedDirection;

                    distance = rawTrack.getDistance() * distanceTransRate;

                    double lat = lastPoint.latitude + (distance * Math.sin(Math.toRadians(direction)) / 100 / (RX / 360));
                    double lng = lastPoint.longitude + (distance * Math.cos(Math.toRadians(direction)) / 100 / (RY * Math.cos(Math.toRadians(lastPoint.latitude)) / 360));
                    point = new LatLng(lat, lng);

                }

                lastPoint = point;
                lastDirection = rawTrack.getDirection();

                transTrajectory.add(new TrackPoint(time, point, direction, distance, rawTrack.getIsStraight(), rawTrack.getLinkId()));
            }

        }

        trajectory.clear();
        trajectory.addAll(transTrajectory.getTrajectory());

        return true;
    }

    public double getChangedDirection() {
        LatLng pointA = trajectory.get(0).getLocation();
        LatLng pointB = trajectory.get(1).getLocation();

        LatLng pointC = trajectory.get(trajectory.size() - 2).getLocation();
        LatLng pointD = trajectory.get(trajectory.size() - 1).getLocation();

        double startDirection = Calculator2D.calculateDirection(pointA, pointB);
        double finishDirection = Calculator2D.calculateDirection(pointC, pointD);
        double changedDirection = finishDirection - startDirection;
        if(changedDirection < 0) {
            changedDirection += 360;
        }
        return changedDirection;
    }

    /*別の軌跡から指定した歩数までの軌跡を取得する*/
    public void drawTrajectory(Trajectory rawTrajectory, int count) {
        this.trajectory.clear();
        for(int i = 0; i < count; i++) {
            this.trajectory.add(new TrackPoint(rawTrajectory.get(i)));
        }
    }

    /*指定した歩数までの軌跡を消去する*/
    public void removeTrajectory(int count) {
        for(int i = 0; i < count; i++) {
            this.trajectory.remove(0);
        }
    }

    /*軌跡を消去する*/
    public  void removeAll() {
        this.trajectory.clear();
    }
}


