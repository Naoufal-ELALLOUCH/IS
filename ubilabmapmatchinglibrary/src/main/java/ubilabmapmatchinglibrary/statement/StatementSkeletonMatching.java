package ubilabmapmatchinglibrary.statement;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.calculate.Calculator2D;
import ubilabmapmatchinglibrary.mapmatching.TrackPoint;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;

/**
 * Created by shun on 2015/02/01.
 */
public class StatementSkeletonMatching {
    private int count = 0;

    /**
     * 座標変換用(地球の円周)
     */
    private static final double RX = 40076500;
    private static final double RY = 40008600;

    private static StatementSkeletonMatchingHelper mSkeletonMatchingHelper;

    private static LatLng matchedPoint;

    //スケルトンマッチング後の最新のTrackPointを格納する変数
    private static TrackPoint baseTrackPoint = new TrackPoint();

    private static TrackPoint lastTrackPoint = new TrackPoint();
    private static Link matchingLink;
    private static boolean isFirst;
    private static boolean isLastStraight;

    public StatementSkeletonMatching(StatementDatabaseHelper db) {
        mSkeletonMatchingHelper = new StatementSkeletonMatchingHelper(db);
        isFirst = true;
    }

    /**
     * 与えられたTrackPointを基に最新のスケルトンマッチング後のTrackPoint(座標や、方角、マッチングしているLinkIdの情報)を算出するクラス
     * @param trackPoint
     * @return
     */
    public static TrackPoint calculateSkeletonMatchingPosition(TrackPoint trackPoint) {
        try {
            LatLng point = trackPoint.getLocation();
            double direction = trackPoint.getDirection();
            boolean isStraight = trackPoint.getIsStraight();

            if (!isFirst) {
                TrackPoint correctedTrackPoint = rePositioningTrackPoint(baseTrackPoint, trackPoint, lastTrackPoint);
                if (isStraight) { //直進中は1歩ごとにスケルトンマッチング

                    List<Link> linkedLinkList = mSkeletonMatchingHelper.getCandidateLinkList(matchingLink);
                    List<Link> candidateMatchingLinkList = new ArrayList<>();

                    for (Link link : linkedLinkList) {
                        if (mSkeletonMatchingHelper.isProjectToLink(correctedTrackPoint.getLocation(), link)) {
                            candidateMatchingLinkList.add(link);
                        }
                    }
                    if (candidateMatchingLinkList.size() == 0) {
                        return null;
                    }

                    matchingLink = mSkeletonMatchingHelper.getMatchingLink(correctedTrackPoint.getLocation(), baseTrackPoint.getLocation(), candidateMatchingLinkList);
                    if (matchingLink == null) {
                        return null;
                    }
                    matchedPoint = mSkeletonMatchingHelper.getProjectedPoint(correctedTrackPoint.getLocation(), matchingLink);
                    LatLng baseMatchedPoint = mSkeletonMatchingHelper.getProjectedPoint(baseTrackPoint.getLocation(), matchingLink);

                    lastTrackPoint.setTrackPoint(trackPoint);

                    double matchedDirection = Calculator2D.calculateDirection(baseMatchedPoint, matchedPoint);
                    double matchedDistance = Calculator2D.calculateDistance(baseTrackPoint.getLocation(), matchedPoint);

                    System.out.println("SM" + "MatchingLinkId:" + matchingLink.getId() + ", MatchedDirection" + matchedDirection);
//                ////Log.v("SM", "BasePoint:" + baseTrackPoint.getLocation().latitude + ", " + baseTrackPoint.getLocation().longitude);
//                ////Log.v("SM", "CorrectedPoint:" + correctedTrackPoint.getLocation().latitude + ", " + baseTrackPoint.getLocation().longitude);
//                ////Log.v("SM", "TrackPoint:" + point.latitude + ", " + point.longitude);
//                ////Log.v("SM", "MatchedPoint:" + matchedPoint.latitude + ", " + matchedPoint.longitude);
                    System.out.println("SM" +  "---------------------------");

                    baseTrackPoint.setTrackPoint(trackPoint.getTime(), matchedPoint, correctedTrackPoint.getDirection(), matchedDistance, trackPoint.getIsStraight(), matchingLink.getId());
                } else { //曲進中は前回の座標(baseTrackPoint)を基準に座標を再計算する
                    TrackPoint onLinkTrackPoint = new TrackPoint(trackPoint);
                    onLinkTrackPoint.setLocation(mSkeletonMatchingHelper.getProjectedPoint(trackPoint.getLocation(), matchingLink));
                    lastTrackPoint.setTrackPoint(trackPoint);
                    baseTrackPoint.setTrackPoint(correctedTrackPoint.getTime(), correctedTrackPoint.getLocation(), correctedTrackPoint.getDirection(), correctedTrackPoint.getDistance(), correctedTrackPoint.getIsStraight(), matchingLink.getId());
                    return onLinkTrackPoint;
                }

            } else { //過去の位置情報がない時のスケルトンマッチング
                List<Link> firstCandidateMatchingLinkList = mSkeletonMatchingHelper.getFirstCandidateLinkList(point);
                System.out.println("SM" +  "firstCandidateMatchingLinkList.size():" + firstCandidateMatchingLinkList.size());
                if (firstCandidateMatchingLinkList.size() == 0) {
                    return null;
                }
                matchingLink = mSkeletonMatchingHelper.getMatchingLink(point, direction, firstCandidateMatchingLinkList);
                matchedPoint = mSkeletonMatchingHelper.getProjectedPoint(point, matchingLink);
                lastTrackPoint.setTrackPoint(trackPoint);
                baseTrackPoint.setTrackPoint(trackPoint.getTime(), matchedPoint, direction, 0, trackPoint.getIsStraight(), matchingLink.getId());
                isFirst = false;
            }

            isLastStraight = isStraight;
            return baseTrackPoint;
        } catch (Exception e) {
            System.out.println("SM" + "Skeleton Matching is Failed");
            e.printStackTrace();
            return  null;
        }
    }

    /**
     * lastRawPointからrawPointまでの関係をbasePointを基準とした場合のTrackPointを取得する
     * @param basePoint
     * @param rawPoint
     * @param lastRawPoint
     * @return
     */
    public static TrackPoint rePositioningTrackPoint(TrackPoint basePoint, TrackPoint rawPoint, TrackPoint lastRawPoint) {
        double changedDirection = rawPoint.getDirection() - lastRawPoint.getDirection();
        double newDirection = basePoint.getDirection() + changedDirection;
        double stepLength = lastRawPoint.getDistance();
        double newLat = basePoint.getLocation().latitude + (stepLength * Math.sin(Math.toRadians(newDirection)) / 100 / (RX / 360));
        double newLng = basePoint.getLocation().longitude + (stepLength * Math.cos(Math.toRadians(newDirection)) / 100 / (RY * Math.cos(Math.toRadians(newLat)) / 360));

        return new TrackPoint(rawPoint.getTime(), newLat, newLng, newDirection, stepLength, rawPoint.getIsStraight(), rawPoint.getLinkId());
    }

    public static void setFirst() {
        isFirst = true;
    }
}

