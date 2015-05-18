package ubilabmapmatchinglibrary.mapmatching;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.calculate.Calculator2D;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Node;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.WallPoint;

/**
 * Created by shun on 2015/01/09.
 */
public class CollisionDetectMatchingHelper extends SkeletonMatchingHelper {
    public CollisionDetectMatchingHelper(Context context, DatabaseHelper db) {
        super(context, db);
    }

    /**
     * リンクを囲う壁の情報を取得する
     *
     * @param link
     * @return
     */
    public static List<List<LatLng>> getLinkWallInfo(Link link) {

        List<List<LatLng>> linkWall = new ArrayList<List<LatLng>>();
        List<LatLng> oneSideWall = new ArrayList<LatLng>();
        List<LatLng> anotherSideWall = new ArrayList<LatLng>();

//        Log.v("WallTest", "link.getType" + link.getType() + ", " + link.getType().ordinal());
        if (link.getType() == Link.LinkType.OPEN_SPACE) {
            if (db.getPointsByNodeId(link.getNode1Id()).size() == 0) {
                return linkWall;
            }
            List<WallPoint> openSpacePointList = db.getPointsByGroupNumber(db.getPointsByNodeId(link.getNode1Id()).get(0).getGroupNumber());

            List<String> openSpaceNodeList = new ArrayList<>();

            List<WallPoint> wallPointListA = new ArrayList<>();
            List<WallPoint> wallPointListB = new ArrayList<>();

            for (WallPoint wallPoint : openSpacePointList) {
//                Log.v("CM", "wallPoint.getId:" + wallPoint.getId() + "wallPoint.getNodeId:" + wallPoint.getNodeId());
                if (!wallPoint.getNodeId().equals("null")) {
                    if (!openSpaceNodeList.contains(wallPoint.getNodeId())) {
                        wallPointListA.add(wallPoint);
                        openSpaceNodeList.add(wallPoint.getNodeId());
                    } else {
                        wallPointListB.add(wallPoint);
                    }
                }
            }

            if (wallPointListA.get(0).getNodeId().equals(wallPointListB.get(0).getNodeId())) {
                wallPointListA.add(wallPointListA.get(0));
                wallPointListA.remove(0);

                for (int i = 0; i < wallPointListA.size(); i++) {
                    List<LatLng> wall = getOneSideIntersectionWall(openSpacePointList, wallPointListB.get(i).getPointOrder(), wallPointListA.get(i).getPointOrder(), true);
                    linkWall.add(wall);
                }
            } else {
                for (int i = 0; i < wallPointListA.size(); i++) {
                    List<LatLng> wall = getOneSideIntersectionWall(openSpacePointList, wallPointListA.get(i).getPointOrder(), wallPointListB.get(i).getPointOrder(), true);
                    linkWall.add(wall);
                }
            }

        } else { //通路リンクの壁
            if (db.getPointsByNodeId(link.getNode1Id()).size() == 0 || db.getPointsByNodeId(link.getNode2Id()).size() == 0) {
                return linkWall;
            }
            List<WallPoint> startPointList = db.getPointsByGroupNumber(db.getPointsByNodeId(link.getNode1Id()).get(0).getGroupNumber());
            List<WallPoint> goalPointList = db.getPointsByGroupNumber(db.getPointsByNodeId(link.getNode2Id()).get(0).getGroupNumber());
            //List<WallPoint> goalPointList = db.getPointsByNodeId(link.getNode2Id());

            WallPoint[] linkStartPoints = getCrossLinkPoints(link, startPointList);
            WallPoint[] linkGoalPoints = getCrossLinkPoints(link, goalPointList);

            if (linkStartPoints[1] == null) {
//                Log.v("hogehoge", "piyopiyo");
            }

            if (!Calculator2D.isCrossed2Line(linkStartPoints[0].getLatng(), linkGoalPoints[0].getLatng(), linkStartPoints[1].getLatng(), linkGoalPoints[1].getLatng())) {
                oneSideWall.add(linkStartPoints[0].getLatng());
                oneSideWall.add(linkGoalPoints[0].getLatng());
                anotherSideWall.add(linkStartPoints[1].getLatng());
                anotherSideWall.add(linkGoalPoints[1].getLatng());
            } else {
                oneSideWall.add(linkStartPoints[0].getLatng());
                oneSideWall.add(linkGoalPoints[1].getLatng());
                anotherSideWall.add(linkStartPoints[1].getLatng());
                anotherSideWall.add(linkGoalPoints[0].getLatng());
            }

            linkWall.add(oneSideWall);
            linkWall.add(anotherSideWall);
        }

        return linkWall;
    }

