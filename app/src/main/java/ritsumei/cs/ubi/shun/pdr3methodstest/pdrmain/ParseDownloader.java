package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.content.Context;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import ubilabmapmatchinglibrary.pedestrianspacenetwork.DatabaseHelper;

//TODO デフォルトのクエリが1000件までしか取れないのでそのあたりの処理
public class ParseDownloader {
//TODO シングルトン?

    private static int mapDBversion = 0;
    private static int wirelessDBversion = 0;
    //TEST  あとでコミットしないようにする
//    private String applicationId = "UkItGXeEkdJFjOn6YZLFx6mcgCW9myTUkKHfDRQ4";
//    private String masterKey = "Cqu6rPIcnJcp8h96hfzQyunIP95aHsreCwZcDxLB";

    //UBINavi
    private String applicationId = "re7vO6cofgV6cYPr8tMWUNpihQwCf039KqRHz13r";
    private String masterKey = "qjMpflGSoZ6a8Di3XoTU1hBoeNJGpePkzioBaQvZ";

    private String areaName = "梅田地下街";
    private int areaID;

    private DatabaseHelper mapDatabaseHelper;

    private Context context;


    public ParseDownloader(Context context) {
        Parse.initialize(context, applicationId, masterKey);
        this.context = context;
    }

    public static void setDBversion(int mapVersion, int wirelessVersion) {
        mapDBversion = mapVersion;
        wirelessDBversion = wirelessVersion;
    }

    public static void incrementDBVersion() {
        mapDBversion++;
        wirelessDBversion++;
    }

    public static int getMapDBVerison(){
        return  mapDBversion;
    }

    public static int getWirelessDBversion(){
        return  wirelessDBversion;
    }

    /**
     * parse.comからダウンロード
     * ローカルデータベースに保存
     */
    public void startDownLoad() {
        //Helperの作成

        Log.v("Downloader", "mapDBVersion:" + mapDBversion);
        mapDatabaseHelper = DatabaseHelper.getInstance(context, mapDBversion);
        try {
            //エリアIDの取得
            ParseQuery<ParseObject> query = ParseQuery.getQuery("area");
            query.whereEqualTo("name", areaName);
            List<ParseObject> idList = query.find();
            areaID = idList.get(0).getInt("area_id");

            //pdr
            //DatabaseHelper.NODE_TABLE
            readPedestrianNode();

            //DatabaseHelper.LINK_TABLE
            readPedestrianLink();

            //DatabaseHelper.WALL_POINT_TABLE
            readPedestrianPoint();

            //DatabaseHelper.LINK_GRID_TABLE
            readPedestrianGrid();

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }




    private void readPedestrianNode() throws ParseException {
        ParseQuery<ParseObject> queryPedestrianNode = ParseQuery.getQuery("node");
        queryPedestrianNode.whereEqualTo("area_id", areaID);
        queryPedestrianNode.setLimit(1000);

        List<ParseObject> results = queryPedestrianNode.find();

        List<String> queries = new ArrayList<>();


        String insertQuery = "insert into " + DatabaseHelper.NODE_TABLE + " values ( ";

        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("node_id") + ",";
            sql += p.getDouble("latitude") + ",";
            sql += p.getDouble("longitude") + ",";
            sql += p.getDouble("level") + ",";
            sql += p.getInt("type") + ",";
            sql += p.getInt("grid_id") + ",";
            sql += p.getInt("area_id");

            sql += ");";

            queries.add(sql);
        }

        Log.e("node", "size:" + results.size());
        mapDatabaseHelper.execQueryList(queries);

    }


    private void readPedestrianLink() throws ParseException {
        ParseQuery<ParseObject> queryPedestrianLink = ParseQuery.getQuery("link");
        queryPedestrianLink.whereEqualTo("area_id", areaID);
        queryPedestrianLink.setLimit(1000);

        List<ParseObject> results = queryPedestrianLink.find();
        List<String> queries = new ArrayList<>();


        String insertQuery = "insert into " + DatabaseHelper.LINK_TABLE + " values ( ";

        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("link_id") + ",";
            sql += p.getInt("node1_id") + ",";
            sql += p.getInt("node2_id") + ",";
            sql += p.getDouble("distance") + ",";
            sql += p.getDouble("bearing") + ",";
            sql += p.getInt("type") + ",";
            sql += p.getDouble("pressure") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        queryPedestrianLink.setSkip(1000);
        results = queryPedestrianLink.find();
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("link_id") + ",";
            sql += p.getInt("node1_id") + ",";
            sql += p.getInt("node2_id") + ",";
            sql += p.getDouble("distance") + ",";
            sql += p.getDouble("bearing") + ",";
            sql += p.getInt("type") + ",";
            sql += p.getDouble("pressure") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        Log.e("link", "size:" + queries.size());
        mapDatabaseHelper.execQueryList(queries);
    }

    private void readPedestrianPoint() throws ParseException {
        ParseQuery<ParseObject> queryPedestrianPoint = ParseQuery.getQuery("point");
        queryPedestrianPoint.whereEqualTo("area_id", areaID);
        queryPedestrianPoint.setLimit(1000);

        List<ParseObject> results = queryPedestrianPoint.find();
        List<String> queries = new ArrayList<>();


        String insertQuery = "insert into " + DatabaseHelper.WALL_POINT_TABLE + " values ( ";

        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("point_id") + ",";
            sql += p.getInt("node_id") + ",";
            sql += p.getInt("group_number") + ",";
            sql += p.getInt("point_order") + ",";
            sql += p.getDouble("latitude") + ",";
            sql += p.getDouble("longitude") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        queryPedestrianPoint.setSkip(1000);
        results = queryPedestrianPoint.find();
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("point_id") + ",";
            sql += p.getInt("node_id") + ",";
            sql += p.getInt("group_number") + ",";
            sql += p.getInt("point_order") + ",";
            sql += p.getDouble("latitude") + ",";
            sql += p.getDouble("longitude") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        queryPedestrianPoint.setSkip(2000);
        results = queryPedestrianPoint.find();
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("point_id") + ",";
            sql += p.getInt("node_id") + ",";
            sql += p.getInt("group_number") + ",";
            sql += p.getInt("point_order") + ",";
            sql += p.getDouble("latitude") + ",";
            sql += p.getDouble("longitude") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        Log.e("point", "size:" + queries.size());
        mapDatabaseHelper.execQueryList(queries);

    }


    private void readPedestrianGrid() throws ParseException {
        ParseQuery<ParseObject> queryPedestrianGrid = ParseQuery.getQuery("link_grid");
        queryPedestrianGrid.whereEqualTo("area_id", areaID);
        queryPedestrianGrid.setLimit(1000);
        List<ParseObject> results = queryPedestrianGrid.find();

        List<String> queries = new ArrayList<>();

        String insertQuery = "insert into " + DatabaseHelper.LINK_GRID_TABLE + " values ( ";
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("link_id") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        queryPedestrianGrid.setSkip(1000);
        results = queryPedestrianGrid.find();
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("link_id") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        queryPedestrianGrid.setSkip(2000);
        results = queryPedestrianGrid.find();
        for (ParseObject p : results) {
            String sql = new String(insertQuery);

            sql += p.getInt("link_id") + ",";
            sql += p.getString("grid_id") + ",";
            sql += p.getInt("area_id");
            sql += ");";

            queries.add(sql);
        }

        mapDatabaseHelper.execQueryList(queries);
    }


    public String getAreaName() {
        return areaName;
    }


    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
}
