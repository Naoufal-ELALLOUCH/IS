package ubilabmapmatchinglibrary.mapmatching;


/**
 * Created by shun on 2014/12/17.
 */
public interface TrajectoryTransformedListener {
    /**
     * 軌跡が変換されたときに呼ばれる
     * @param rate 変換後の較正係数の組
     * @param trajecotry 確定した軌跡
     * @param newTrackPoint 変換後の測位開始地点
     */
    public void onTrajectoryTransed(Point rate, Trajectory trajecotry, TrackPoint newTrackPoint);
}
