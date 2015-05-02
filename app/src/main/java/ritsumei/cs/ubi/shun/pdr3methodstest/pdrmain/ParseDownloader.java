package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.AlertDialog;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
        ListView fileListView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("既存データの読み込み")
                .setMessage("ファイルを選択してください．")
                .setView(fileListView)
                .setPositiveButton("キャンセル", null)
                .show();

        String saveRootPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        File[] files = new File(saveRootPath).listFiles();
        if (files != null) {
            for (File file : files) {
                adapter.add(file.getName());
            }
            fileListView.setAdapter(adapter);
            fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListView listView = (ListView) parent;
                    String selectedFileName = (String) listView.getItemAtPosition(position);

                    mapDatabaseHelper = DatabaseHelper.getInstance(context, mapDBversion);
                    mapDatabaseHelper.dropDB();
                    Log.v("StartDownload", "Drop DB");
                    try {
                        readDatabaseFromFile(selectedFileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "読み込みエラー", Toast.LENGTH_SHORT).show();
                    } finally {
                        Log.v("StartDownload", "Read Database from file.");
                    }
                    dialog.dismiss();
                }
            });
        }

//        try {
//            //エリアIDの取得
//            ParseQuery<ParseObject> query = ParseQuery.getQuery("area");
//            query.whereEqualTo("name", areaName);
//            List<ParseObject> idList = query.find();
//            areaID = idList.get(0).getInt("area_id");
//
//            //pdr
//            //DatabaseHelper.NODE_TABLE
//            readPedestrianNode();
//
//            //DatabaseHelper.LINK_TABLE
//            readPedestrianLink();
//
//            //DatabaseHelper.WALL_POINT_TABLE
//            readPedestrianPoint();
//
//            //DatabaseHelper.LINK_GRID_TABLE
//            readPedestrianGrid();
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
    }


    private void readDatabaseFromFile(String db_query_file) throws IOException {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + db_query_file;
        File file = new File(filePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        List<String> queries = new ArrayList<>();

        String line = bufferedReader.readLine();
        while(line != null) {
            if(!line.endsWith(";")) {
                Toast.makeText(context, "このファイルは読み込めません", Toast.LENGTH_SHORT).show();
                return;
            }
            queries.add(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        mapDatabaseHelper.execQueryList(queries);
        Toast.makeText(context, "読み込み完了", Toast.LENGTH_SHORT).show();
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

        //Log.e("node", "size:" + results.size());
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

        //Log.e("link", "size:" + queries.size());
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

        //Log.e("point", "size:" + queries.size());
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