    /**
     * firstLinkからnextLinkに向かう際の壁の情報を格納するリストを返す
     * List.get(0)が左側の壁、List.get(1)が右側の壁を示す
     *
     * @param firstLink
     * @param nextLink
     * @return
     */
    public static List<List<LatLng>> getLinksWallInfo(Link firstLink, Link nextLink) {

        List<List<LatLng>> wallInfo = new ArrayList<List<LatLng>>();
        List<LatLng> leftWallInfo = new ArrayList<LatLng>();
        List<LatLng> rightWallInfo = new ArrayList<LatLng>();

        String commonNodeId = getLinksCommonNodeId(firstLink, nextLink);
        String startNodeId = getAnotherLinksNodeId(firstLink, commonNodeId);
        String goalNodeId = getAnotherLinksNodeId(nextLink, commonNodeId);

        if (db.getPointsByNodeId(startNodeId).size() == 0 || db.getPointsByNodeId(commonNodeId).size() == 0 || db.getPointsByNodeId(goalNodeId).size() == 0) {
            return wallInfo;
        }

        List<WallPoint> startPointList = db.getPointsByGroupNumber(db.getPointsByNodeId(startNodeId).get(0).getGroupNumber());
        List<WallPoint> commonPointList = db.getPointsByGroupNumber(db.getPointsByNodeId(commonNodeId).get(0).getGroupNumber());
        List<WallPoint> goalPointList = db.getPointsByGroupNumber(db.getPointsByNodeId(goalNodeId).get(0).getGroupNumber());

        WallPoint[] firstLinkStartPoints = getCrossLinkPoints(firstLink, startPointList);
        WallPoint[] firstLinkGoalPoints = getCrossLinkPoints(firstLink, commonPointList);
        WallPoint[] nextLinkStartPoints = getCrossLinkPoints(nextLink, commonPointList);
        WallPoint[] nextLinkGoalPoints = getCrossLinkPoints(nextLink, goalPointList);

        if (firstLink.getType() == Link.LinkType.OPEN_SPACE || nextLink.getType() == Link.LinkType.OPEN_SPACE) {
            wallInfo.addAll(getLinkWallInfo(firstLink));
            wallInfo.addAll(getLinkWallInfo(nextLink));

        } else {

            //交差点の壁の右側を表すリスト
            List<LatLng> intersectionRightWall = getOneSideIntersectionWall(commonPointList, firstLinkGoalPoints[1].getPointOrder(), nextLinkStartPoints[0].getPointOrder(), true);

            //交差点の壁の左側を表すリスト
            List<LatLng> intersectionLeftWall = getOneSideIntersectionWall(commonPointList, firstLinkGoalPoints[0].getPointOrder(), nextLinkStartPoints[1].getPointOrder(), false);

            rightWallInfo.add(firstLinkStartPoints[0].getLatng());
            rightWallInfo.addAll(intersectionRightWall);
            rightWallInfo.add(nextLinkGoalPoints[1].getLatng());

            leftWallInfo.add(firstLinkStartPoints[1].getLatng());
            leftWallInfo.addAll(intersectionLeftWall);
            leftWallInfo.add(nextLinkGoalPoints[0].getLatng());

            wallInfo.add(rightWallInfo);
            wallInfo.add(leftWallInfo);
        }
        return wallInfo;
    }

