package ubilabmapmatchinglibrary.mapmatching;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.calculate.Calculator2D;
import ubilabmapmatchinglibrary.calculate.PointInfoMeshcode;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Node;

/**
 * スケルトンマッチングに必要な情報を算出するクラス
 */
public class SkeletonMatchingHelper {

    private final static double MU_D = 10.0;
    private final static double A = 0.17;
    private final static double N_D = 1.4;
    private final static double MU_ALPHA = 10;
    private final static double N_ALPHA = 4;

    /**
     * 座標変換用(地球の円周)
     */
    final static double RX = 40076500;
    final static double RY = 40008600;

    public static DatabaseHelper db;
    private static Context context;

    public SkeletonMatchingHelper(Context context, DatabaseHelper db) {
        this.context = context;
        this.db = db;
        //db = DatabaseHelper.getInstance(context);
    }

    /**
     * リンクの候補から実際にマッチングさせるリンクを取得する
     * @param point
     * @param lastPoint
     * @param linkList
     * @return
     */
    public static Link getMatchingLink(LatLng point, LatLng lastPoint, List<Link> linkList) {
        double bestScore = 0;
        Link bestScoreLink = null;
        for(Link link : linkList) {
            double totalScore = calculateMatchingScore(point, lastPoint, link);
            if (bestScore < totalScore) {
                bestScore = totalScore;
                bestScoreLink = link;
            }
        }
        return bestScoreLink;
    }

    /**
     * リンクの候補から実際にマッチングさせるリンクを取得する
     * @param point
     * @param direction
     * @param linkList
     * @return
     */
    public static Link getMatchingLink(LatLng point, double direction, List<Link> linkList) {
        double bestScore = 0;
        Link bestScoreLink = null;
        for(Link link : linkList) {
            double totalScore = calculateMatchingScore(point, direction, link);
            if (bestScore < totalScore) {
                bestScore = totalScore;
                bestScoreLink = link;
            }
        }
        return bestScoreLink;
    }

    /**
     * 1歩の歩行軌跡とリンクのマッチングスコア(類似度)を算出する
     * @param point
     * @param lastPoint
     * @param link
     * @return
     */
    public static double calculateMatchingScore(LatLng point, LatLng lastPoint, Link link) {
        double pointsDirection = Calculator2D.calculateDirection(lastPoint, point);
        ////Log.v("SCORE", "pointsDirection:" + pointsDirection);
        return calculateMatchingScore(point, pointsDirection, link);
    }

    /**
     * 1歩の歩行軌跡とリンクのマッチングスコア(類似度)を算出する
     * @param point
     * @param direction
     * @param link
     * @return
     */
    public static double calculateMatchingScore(LatLng point, double direction, Link link) {
        double distanceScore = calculateDistanceScore(calculateDistanceToLink(point, link));
//       ////Log.v("SCORE", "Direction:" +  direction + ", LinkBearing:" + link.getBearing() + ", directionDiff:" + (direction - link.getBearing()));
        double directionScore = calculateDirectionScore((Math.toRadians(direction - link.getBearing())));
        double totalScore = distanceScore + directionScore;
//       ////Log.v("SCORE", "LinkID:" + link.getId() + ", TotalScore" + totalScore);
//       ////Log.v("SCORE", "--------------------------------------------------------------------");
        return totalScore;
    }

    /**
     * 距離のスコア
     * @param distance
     * @return
     */
    public static double calculateDistanceScore(double distance) {
        double score = MU_D - A * Math.pow(distance, N_D);
//       ////Log.v("SCORE", "Distance:" + distance + ", DistanceScore:" + score);
        return score;
    }

    /**
     * 点と直線の距離
     * @param x 点のX座標
     * @param y 点のY座標
     * @param constantA 直線を表す方程式の定数 (y = constantA * x + constantB)
     * @param constantB 直線を表す方程式の定数
     * @return
     */
    public static double calculateDistanceToLink(double x, double y, double constantA, double constantB) {
        double distance = Math.abs(constantA * x - y + constantB) / Math.sqrt(constantA * constantA + 1);
        return distance;
    }

    /**
     * 点からリンクまでの距離
     * @param point
     * @param link
     * @return
     */
    public static double calculateDistanceToLink(LatLng point, Link link) {
        LatLng closestPoint = getProjectedPoint(point, link);
        float results[] = new float[3];
        ////Log.v("SCORE", "closestPoint:" + closestPoint.longitude +  ", " + closestPoint.latitude);
        Location.distanceBetween(point.latitude, point.longitude, closestPoint.latitude, closestPoint.longitude, results);
        return results[0];
    }

    /**
     * 点からリンクのもつノードまでの距離
     * @param point
     * @param link
     * @return
     */
    public static double calculateDistanceToClosestNode(LatLng point, Link link){
        float[] result = new float[3];
        Location.distanceBetween(point.latitude, point.longitude, db.getNodeById(link.getNode1Id()).getLat(), db.getNodeById(link.getNode1Id()).getLng(), result);
        double distance = result[0];
        Location.distanceBetween(point.latitude, point.longitude, db.getNodeById(link.getNode2Id()).getLat(), db.getNodeById(link.getNode2Id()).getLng(), result);
        if(distance > result[0]) {
            distance = result[0];
        }
        return distance;
    }

    /**
     * 角度のスコア
     * @param direction
     * @return
     */
    public static double calculateDirectionScore(double direction) {
        double cos = Math.cos(direction);
        double score =MU_ALPHA * Math.pow(cos, N_ALPHA);
        ////Log.v("SCORE", "Direction:" +  Math.toDegrees(direction) + ", DirectionCos:" + cos + ", DirectionScore:" + score);
        return score;
    }

