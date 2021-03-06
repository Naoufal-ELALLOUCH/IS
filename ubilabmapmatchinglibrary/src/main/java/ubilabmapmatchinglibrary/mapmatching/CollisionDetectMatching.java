package ubilabmapmatchinglibrary.mapmatching;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.calculate.Calculator2D;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;

/**
 * Created by shun on 2015/01/09.
 */
public class CollisionDetectMatching extends TrajectoryTransedDetector{

    /**
     * 較正係数の範囲
     * 進行方向変化量の較正係数Rdの範囲は0.9倍～1.1倍
     * 歩幅の較正係数Rsの範囲は0.8～1.2倍
     */
    private static final int MIN_DIRECTION_RATE = 90;
    private static final int MAX_DIRECTION_RATE = 110;
    private static final int MIN_DISTANCE_RATE = 80;
    private static final int MAX_DISTANCE_RATE = 120;

    public static CollisionDetectMatchingHelper mCollisionDetectMatchingHelper;
    public static SkeletonMatching mSkeletonMatching;

    private boolean isFirst = false;

    /*生の軌跡*/
    private static Trajectory rawTrajectory = new Trajectory();

    /*ターンごとの軌跡の変換の元となる軌跡*/
    private static Trajectory baseTrajectory = new Trajectory();

    /*通路の始め、終わり時の歩数を格納するリスト*/
    private static List<Integer> passageFinishStepCount = new ArrayList<Integer>();
    private static List<Integer> passageStartStepCount = new ArrayList<Integer>();

    /*高精度な絶対座標が得られたかどうか*/
    private static boolean isHighAccuracyPoint = false;
    private static List<LatLng> highAccuracyPointList = new ArrayList<LatLng>();
    private static List<Integer> stepNumberList = new ArrayList<Integer>();

    //スケルトンマッチング後のTrackPoint
    private static TrackPoint skeletonMatchingTrackPoint = new TrackPoint();
    private static TrackPoint lastSkeletonMatchingTrackPoint = new TrackPoint();

    //マップマッチング後のTrackPoint
//    private static TrackPoint transedTrackPoint = new TrackPoint();
    private static TrackPoint lastTransformedTrackPoint = new TrackPoint();

//    Trajectory transedTrajectory = new Trajectory();

    //直前のリンクのIdを格納
    private String lastLinkId;

    private String lastStraightLinkId = "null";

   //較正係数の組
    Point correctRate = new Point();

    //ルート順のリンクを格納するリスト
    private List<Link> linkList = new ArrayList<>();

    private int turnCount = 0;
    private int turnedStepCount = 0;
    private double lastDirectionRate = 1.0;
    private double lastDistanceRate = 1.0;

    private Trajectory turningTrajectory = new Trajectory();

    List<Point> rateSet;

    /**
     * コンストラクタ
     */
    public CollisionDetectMatching(Context context, DatabaseHelper db) {
        mSkeletonMatching = new SkeletonMatching(context, db);
        mCollisionDetectMatchingHelper = new CollisionDetectMatchingHelper(context, db);
        isFirst = true;

        this.rateSet = new ArrayList<>();
        this.originLatLngArray = new ArrayList<>();
        this.adjustedLatLngArray = new ArrayList<>();
        this.straightStepRatePlusMinus = new ArrayList<>();
    }