    /**
     * linkListの両サイドの壁の情報を取得する
     *
     * @param linkList
     * @return
     */
    public List<List<LatLng>> getLinksWallInfo(List<Link> linkList) {
        for (Link link : linkList) {
//            Log.v("CM", "getWallLink linkId:" + link.getId());
        }
//        Log.v("CM", "Link Size : " + linkList.size());

        int linkSize = linkList.size();
        if (linkSize == 0) {
            return null;
        } else if (linkSize == 1) {
            return getLinkWallInfo(linkList.get(0));
        } else if (linkSize == 2) {
            return getLinksWallInfo(linkList.get(0), linkList.get(1));
        } else {
            List<List<LatLng>> linksWallInfo = new ArrayList<List<LatLng>>();
            List<Link> passageLinkList = new ArrayList<>();
            for (Link link : linkList) {

//                Log.v("CM", "linkList linkId:" + link.getId());
                if (link.getType() == Link.LinkType.OPEN_SPACE) {
                    if (passageLinkList.size() != 0) {
                        linksWallInfo.addAll(getLinksWallInfo(passageLinkList));
                        linksWallInfo.addAll(getLinkWallInfo(link));
                        passageLinkList.clear();
                    } else {
                        linksWallInfo.addAll(getLinkWallInfo(link));
                    }
                } else {
                    passageLinkList.add(link);
                }
            }


            linkSize = passageLinkList.size();
//            Log.v("CM", "passageLinkListSize:" + linkSize);
            if (linkSize == 0) {
                return linksWallInfo;
            } else if (linkSize < 3) {
                linksWallInfo.addAll(getLinksWallInfo(passageLinkList));
                return linksWallInfo;
            }
            for (Link link : passageLinkList) {
//                Log.v("CM", "passagesLinkList linkId:" + link.getId());
            }
            List<Node> nodeList = getNodeListByLinkList(passageLinkList);
            //リンクの左側の壁を表すリスト
            List<LatLng> leftSideWall = new ArrayList<LatLng>();

            //リンクの右側の壁を表すリスト
            List<LatLng> rightSideWall = new ArrayList<LatLng>();

            Link lastLink = null;
            for (int i = 0; i < passageLinkList.size(); i++) {
                Link link = passageLinkList.get(i);
                List<WallPoint> commonPointList = db.getPointsByNodeId(nodeList.get(i).getId());

                if (lastLink == null) {
                    List<WallPoint> startPointList = db.getPointsByNodeId(nodeList.get(0).getId());
                    WallPoint[] firstLinkStartPoints = getCrossLinkPoints(link, startPointList);
                    leftSideWall.add(firstLinkStartPoints[1].getLatng());
                    rightSideWall.add(firstLinkStartPoints[0].getLatng());
                } else {

                    WallPoint[] firstLinkGoalPoints = getCrossLinkPoints(lastLink, commonPointList);
                    WallPoint[] nextLinkStartPoints = getCrossLinkPoints(link, commonPointList);

                    if (firstLinkGoalPoints.length == 0 || nextLinkStartPoints.length == 0) {
                        return linksWallInfo;
                    }

                    ////Log.v("CM","firstGoal[0]:" + firstLinkGoalPoints[0].getId() + ", firstGoal[1]:" + firstLinkGoalPoints[1].getId());
                    ////Log.v("CM","nextStart[0]:" + nextLinkStartPoints[0].getId() + ", nextStart[1]:" + nextLinkStartPoints[1].getId());

                    //交差点の壁の右側を表すリスト
                    List<LatLng> intersectionRightWall = getOneSideIntersectionWall(commonPointList, firstLinkGoalPoints[1].getPointOrder(), nextLinkStartPoints[0].getPointOrder(), true);

                    //交差点の壁の左側を表すリスト
                    List<LatLng> intersectionLeftWall = getOneSideIntersectionWall(commonPointList, firstLinkGoalPoints[0].getPointOrder(), nextLinkStartPoints[1].getPointOrder(), false);

                    leftSideWall.addAll(intersectionLeftWall);
                    rightSideWall.addAll(intersectionRightWall);

                    if (i == (linkSize - 1)) {
                        List<WallPoint> goalPointList = db.getPointsByNodeId(nodeList.get(linkSize).getId());
                        WallPoint[] nextLinkGoalPoints = getCrossLinkPoints(link, goalPointList);
                        leftSideWall.add(nextLinkGoalPoints[0].getLatng());
                        rightSideWall.add(nextLinkGoalPoints[1].getLatng());
                    }

                }
                lastLink = link;
            }
            linksWallInfo.add(rightSideWall);
            linksWallInfo.add(leftSideWall);
            return linksWallInfo;
        }

    }

    /**
     * pointList(グループ)の中からのリンクと交差するような隣接するWallPointの組み合わせを取得する
     *
     * @param link
     * @param pointList
     * @return
     */
    public static WallPoint[] getCrossLinkPoints(Link link, List<WallPoint> pointList) {

        LatLng node1 = db.getNodeById(link.getNode1Id()).getLatLng();
        LatLng node2 = db.getNodeById(link.getNode2Id()).getLatLng();

        int nextPointId = 1;
        for (int pointId = 0; pointId < pointList.size(); pointId++) {
            if (nextPointId == pointList.size()) {
                nextPointId = 0;
            }
            LatLng point1 = pointList.get(pointId).getLatng();
            LatLng point2 = pointList.get(nextPointId).getLatng();

            if (Calculator2D.isCrossed2Line(point1, point2, node1, node2)) {
                WallPoint[] crossLinkPoint = {pointList.get(pointId), pointList.get(nextPointId)};
                return crossLinkPoint;
            }

            nextPointId++;
        }
        return null;
    }

