package ubilabmapmatchinglibrary.statement;

import android.database.SQLException;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.pedestrianspacenetwork.Link;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.Node;
import ubilabmapmatchinglibrary.pedestrianspacenetwork.WallPoint;

/**
 * Created by shun on 2015/02/01.
 */
public class StatementDatabaseHelper {
    private Statement operationStatement;
    private ResultSet rs;
    public static final String NODE_TABLE = "node_info";    //テーブル名
    public static final String WALL_POINT_TABLE = "point_info"; //テーブル名
    public static final String LINK_TABLE = "link_info";    //テーブル名
    public static final String LINK_GRID_TABLE = "link_grid_info";//テーブル名

    public StatementDatabaseHelper(Statement statement){
        this.operationStatement = statement;
    }

    /**
     * queryによりNodeを一つ取得する
     * @param query
     * @return
     */
    public Node getNodeByQuery(String query) {

        Node node = null;
        try {
            rs = operationStatement.executeQuery(query);
            rs.next();;
            node = new Node(rs.getString("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), (int) rs.getDouble("level"), rs.getInt("type"), rs.getString("grid_id"));
            rs.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return node;
    }

    /**
     * NodeTableから指定したidのNodeを取得する
     * @param id
     * @return
     */
    public Node getNodeById(String id) {

        String GET_NODE_BY_ID =
                "select * from " + NODE_TABLE + " where id = " + id;

        try{
            Node node = getNodeByQuery(GET_NODE_BY_ID);
            return node;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * queryによりPointリストを取得する
     * 
     * @param query
     * @return
     */
    public List<WallPoint> getPointsByQuery(String query) {

        List<WallPoint> pointsList = new ArrayList<WallPoint>();
        try {
            rs = operationStatement.executeQuery(query);

            while (rs.next()) {
                pointsList.add(new WallPoint(rs.getString("id"), rs.getString("node_id"), rs.getString("\"group\""), rs.getInt("point_order"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("grid_id")));
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return pointsList;
    }

    /**
     * PointTableから指定したidのNodeに属するPointリストを取得する
     * @param nodeId
     * @return
     */
    public List<WallPoint> getPointsByNodeId(String nodeId) {

        String GET_POINTS_BY_NODE_ID =
                "select * from " + WALL_POINT_TABLE + " where node_id = '" + nodeId + "' order by point_order";

        try{

            List<WallPoint> pointsList = getPointsByQuery( GET_POINTS_BY_NODE_ID);
            
            return pointsList;

        } catch (SQLException e) {

            
            return null;
        }
    }

    /**
     * PointTableから指定したidのgroupに属するPointリストをpointOrder昇順で取得する
     * @param groupNumber
     * @return
     */
    public List<WallPoint> getPointsByGroupNumber(String groupNumber) {
        

        String GET_POINTS_BY_GROUP_ID =
                "select * from " + WALL_POINT_TABLE + " where \"group\" = '" + groupNumber + "' order by point_order";

        try{
            List<WallPoint> pointsList = (getPointsByQuery( GET_POINTS_BY_GROUP_ID));
            
            return pointsList;

        } catch (SQLException e) {
           ////Log.v("MM", "getPointsByGroupNumber" + e.toString());
            
            return null;
        }
    }


    /**
     * queryによりLinkを一つ取得する
     * 
     * @param query
     * @return
     */
    public Link getLinkByQuery(String query) {

        Link link = null;
        try {
            rs = operationStatement.executeQuery(query);
            rs.next();
            link = new Link(rs.getString("id"), rs.getString("node1_id"), rs.getString("node2_id"), rs.getDouble("distance"), rs.getDouble("bearing"), rs.getInt("type"), rs.getDouble("pressure"));
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return link;
    }

    /**
     * LinkTableから指定したidのLinkを取得する
     * @param id
     * @return
     */
    public Link getLinkById(String id) {
        

        String GET_LINK_BY_ID =
                "select * from " + LINK_TABLE + " where id like '" + id + "'";

        try{
            Link link = getLinkByQuery( GET_LINK_BY_ID);
            
            return link;

        } catch (SQLException e) {
            
            return null;
        }
    }

    /**
     * LinkTableから指定したidListのLinkをリストで取得する
     * @param idList
     * @return
     */
    public List<Link> getLinkListByIdList(List<String> idList) {
        List<Link> linkList= new ArrayList<Link>();
        if(idList != null) {
            for (String id : idList) {
                linkList.add(getLinkById(id));
            }
            return linkList;
        } else {
            return null;
        }
    }

    /**
     * queryによりLinkのidリストを取得する
     * 
     * @param query
     * @return
     */
    public List<String> getLinkIdListByQuery(String query) {

        List<String> linkIdList = new ArrayList<>();
        try {
            rs = operationStatement.executeQuery(query);

            while (rs.next()) {
                linkIdList.add(rs.getString(1));
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return linkIdList;
    }

    /**
     * LinkGridTableから指定したidのGridを通るようなLinkのidのリストを取得する
     * @param gridId
     * @return
     */
    public List<String> getLinkIdListByGridId(String gridId) {

        String GET_LINKS_ID_BY_GRID_ID =
                "select link_id from " + LINK_GRID_TABLE + " where grid_id like " + gridId;

        try{
            List<String> linksIdList = getLinkIdListByQuery( GET_LINKS_ID_BY_GRID_ID);
            
            return linksIdList;

        } catch (SQLException e) {
            
            return null;
        }
    }


    /**
     * LinkGridTableから指定したidのGridを通り、指定したlevelにあるノードを繋ぐLinkのidのリストを取得する
     * @param gridId
     * @param level
     * @return
     */
    public List<String> getLinkIdListByGridIdAndLevel(String gridId, int level) {
        

        //指定したgrid_idかつ、リンクを構成するノードが指定したlevelにあるlinkIdを取得するクエリ
        //要検証
        String GET_LINKS_BY_GRID_ID_AND_LEVEL =
                "select link_id from "+ LINK_GRID_TABLE + " where grid_id like " + gridId + "and link_id in (select id from "
                        +LINK_TABLE + " where (node1_id or node2_id) in (select id from " + NODE_TABLE + "where level = " + level +"))";

        try{
            List<String> linksIdList = getLinkIdListByQuery( GET_LINKS_BY_GRID_ID_AND_LEVEL);
            
            return linksIdList;

        } catch (SQLException e) {
            
            return null;
        }
    }

    /**
     * LinkGridTableから指定したidのGridを通り、指定したlevelにあるノード間を繋ぐ通路および広場のLinkのidのリストを取得する
     * 初期位置からスケルトンマッチングさせるリンクの候補を調べるために用いる
     * @param gridId
     * @param level
     * @return
     */
    public List<String> getLinkIdListByGridIdAndLevelAndType(String gridId, int level) {
        

        //指定したgrid_idかつ、リンクを構成するノードが指定したlevelにある、LinkTypeが0か1のlinkIdを取得するクエリ
        //要検証
        String GET_LINKS_BY_GRID_ID_AND_LEVEL_AND_TYPE =
                "select link_id from "+ LINK_GRID_TABLE + " where grid_id like " + gridId + "and link_id in (select id from " +
                        LINK_TABLE + " where ((node1_id or node2_id) in (select id from " + NODE_TABLE + "where level = " + level + ")) " +
                        "and type = (" + Link.LinkType.PASSAGE.ordinal() + " or " + Link.LinkType.OPEN_SPACE.ordinal() + "))";

        try{
            List<String> linksIdList = getLinkIdListByQuery( GET_LINKS_BY_GRID_ID_AND_LEVEL_AND_TYPE);
            
            return linksIdList;

        } catch (SQLException e) {
            
            return null;
        }
    }

    /**
     * LinkTableより指定したidのリンクと、それに隣接するリンクのIdリストを取得する
     * 前回のスケルトンマッチングの結果から、次にマッチさせるリンクの候補を調べるために用いる
     * @param linkId
     * @return
     */
    public List<String> getConnectingLinkIdListByLinkId(String linkId) {

        Link link = getLinkById(linkId);
        String node1Id = link.getNode1Id();
        String node2Id = link.getNode2Id();

        
        String GET_CONNECTING_LINK_ID_LIST_BY_LINK_ID =
                "select id from " + LINK_TABLE + " where node1_id = '" + node1Id + "' or node2_id = '" + node1Id + "' or node1_id = '" + node2Id + "' or node2_id = '" + node2Id + "'";

        try{
            List<String> linksIdList = getLinkIdListByQuery(GET_CONNECTING_LINK_ID_LIST_BY_LINK_ID);
            
            return linksIdList;
        } catch (SQLException e) {
            
            return null;
        }
    }

    /**
     * LinkTableより指定したidのグループ内にあるNodeから外部に出ているリンクのIdリストを取得する
     * 広場の出口のスケルトンマッチング候補を探す
     * @param groupNumber
     * @return
     */
    public List<String> getConnectingLinkIdListByGroupNumber(String groupNumber) {

        

        String GET_CONNECTING_LINK_ID_LIST_BY_GROUP_ID =
                "select id from " + LINK_TABLE + " where type = " + Link.LinkType.PASSAGE.ordinal() + " and " +
                        "(node1_id in (select node_id from " + WALL_POINT_TABLE + " where \"group\" = '" + groupNumber + "')" +
                        "or node2_id in (select node_id from " + WALL_POINT_TABLE + " where \"group\" = '" + groupNumber + "'))";

        try{
            List<String> linksIdList = getLinkIdListByQuery( GET_CONNECTING_LINK_ID_LIST_BY_GROUP_ID);
            
            return linksIdList;
        } catch (SQLException e) {
            
            return null;
        }
    }

    /**
     * 二つのlinkに共通するNodeを取得する
     * @param link1Id
     * @param link2Id
     * @return
     */
    public Node getLinksCommonNode(String link1Id, String link2Id) {

        String commonNodeId = "null";
        String GET_LINKS_COMMON_NODE_ID =
                "select node_id from" + LINK_TABLE  + "where (link1_id or link2_id) = '" + link1Id + "' or (link1_id or link2_id) = '" + link2Id + "')";

        List<WallPoint> pointsList = new ArrayList<WallPoint>();
        try {
            rs = operationStatement.executeQuery(GET_LINKS_COMMON_NODE_ID);
            rs.next();
            commonNodeId = rs.getString(1);
            rs.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        Node commonNode = getNodeById(commonNodeId);

        return commonNode;
    }
}
