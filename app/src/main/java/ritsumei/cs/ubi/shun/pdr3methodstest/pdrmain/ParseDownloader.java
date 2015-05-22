package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.Parse;

import java.io.BufferedReader;
import java.io.File;
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

    public static int getMapDBVerison() {
        return mapDBversion;
    }

    public static int getWirelessDBversion() {
        return wirelessDBversion;
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
    }


    private void readDatabaseFromFile(String db_query_file) throws IOException {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + db_query_file;
        File file = new File(filePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        List<String> queries = new ArrayList<>();

        String line = bufferedReader.readLine();
        while (line != null) {
            if (!line.endsWith(";")) {
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

    public String getAreaName() {
        return areaName;
    }


    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }
}