    /**
     * 二つのリンクに共通するノードのIdを取得する
     *
     * @param link1
     * @param link2
     * @return
     */
    public static String getLinksCommonNodeId(Link link1, Link link2) {
        String link1Node1Id = link1.getNode1Id();
        String link1Node2Id = link1.getNode2Id();
        String link2Node1Id = link2.getNode1Id();
        String link2Node2Id = link2.getNode2Id();

        String commonNodeId;
        if ((link1Node1Id.equals(link2Node1Id)) || (link1Node1Id.equals(link2Node2Id))) {
            commonNodeId = link1Node1Id;
        } else if ((link1Node2Id.equals(link2Node1Id)) || (link1Node2Id.equals(link2Node2Id))) {
            commonNodeId = link1Node2Id;
        } else {
            commonNodeId = "null";
        }

        return commonNodeId;

    }

    /**
     * リンクが繋ぐ2つのノードのうち指定したノードIdではない方のノードIdを取得する
     *
     * @param link
     * @param nodeId
     * @return
     */
    public static String getAnotherLinksNodeId(Link link, String nodeId) {
        if (link.getNode1Id().equals(nodeId)) {
            return link.getNode2Id();
        } else {
            return link.getNode1Id();
        }

    }

    /**
     * pointListからpointOrderがstartPointOrderからgoalPointOrderまでのWallPointを格納したリストを返す
     * orderがtrueの場合昇順(時計回り)で、falseの場合逆順(反時計回り)で見る
     *
     * @param pointList
     * @param startPointOrder
     * @param goalPointOrder
     * @param order
     * @return
     */
    public static List<LatLng> getOneSideIntersectionWall(List<WallPoint> pointList, int startPointOrder, int goalPointOrder, boolean order) {

        int next;
        if (order) {
            next = 1;
        } else {
            next = -1;
        }

        List<LatLng> oneSideIntersectionWall = new ArrayList<>();
        int pointOrder = startPointOrder;

        while (pointOrder != goalPointOrder) {
            oneSideIntersectionWall.add(pointList.get(pointOrder - 1).getLatng());
            pointOrder += next;

            if (pointOrder > pointList.size()) {
                pointOrder = 1;
            } else if (pointOrder == 0) {
                pointOrder = pointList.size();
            }
        }

        oneSideIntersectionWall.add(pointList.get(pointOrder - 1).getLatng());

        return oneSideIntersectionWall;
    }

    /**
     * 指定されたLinkのリストの順番でたどるNodeをリストを取得する
     *
     * @param linkList
     * @return
     */
    public List<Node> getNodeListByLinkList(List<Link> linkList) {
        List<Node> nodeList = new ArrayList<Node>();
        Link lastLink = null;
        String nextNodeId = null;
        for (Link link : linkList) {
            if (lastLink != null) {
                if (nodeList.size() == 0) {
                    nextNodeId = getLinksCommonNodeId(link, lastLink);
                    String firstNodeId = getAnotherLinksNodeId(lastLink, nextNodeId);
                    nodeList.add(db.getNodeById(firstNodeId));
                    nodeList.add(db.getNodeById(nextNodeId));
                }
                nextNodeId = getAnotherLinksNodeId(link, nextNodeId);
                nodeList.add(db.getNodeById(nextNodeId));

            }
            lastLink = link;
        }
        return nodeList;
    }

    /**
     * 軌跡と壁との衝突判定を行う
     * 軌跡が壁と交差する場合falseを、交差しない場合trueを返す
     *
     * @param trajectory
     * @param wallInfo
     * @return
     */
    public static boolean detectCollisionWithWallAndTrajectory(Trajectory trajectory, List<List<LatLng>> wallInfo) {
        LatLng lastPoint = null;
        int i = 0;
        for (TrackPoint track : trajectory.getTrajectory()) {
            LatLng point = track.getLocation();
            if (lastPoint != null) {
                for (List<LatLng> sideWallList : wallInfo) {
                    LatLng wallPointB = null;
                    for (LatLng wallPointA : sideWallList) {
                        if (wallPointB != null) {
                            if (Calculator2D.isCrossed2Line(wallPointA, wallPointB, lastPoint, point)) {
                                return true;
                            }
                        }
                        wallPointB = wallPointA;
                    }
                }
            }
            lastPoint = track.getLocation();
            i++;
        }
        return false;
    }

}