    /**
     * 与えられたTrackPointを基に最新のスケルトンマッチング後のTrackPoint(座標や、方角、マッチングしているLinkIdの情報)を算出するクラス
     * @param trackPoint
     * @return
     */
    public TrackPoint calculateCollisionDetectMatchingPosition(TrackPoint trackPoint) {
            rawTrajectory.add(trackPoint);

            TrackPoint smTrackPoint = mSkeletonMatching.calculateSkeletonMatchingPosition(trackPoint);
            if (smTrackPoint == null) {
                return null;
            } else {
                skeletonMatchingTrackPoint.setTrackPoint(smTrackPoint);
            }
            Link matchingLink = mCollisionDetectMatchingHelper.db.getLinkById(skeletonMatchingTrackPoint.getLinkId());

//            Log.v("CM", "linkId:" + matchingLink.getId());
            trackPoint.setLinkId(matchingLink.getId());

            if (!isFirst) {
                if (trackPoint.getIsStraight()) {
                    if (!lastSkeletonMatchingTrackPoint.getIsStraight()) { //曲進途中
                        if (!lastStraightLinkId.equals(matchingLink.getId())) {
                            if (turnCount > 0) {
                                rawTrajectory.removeTrajectory(turnedStepCount);
                                adjustStepNumberOfHighAccuracyPoints(turnedStepCount);
                                linkList.remove(0);
                            }

                            turnCount++;
                            turnedStepCount = rawTrajectory.size() - 1;
                        } else {
                            passageFinishStepCount.remove(passageFinishStepCount.size() - 1);
                        }
                    }
//                } else if (lastSkeletonMatchingTrackPoint.getIsStraight()) { //直進中 //実質要らないのでは //これのせいでlastStraightLinkIdが更新されず、wallInfoが過去3つ分とかになってる？
                } else {
                /*通路終了時の歩数*/
                    passageFinishStepCount.add(rawTrajectory.size() - 1);
                    lastStraightLinkId = lastLinkId;
                }

                if (!lastLinkId.equals(matchingLink.getId())) {
                    if (linkList.size() > 1) {

                        String lastCommonNodeId = mCollisionDetectMatchingHelper.getLinksCommonNodeId(linkList.get(linkList.size() - 2), linkList.get(linkList.size() - 1));
                        String commonNodeId = mCollisionDetectMatchingHelper.getLinksCommonNodeId(linkList.get(linkList.size() - 2), matchingLink);
                        if (lastCommonNodeId.equals(commonNodeId)) {
                            linkList.remove(linkList.size() - 1);
                        }
                    }
                    linkList.add(matchingLink);
                }

                //軌跡が衝突しているか否かの判定処理
                boolean isCollision = false;
                if (trackPoint.getIsStraight()) {

                    if (!lastTransformedTrackPoint.getIsStraight()) {
                        if (!(isCollision = isCollisionDetectLinkWall(turningTrajectory, matchingLink))) {
                            if (linkList.size() > 1) {
                                //isCollision = isCollisionDetectLinkWall(turningTrajectory, linkList.get(linkList.size() - 2));
                                List<Link> turningLinkList = new ArrayList<>();
                                turningLinkList.add(linkList.get(linkList.size() - 2));
                                turningLinkList.add(linkList.get(linkList.size() - 1));
                                List<List<LatLng>> wallInfo = mCollisionDetectMatchingHelper.getLinksWallInfo(turningLinkList);
                                isCollision = mCollisionDetectMatchingHelper.detectCollisionWithWallAndTrajectory(rawTrajectory, wallInfo);
                            }
                        }
                    } else {
                        isCollision = isCollisionDetectLinkWall(trackPoint.getLocation(), lastTransformedTrackPoint.getLocation(), matchingLink);
                    }
                    turningTrajectory.clear();
                } else {
                    turningTrajectory.add(trackPoint);

                }

                //衝突
                if (isCollision) {
                    rateSet.clear();
                    rateSet.addAll(getRateSetNotCollideLinksWall(rawTrajectory, linkList));

                    //壁に当たらないような変換ができないとき、初期値をスケルトンマッチングのものに切り替える
                    if (rateSet.size() == 0) {
                        LatLng skeletonMatchingPoint = mCollisionDetectMatchingHelper.getProjectedPoint(trackPoint.getLocation(), matchingLink);
                        double matchingDirection = getMatchingLinkDirection(trackPoint.getDirection(), matchingLink);
                        trackPoint.setLocation(skeletonMatchingPoint);
                        trackPoint.setDirection(matchingDirection);
                        trackPoint.polylineColor = Color.CYAN;
                        trackPoint.isSkeletonMatch = true;
//                        newStartTrackPoint = trackPoint; //new~は使ってない？

                        rawTrajectory.clear();
                        rawTrajectory.add(trackPoint);

                        linkList.clear();
                        linkList.add(matchingLink);

                        turnedStepCount = 0;
                        turnCount = 0;
                        highAccuracyPointList.clear();

                    } else {//壁に当たらないように変換できるような較正係数の組から尤もらしいものを選択し、変換を行う
                        if (isHighAccuracyPoint) {
                      /*最もマッチングするリンクに近い軌跡に変換する較正係数の組を取得する*/
                            correctRate.set(getTransedTrajectoryToPassHighAccuracyPoints(rawTrajectory, rateSet, highAccuracyPointList, stepNumberList));
                        } else {
                            correctRate = getCentroid(rateSet);
                        }

                        originLatLngArray.add(rawTrajectory.get(rawTrajectory.size()-1).getLocation());
                        rawTrajectory.transTrack(correctRate); //生の軌跡を補正 //クラスフィールド上書き
                        adjustedLatLngArray.add(rawTrajectory.get(rawTrajectory.size()-1).getLocation());

                        if (correctRate.getY() < 1) {
                            straightStepRatePlusMinus.add(false);
                        } else {
                            straightStepRatePlusMinus.add(true);
                        }

                        lastDirectionRate = correctRate.getX() * lastDirectionRate;
                        lastDistanceRate = correctRate.getY() * lastDistanceRate;
                        trackPoint = rawTrajectory.get(rawTrajectory.size() - 1);
                        trackPoint.polylineColor = Color.BLACK;
                        trackPoint.isSkeletonMatch = false;
                        ////Log.v("CM", "{Rd,Rs} = {" + lastDirectionRate + ", " + lastDistanceRate + "}");
                    }

                    for (TrajectoryTransformedListener listener: mTrajectoryTransedListeners) {
                        listener.onTrajectoryTransed(new Point(lastDirectionRate, lastDistanceRate), rawTrajectory, trackPoint);
                    }
                    mSkeletonMatching.setFirst();

                }
            } else {
                isFirst = false;
                passageFinishStepCount.add(0);
                linkList.add(matchingLink);
                ////Log.v("CM", "first");
            }

            lastSkeletonMatchingTrackPoint.setTrackPoint(skeletonMatchingTrackPoint);
            lastTransformedTrackPoint.setTrackPoint(trackPoint);

            lastLinkId = matchingLink.getId();

            return trackPoint;
    }
    public ArrayList<LatLng> originLatLngArray;
    public ArrayList<LatLng> adjustedLatLngArray;
    public ArrayList<Boolean> straightStepRatePlusMinus;

