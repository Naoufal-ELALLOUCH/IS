package ubilabmapmatchinglibrary.pedestrianspacenetwork;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteのデータベースを生成したり、クエリにより必要データを取得するクラス
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String NODE_TABLE = "node_info";    //テーブル名
    public static final String WALL_POINT_TABLE = "point_info"; //テーブル名
    public static final String LINK_TABLE = "link_info";    //テーブル名
    public static final String LINK_GRID_TABLE = "link_grid_info";//テーブル名
    public static final String DATABASE_NAME = "pedestrian_space_network.db";
    public static final int DATABASE_VERSION = 1;

    private List<String> queryList = new ArrayList<>();
    public Context context;
    private static DatabaseHelper sSingleton = null;

    private SQLiteDatabase mDatabase;
    private final Context mContext;
    private final File mDatabasePath;

    private static String DB_NAME = "umechika_psn";
    private static String DB_NAME_ASSET = "umechika_psn.db";

//    /**
//     * クエリによってデータベースを作成するときのコンストラクタ
//     * @param context
//     * @param queryList
//     */
//    public DatabaseHelper(Context context, List<String> queryList) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        this.context = context;
//        this.queryList = queryList;
//    }

    /**
     * 既に用意されているデータベースを利用するときのコンストラクタ
     * @param context
     * @param databaseVersion
     */