    /**
     * ベクトルの大きさの2乗
     * @param x ベクトル
     * @return
     */
    public static double norm(double x[])
    {
        return x[0] * x[0] + x[1] * x[1];
    }

    /**
     * リンクに射影させた位置を取得する
     * 線分上に垂線を下せない場合は最も近いノードの位置を取得する
     * @param point
     * @param link
     * @return
     */
    public static LatLng getProjectedPoint(LatLng point, Link link) {

        double d[] = new double[2];

        Node node1 = db.getNodeById(link.getNode1Id());
        Node node2 = db.getNodeById(link.getNode2Id());
        d[0] = node2.getLat() - node1.getLat();
        d[1] = node2.getLng() - node1.getLng();

        double a = norm(d);
        double b = d[0] * (node1.getLat() - point.latitude) + d[1] * (node1.getLng() - point.longitude);
        double t =  - (b / a);

        if(t < 0) {
            t = 0.0;
        } else if (t > 1.0) {
            t = 1.0;
        }

        double x = t * d[0] + node1.getLat();
        double y = t * d[1] + node1.getLng();

        return new LatLng(x, y);
    }


    /**
     * リンクに上に射影できるか否かを確かめる
     * @param point
     * @param link
     * @return
     */
    public static boolean isProjectToLink(LatLng point, Link link) {
        Node node1 = db.getNodeById(link.getNode1Id());
        Node node2 = db.getNodeById(link.getNode2Id());

        return isProjectToLineSegment(point, node1.getLatLng(), node2.getLatLng());
    }

    /**
     * 線分上に射影できるか否かを確かめる
     * @param point
     * @param linePoint1
     * @param linePoint2
     * @return
     */
    public static boolean isProjectToLineSegment(LatLng point, LatLng linePoint1, LatLng linePoint2) {

        double d[] = new double[2];

        d[0] = linePoint2.latitude - linePoint1.latitude;
        d[1] = linePoint2.longitude - linePoint1.longitude;

        double a = norm(d);
        double b = d[0] * (linePoint1.latitude - point.latitude) + d[1] * (linePoint1.longitude - point.longitude);
        double t =  - (b / a);

        ////Log.v("MM", "method2 a:" + a + ", b:" + b + "t:" + t);
        if(t < 0) {
            return false;
        } else if (t > 1.0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 指定したLatLngの周囲を通るLinkのリストを取得する
     * 過去の位置情報がないときにスケルトンマッチングするためのリンクの候補を見つけるために使用する
     * @param point
     * @return
     */
    public static List<Link> getFirstCandidateLinkList(LatLng point) {
       return getLinkListByGridIdList(getSurroundGridList(point));
    }

    /**
     * 指定したGridIdのリストを通るLinkのリストを取得する
     * @param surroundGridList
     * @return
     */
    public static List<Link> getLinkListByGridIdList(List<String> surroundGridList) {
        List<Link> firstCandidateMatchingLinkList = new ArrayList<Link>();
        List<Integer> firstCandidateMatchingLinkIdList = new ArrayList<>();
        for (String gridId : surroundGridList) {
            List<Integer> firstCandidateMatchingLinkIdListInGrid = db.getLinkIdListByGridId(gridId);
            for (int linkId : firstCandidateMatchingLinkIdListInGrid) {
                if(!firstCandidateMatchingLinkIdList.contains(linkId)) {
                    firstCandidateMatchingLinkIdList.add(linkId);
                    firstCandidateMatchingLinkList.add(db.getLinkById(linkId));
                }
            }
        }
        return firstCandidateMatchingLinkList;
    }

    /**
     * 指定したLatLngの周囲9ます分のGridIdのリストを取得する
     * ある地点から上下左右に15mずつずらした地点を含めた9地点のGridIdを取得する(ごり押し!!)
     * @param point
     * @return
     */
    public static List<String> getSurroundGridList(LatLng point) {
        List<String> surroundGridList = new ArrayList<>();

        surroundGridList.add(PointInfoMeshcode.calcMeshCode(point.latitude, point.longitude, 9));
        LatLng movedPoint = calculateMovedPoint(point, 15, 0);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, 15, 15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, 0, 15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, -15, 15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, -15, 0);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, -15, -15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, 0, -15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));
        movedPoint = calculateMovedPoint(point, 15, -15);
        surroundGridList.add(PointInfoMeshcode.calcMeshCode(movedPoint.latitude, movedPoint.longitude, 9));

        //surroundGridList.add("0");//TODO:link_grid_infoが完成したら上のものに戻す

        return surroundGridList;
    }

    /**
     * pointから緯度に対してym、経度に対してxm動かした座標を取得する
     * @param point
     * @param x
     * @param y
     * @return
     */
    public static LatLng calculateMovedPoint(LatLng point, int x, int y) {
        double lat = point.latitude + (y /  (RX / 360));
        double lng = point.longitude + (x / (RY * Math.cos(Math.toRadians(point.latitude)) / 360));

        return  new LatLng(lat, lng);
    }

    /**
     * 指定したLinkとそれに隣接するLinkのリストを取得する
     * @param lastMatchedLink
     * @return
     */
    public static List<Link> getCandidateLinkList(Link lastMatchedLink) {
        List<Integer> candidateMatchingLinkIdList = db.getConnectingLinkIdListByLinkId(lastMatchedLink.getId());
        List<Link> candidateMatchingLinkList = new ArrayList<Link>();
        for (int linkId : candidateMatchingLinkIdList) {
            candidateMatchingLinkList.add(db.getLinkById(linkId));
        }
        return candidateMatchingLinkList;
    }

}