    public static List<Integer> getPassageFinishStepCount() {
        return passageFinishStepCount;
    }

    /**
     * 壁にぶつからないように変換できる較正係数の組のセットを取得する
     * @param trajectory
     * @param linkList
     * @return
     */
   private List<Point> getRateSetNotCollideLinksWall(Trajectory trajectory, List<Link> linkList) {
       List<Point> rateSet = new ArrayList<Point>();

       List<List<LatLng>> linksWall =  mCollisionDetectMatchingHelper.getLinksWallInfo(linkList);

        /*ループ用にintで表記*/
       int intDirectionRate = MIN_DIRECTION_RATE;
       int intDistanceRate = MIN_DISTANCE_RATE;

       //ループさせて各較正係数の組について調べる
       while (intDirectionRate <= MAX_DIRECTION_RATE) {
           double directionRate = (double)intDirectionRate / 100.0/ lastDirectionRate;

           while (intDistanceRate <= MAX_DISTANCE_RATE) {
               double distanceRate = (double)intDistanceRate / 100.0/ lastDistanceRate;

               Trajectory transformedTrajectory = new Trajectory();
               transformedTrajectory.addAll(trajectory);

               transformedTrajectory.transTrack(directionRate, distanceRate);
               if (!mCollisionDetectMatchingHelper.detectCollisionWithWallAndTrajectory(transformedTrajectory, linksWall)) {
                   rateSet.add(new Point(directionRate , distanceRate));
               }

               intDistanceRate++;
           }
           intDistanceRate = MIN_DISTANCE_RATE;
           intDirectionRate++;
       }

       ////Log.v("CM","RateSetSize:" + rateSet.size());
       return rateSet;

   }

    /**
     * ある1歩が壁とぶつかるか判定する
     * @param point
     * @param lastPoint
     * @param wall
     * @return
     */
    private boolean isCollisionDetectWall(LatLng point, LatLng lastPoint, List<List<LatLng>> wall) {

        for(List<LatLng> sideWall: wall) {
            isFirst = true;
            LatLng lastWallPoint = null;
            for (LatLng wallPoint: sideWall) {
                if(isFirst) {
                    isFirst = false;
                } else {
                    if(Calculator2D.isCrossed2Line(point, lastPoint, wallPoint, lastWallPoint)) {
                        return true;
                    }

                }
                lastWallPoint = wallPoint;
            }
        }
        return false;
    }

    /**
     * ある1歩がリンクの両サイドの壁とぶつかるか判定する
     * @param point
     * @param lastPoint
     * @param link
     * @return
     */
    private boolean isCollisionDetectLinkWall(LatLng point, LatLng lastPoint, Link link) {
        List<List<LatLng>> linkWall = mCollisionDetectMatchingHelper.getLinkWallInfo(link);
        return isCollisionDetectWall(point, lastPoint, linkWall);
    }