//    public DatabaseHelper(Context context, int databaseVersion) {
//        super(context, DATABASE_NAME, null, databaseVersion);
//        this.context = context;
//    }
//
////    public static synchronized DatabaseHelper getInstance(Context context, List<String> queryList) {
////        if (sSingleton == null) {
////            sSingleton = new DatabaseHelper(context, queryList);
////        }
////        return sSingleton;
////    }
//
//    public static synchronized DatabaseHelper getInstance(Context context, int databaseVersion) {
//        if (sSingleton == null) {
//            sSingleton = new DatabaseHelper(context, databaseVersion);
//        }
//        return sSingleton;
//    }


    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        mContext = context;
        mDatabasePath = mContext.getDatabasePath(DB_NAME);
    }

    /**
     * asset に格納したデータベースをコピーするための空のデータベースを作成する
     */
    public void createEmptyDataBase() throws IOException {
        boolean dbExist = checkDataBaseExists();

        if (dbExist) {
            // すでにデータベースは作成されている
        } else {
            // このメソッドを呼ぶことで、空のデータベースがアプリのデフォルトシステムパスに作られる
            getReadableDatabase();

            try {
                // asset に格納したデータベースをコピーする
                copyDataBaseFromAsset();

                String dbPath = mDatabasePath.getAbsolutePath();
                SQLiteDatabase checkDb = null;
                try {
                    checkDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
                } catch (SQLiteException e) {
                }

                if (checkDb != null) {
                    checkDb.setVersion(DATABASE_VERSION);
                    checkDb.close();
                }

            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * 再コピーを防止するために、すでにデータベースがあるかどうか判定する
     *
     * @return 存在している場合 {@code true}
     */
    private boolean checkDataBaseExists() {
        String dbPath = mDatabasePath.getAbsolutePath();

        SQLiteDatabase checkDb = null;
        try {
            checkDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            // データベースはまだ存在していない
        }

        if (checkDb == null) {
            // データベースはまだ存在していない
            return false;
        }

        int oldVersion = checkDb.getVersion();
        int newVersion = DATABASE_VERSION;

        if (oldVersion == newVersion) {
            // データベースは存在していて最新
            checkDb.close();
            return true;
        }

        // データベースが存在していて最新ではないので削除
        File f = new File(dbPath);
        f.delete();
        return false;
    }

    /**
     * asset に格納したデーだベースをデフォルトのデータベースパスに作成したからのデータベースにコピーする
     */
    private void copyDataBaseFromAsset() throws IOException {

        // asset 内のデータベースファイルにアクセス
        InputStream mInput = mContext.getAssets().open(DB_NAME_ASSET);

        // デフォルトのデータベースパスに作成した空のDB
        OutputStream mOutput = new FileOutputStream(mDatabasePath);

        // コピー
        byte[] buffer = new byte[1024];
        int size;
        while ((size = mInput.read(buffer)) > 0) {
            mOutput.write(buffer, 0, size);
        }

        // Close the streams
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public synchronized void close() {
        if(mDatabase != null)
            mDatabase.close();

        super.close();
    }




//
//    //データベースの作成
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//
//        //UbiNaviで出力した、DB生成クエリを読み込む
//        //execQueryFromFile(DB_QUERY_FILE, db);
//
//        //スキーマの定義
//        String NODE_DATABASE_CREATE_STATES =
//                "create table if not exists " + NODE_TABLE + "("
//                        + " id integer primary key"
//                        + ", latitude double not null"
//                        + ", longitude double not null"
//                        + ", level real not null"
//                        + ", type integer not null"
//                        + ", grid_id text not null"
//                        + ", area_id integer not null"
//                        + ");";
//        db.execSQL(NODE_DATABASE_CREATE_STATES);
//
//
//        String POINT_DATABASE_CREATE_STATES =
//                "create table if not exists " + WALL_POINT_TABLE + " ("
//                        + " id integer primary key"
//                        + ", node_id integer"
//                        + ", group_number integer not null"
//                        + ", point_order integer not null"
//                        + ", latitude real not null"
//                        + ", longitude real not null"
//                        + ", grid_id integer not null"
//                        + ", area_id integer not null"
//                        + ")";
//        db.execSQL(POINT_DATABASE_CREATE_STATES);
//
//        /**
//         * bearingはラジアン形式
//         */
//        String LINK_DATABASE_CREATE_STATES =
//                "create table if not exists " + LINK_TABLE + " ("
//                        + " id integer primary key"
//                        + ", node1_id integer not null"
//                        + ", node2_id integer not null"
//                        + ", distance real not null"
//                        + ", bearing real not null"
//                        + ", type integer not null"
//                        + ", pressure real not null"
//                        + ", area_id integer not null"
//                        + ")";
//        db.execSQL(LINK_DATABASE_CREATE_STATES);
//
//        String LINK_GRID_DATABASE_CREATE_STATES =
//                "create table if not exists " + LINK_GRID_TABLE + " ("
//                        + " link_id integer not null"
//                        + ", grid_id text not null"
//                        + ", area_id integer not null"
//                        + ")";
//
//        db.execSQL(LINK_GRID_DATABASE_CREATE_STATES);
//
//
//    }
//
//    //データベースの更新
//	/*DATABASE_VERSIONの値が最初に実行した時と、新しく実行した時の値が等しくなければ、
//	 * このメソッドが呼び出され、データベースを再度作成する
//	 * 値が大きくても、小さくてもよい*/
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
//        Log.v("DB","onUpgrade");
//        onCreate(db);
//    }

    /**
     * queryを実行する
     * @param queryList
     */
    public void execQueryList(List<String> queryList) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            for(String query : queryList) {
                db.execSQL(query);
            }
            db.setTransactionSuccessful();
        } catch (Exception e){         // 例外発生
            Log.v("DB_ERROR","query error");
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

    }

     /**
     * queryによりNodeを一つ取得する
     * @param db
     * @param query
     * @return
     */
    public Node getNodeByQuery(SQLiteDatabase db, String query) {

        SQLiteCursor c = (SQLiteCursor) db.rawQuery(query, null);
        c.moveToFirst();
        Node node = new Node(c.getInt(0), c.getDouble(1), c.getDouble(2), (int)c.getDouble(3), c.getInt(4), c.getString(5));
        c.close();

        return node;
    }

    /**
     * NodeTableから指定したidのNodeを取得する
     * @param id
     * @return
     */
    public Node getNodeById(int id) {
        SQLiteDatabase db = getReadableDatabase();

        String GET_NODE_BY_ID =
                "select * from " + NODE_TABLE + " where id = " + id;

        try{
            Node node = getNodeByQuery(db, GET_NODE_BY_ID);
            db.close();
            return node;

        } catch (SQLException e) {
            db.close();
            return null;
        }
    }

    /**
     * queryによりPointリストを取得する
     * @param db
     * @param query
     * @return
     */
    public List<WallPoint> getPointsByQuery(SQLiteDatabase db, String query) {

        SQLiteCursor c = (SQLiteCursor)db.rawQuery(query, null);
        List<WallPoint> pointsList = new ArrayList<WallPoint>();
        boolean next = c.moveToFirst();
        while(next) {
            pointsList.add(new WallPoint(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getDouble(4), c.getDouble(5), c.getString(6)));
            next = c.moveToNext();
        }
        c.close();
        return pointsList;
    }

    /**
     * PointTableから指定したidのNodeに属するPointリストを取得する
     * @param nodeId
     * @return
     */
    public List<WallPoint> getPointsByNodeId(int nodeId) {

        SQLiteDatabase db = getReadableDatabase();

        String GET_POINTS_BY_NODE_ID =
                "select * from " + WALL_POINT_TABLE + " where node_id = " + nodeId + " order by point_order";

        try{

            List<WallPoint> pointsList = getPointsByQuery(db, GET_POINTS_BY_NODE_ID);
            db.close();
            return pointsList;

        } catch (SQLException e) {

            db.close();
            return null;
        }
    }

    /**
     * PointTableから指定したidのgroupに属するPointリストをpointOrder昇順で取得する
     * @param groupNumber
     * @return
     */
    public List<WallPoint> getPointsByGroupNumber(int groupNumber) {
        SQLiteDatabase db = getReadableDatabase();

        String GET_POINTS_BY_GROUP_ID =
                "select * from " + WALL_POINT_TABLE + " where group_number = " + groupNumber + " order by point_order";

        try{
            List<WallPoint> pointsList = (getPointsByQuery(db, GET_POINTS_BY_GROUP_ID));
            db.close();
            return pointsList;

        } catch (SQLException e) {
            Log.v("MM","getPointsByGroupNumber" + e.toString());
            db.close();
            return null;
        }
    }


    /**
     * queryによりLinkを一つ取得する
     * @param db
     * @param query
     * @return
     */
    public Link getLinkByQuery(SQLiteDatabase db, String query) {

        SQLiteCursor c = (SQLiteCursor) db.rawQuery(query, null);
        c.moveToFirst();

        Link link = new Link(c.getInt(0), c.getInt(1), c.getInt(2), c.getDouble(3), c.getDouble(4),  c.getInt(5), c.getDouble(6));
        c.close();

        return link;
    }

    /**
     * LinkTableから指定したidのLinkを取得する
     * @param id
     * @return
     */
    public Link getLinkById(int id) {
        SQLiteDatabase db = getReadableDatabase();

        String GET_LINK_BY_ID =
                "select * from " + LINK_TABLE + " where id like " + id;

        try{
            Link link = getLinkByQuery(db, GET_LINK_BY_ID);
            db.close();
            return link;

        } catch (SQLException e) {
            db.close();
            return null;
        }
    }

    /**
     * LinkTableから指定したidListのLinkをリストで取得する
     * @param idList
     * @return
     */
    public List<Link> getLinkListByIdList(List<Integer> idList) {
        List<Link> linkList= new ArrayList<Link>();
        if(idList != null) {
            for (int id : idList) {
                linkList.add(getLinkById(id));
            }
            return linkList;
        } else {
            return null;
        }
    }

    /**
     * queryによりLinkのidリストを取得する
     * @param db
     * @param query
     * @return
     */
    public List<Integer> getLinkIdListByQuery(SQLiteDatabase db, String query) {

        SQLiteCursor c = (SQLiteCursor)db.rawQuery(query, null);
        List<Integer> linksIdList = new ArrayList<Integer>();
        boolean next = c.moveToFirst();
        while(next) {
            linksIdList.add(c.getInt(0));
            next = c.moveToNext();
        }
        c.close();

        return linksIdList;
    }

    /**
     * LinkGridTableから指定したidのGridを通るようなLinkのidのリストを取得する
     * @param gridId
     * @return
     */
    public List<Integer> getLinkIdListByGridId(String gridId) {
        SQLiteDatabase db = getReadableDatabase();

        String GET_LINKS_ID_BY_GRID_ID =
                "select link_id from " + LINK_GRID_TABLE + " where grid_id like " + gridId;

        try{
            List<Integer> linksIdList = getLinkIdListByQuery(db, GET_LINKS_ID_BY_GRID_ID);
            db.close();
            return linksIdList;

        } catch (SQLException e) {
            db.close();
            return null;
        }
    }


    /**
     * LinkGridTableから指定したidのGridを通り、指定したlevelにあるノードを繋ぐLinkのidのリストを取得する
     * @param gridId
     * @param level
     * @return
     */
    public List<Integer> getLinkIdListByGridIdAndLevel(String gridId, int level) {
        SQLiteDatabase db = getReadableDatabase();

        //指定したgrid_idかつ、リンクを構成するノードが指定したlevelにあるlinkIdを取得するクエリ
        //要検証
        String GET_LINKS_BY_GRID_ID_AND_LEVEL =
                "select link_id from "+ LINK_GRID_TABLE + " where grid_id like " + gridId + "and link_id in (select id from "
                        +LINK_TABLE + " where (node1_id or node2_id) in (select id from " + NODE_TABLE + "where level = " + level +"))";

        try{
            List<Integer> linksIdList = getLinkIdListByQuery(db, GET_LINKS_BY_GRID_ID_AND_LEVEL);
            db.close();
            return linksIdList;

        } catch (SQLException e) {
            db.close();
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
    public List<Integer> getLinkIdListByGridIdAndLevelAndType(String gridId, int level) {
        SQLiteDatabase db = getReadableDatabase();

        //指定したgrid_idかつ、リンクを構成するノードが指定したlevelにある、LinkTypeが0か1のlinkIdを取得するクエリ
        //要検証
        String GET_LINKS_BY_GRID_ID_AND_LEVEL_AND_TYPE =
                "select link_id from "+ LINK_GRID_TABLE + " where grid_id like " + gridId + "and link_id in (select id from " +
                        LINK_TABLE + " where ((node1_id or node2_id) in (select id from " + NODE_TABLE + "where level = " + level + ")) " +
                        "and type = (" + Link.LinkType.PASSAGE.ordinal() + " or " + Link.LinkType.OPEN_SPACE.ordinal() + "))";

        try{
            List<Integer> linksIdList = getLinkIdListByQuery(db, GET_LINKS_BY_GRID_ID_AND_LEVEL_AND_TYPE);
            db.close();
            return linksIdList;

        } catch (SQLException e) {
            db.close();
            return null;
        }
    }

    /**
     * LinkTableより指定したidのリンクと、それに隣接するリンクのIdリストを取得する
     * 前回のスケルトンマッチングの結果から、次にマッチさせるリンクの候補を調べるために用いる
     * @param linkId
     * @return
     */
    public List<Integer> getConnectingLinkIdListByLinkId(int linkId) {

        Link link = getLinkById(linkId);
        int node1Id = link.getNode1Id();
        int node2Id = link.getNode2Id();

        SQLiteDatabase db = getReadableDatabase();
        String GET_CONNECTING_LINK_ID_LIST_BY_LINK_ID =
                "select id from " + LINK_TABLE + " where node1_id = " + node1Id + " or node2_id = " + node1Id + " or node1_id = " + node2Id + " or node2_id = " + node2Id;

        try{
            List<Integer> linksIdList = getLinkIdListByQuery(db, GET_CONNECTING_LINK_ID_LIST_BY_LINK_ID);
            db.close();
            return linksIdList;
        } catch (SQLException e) {
            db.close();
            return null;
        }
    }

    /**
     * LinkTableより指定したidのグループ内にあるNodeから外部に出ているリンクのIdリストを取得する
     * 広場の出口のスケルトンマッチング候補を探す
     * @param groupNumber
     * @return
     */
    public List<Integer> getConnectingLinkIdListByGroupNumber(int groupNumber) {

        SQLiteDatabase db = getReadableDatabase();

        String GET_CONNECTING_LINK_ID_LIST_BY_GROUP_ID =
                "select id from " + LINK_TABLE + " where type = " + Link.LinkType.PASSAGE.ordinal() + " and " +
                        "(node1_id in (select node_id from " + WALL_POINT_TABLE + " where group_number = " + groupNumber + ")" +
                        "or node2_id in (select node_id from " + WALL_POINT_TABLE + " where group_number = " + groupNumber + "))";

        try{
            List<Integer> linksIdList = getLinkIdListByQuery(db, GET_CONNECTING_LINK_ID_LIST_BY_GROUP_ID);
            db.close();
            return linksIdList;
        } catch (SQLException e) {
            db.close();
            return null;
        }
    }

    /**
     * 二つのlinkに共通するNodeを取得する
     * @param link1Id
     * @param link2Id
     * @return
     */
    public Node getLinksCommonNode(int link1Id, int link2Id) {
        SQLiteDatabase db = getReadableDatabase();

        String GET_LINKS_COMMON_NODE_ID =
                "select node_id from" + LINK_TABLE  + "where (link1_id or link2_id) = " + link1Id + " or (link1_id or link2_id) = " + link2Id + ")";

        int commonNodeId = -1;
        try{
            SQLiteCursor c = (SQLiteCursor) db.rawQuery(GET_LINKS_COMMON_NODE_ID, null);
            c.moveToFirst();
            commonNodeId = c.getInt(0);
            c.close();
            db.close();
        } catch (SQLException e) {
            db.close();
        }

        Node commonNode = getNodeById(commonNodeId);

        return commonNode;
    }

}