    /**
     * ある軌跡がリンクの両サイドの壁とぶつかるか判定する
     * @param trajectory
     * @param link
     * @return
     */
    private boolean isCollisionDetectLinkWall(Trajectory trajectory, Link link) {
        List<List<LatLng>> linkWall = mCollisionDetectMatchingHelper.getLinkWallInfo(link);
        LatLng lastPoint = null;
        for(TrackPoint trackPoint: trajectory.getTrajectory()) {
            if(lastPoint != null) {
                if(isCollisionDetectWall(trackPoint.getLocation(), lastPoint, linkWall)) {
                    return true;

                }
            }
            lastPoint = trackPoint.getLocation();
        }
        return false;
    }

    /**
     * Pointのリストの重心を取得する
     * @param rateSet
     * @return
     */
    public static Point getCentroid(List<Point> rateSet) {

        if(rateSet.size() == 0) {
            return  null;
        }
        double rd = 0;
        double rs = 0;

        for(Point rate : rateSet) {
            rd += rate.getX();
            rs += rate.getY();
        }
        rd /= rateSet.size();
        rs /= rateSet.size();

        return new Point(rd, rs);

    }

    /**
     * Wi-Fiなどで取得できた高精度で測位結果の座標を通るような軌跡に変換する貯めの較正係数の組を取得する.
     * n歩目における変換後の軌跡と、高精度な測位結果の距離スコアを算出し、それらをすべて乗算したトータルスコアが最大になるものを採用する.
     * @param baseTrajectory
     * @param rateSet
     * @param highAccuracyPointsList
     * @param stepNumberList
     * @return
     */
    public static Point getTransedTrajectoryToPassHighAccuracyPoints(Trajectory baseTrajectory, List<Point> rateSet, List<LatLng> highAccuracyPointsList, List<Integer> stepNumberList) {
        double maxScore = 0;
        Point maxScoreRate =  new Point();
        for(Point rate : rateSet) {
            baseTrajectory.transTrack(rate);
            double score = 0;
            double totalScore = 0;

            int i = 0;
            for(int stepNumber : stepNumberList) {
                score = mCollisionDetectMatchingHelper.calculateDistanceScore(Calculator2D.calculateDistance(baseTrajectory.get(stepNumber).getLocation(), highAccuracyPointsList.get(i)));
                if(totalScore == 0) {
                    totalScore = score;
                } else {
                    totalScore *= score;
                }
                i++;
            }

            if(maxScore < score) {
                maxScore = totalScore;
                maxScoreRate = rate;
            }
        }

        return maxScoreRate;
    }


    /**
     * 精度の高い測位地点をセットする
     * @param highAccuracyPoint
     */
    public static void  setHighAccuracyPoint(LatLng highAccuracyPoint, long time) {
        int stepCount = rawTrajectory.size() - 1;
        for(int i = (rawTrajectory.size() - 1); i > 0; i--) {
            if(rawTrajectory.get(i).getTime() < (time - 1000000000)) { //1秒以上前の地点
                stepCount = i;
            }
        }
        stepNumberList.add(stepCount);
        highAccuracyPointList.add(highAccuracyPoint);
    }

    /**
     * ターンの切り替えによって、HighAccuracyPointListとStepNumberリストの更新を行う
     * @param newTrajectoryStartStepNumber
     */
    public static void adjustStepNumberOfHighAccuracyPoints(int newTrajectoryStartStepNumber) {
        int i = 0;
        for(int stepNumber : stepNumberList) {

            if(stepNumber < newTrajectoryStartStepNumber) {
                stepNumberList.remove(i);
                highAccuracyPointList.remove(i);
            } else {
                int newStepNumber = stepNumber - newTrajectoryStartStepNumber;
                stepNumberList.set(i, newStepNumber);
            }
            i++;
        }

        if(stepNumberList.size() ==0) {
            isHighAccuracyPoint = false;
        }
    }

    /**
     * 推定進行方向をマッチングリンクにマッチングさせた際の進行方向を取得する
     * @param rawDirection
     * @param matchingLink
     * @return
     */
    public double getMatchingLinkDirection(double rawDirection, Link matchingLink){
        if(Math.cos(Math.toRadians(matchingLink.getBearing()) - Math.toRadians(rawDirection)) < 0) {
            return matchingLink.getBearing() + 180;
        } else {
            return matchingLink.getBearing();
        }
    }

    public List<Link> getLinkList() {
        return linkList;
    }

    public void removeOldTrajectory(){
        rawTrajectory.getTrajectory().clear();
    }
}

