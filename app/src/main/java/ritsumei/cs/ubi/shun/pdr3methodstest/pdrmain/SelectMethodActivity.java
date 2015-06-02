package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;

import ritsumei.cs.ubi.shun.pdr3methodstest.R;
/**
 * Created by shun on 2014/12/25.
 */
public class SelectMethodActivity extends Activity implements OnClickListener{
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private CheckBox pdrCheckBox;
    private CheckBox smCheckBox;
    private CheckBox cmCheckBox;
    private Button applyButton;
    private Switch colorfulPolylineSwitch;
    private Switch wallLinkDrawSwitch;
    private Switch rawDataMeasureSwitch;

    public static final String METHOD_PDR_KEY = "methodPdr";
    public static final String METHOD_SM_KEY = "methodSm";
    public static final String METHOD_CM_KEY = "methodCm";
    public static final String COLORFUL_POLYLINE = "colorfulPolyline";
    public static final String WALL_LINK_DRAW = "wallLinkDrawSwitch";
    public static final String STRAIGHT_STEP_AUTO_ADJUST = "straightStepAutoAdjust";
    public static final String STRAIGHT_STEP_AUTO_ADJUST_ALWAYS = "straightStepAutoAdjustAlways";
    public static final String RAW_DATA_MEASURE = "rawDataMeasure";
    public static final String CHECK_POINT_LAT_LNG_OUTPUT = "checkPointLatLngOutput";

    private static ProgressDialog waitDialog;
    private Button downloadButton;
    private EnginePrefConfig enginePrefConfig;
    private ParseDownloader parseDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        // インスタンス作成
        waitDialog = new ProgressDialog(this);
        // タイトル設定
        waitDialog.setTitle("設定ファイルをダウンロード中");
        // メッセージ設定
        waitDialog.setMessage("now loading...");
        // スタイル設定 スピナー
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // キャンセル可能か(バックキーでキャンセル）
        waitDialog.setCancelable(false);
        enginePrefConfig = new EnginePrefConfig(this);
        parseDownloader = new ParseDownloader(this);

        setContentView(R.layout.activity_select_method);

        pdrCheckBox = (CheckBox) findViewById(R.id.method_pdr);
        pdrCheckBox.setChecked(pref.getBoolean(METHOD_PDR_KEY, true));
        smCheckBox = (CheckBox) findViewById(R.id.method_sm);
        smCheckBox.setChecked(pref.getBoolean(METHOD_SM_KEY, false));
        cmCheckBox = (CheckBox) findViewById(R.id.method_cm);
        cmCheckBox.setChecked(pref.getBoolean(METHOD_CM_KEY, false));

        applyButton = (Button) findViewById(R.id.method_applay_button);
        applyButton.setOnClickListener(this);

        colorfulPolylineSwitch = (Switch) findViewById(R.id.colorfulPolylineSwitch);
        colorfulPolylineSwitch.setChecked(pref.getBoolean(COLORFUL_POLYLINE, false));
        colorfulPolylineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(COLORFUL_POLYLINE, isChecked);
            }
        });

        wallLinkDrawSwitch = (Switch) findViewById(R.id.wallLinkDrawSwitch);
        wallLinkDrawSwitch.setChecked(pref.getBoolean(WALL_LINK_DRAW, false));
        wallLinkDrawSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(WALL_LINK_DRAW, isChecked);
            }
        });

        rawDataMeasureSwitch = (Switch) findViewById(R.id.RawDataMeasureSwitch);
        rawDataMeasureSwitch.setChecked(pref.getBoolean(RAW_DATA_MEASURE, false));
        rawDataMeasureSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(RAW_DATA_MEASURE, isChecked);
            }
        });
        downloadButton = (Button) findViewById(R.id.button_download);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if (!enginePrefConfig.getKeyLastArea().equals(enginePrefConfig.getKeyArea())) {
                    // ダイアログ表示
                    waitDialog.show();
                    // 別スレッドで時間のかかる処理を実行

//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
                            parseDownloader.setAreaName(enginePrefConfig.getKeyArea());
                            parseDownloader.incrementDBVersion();
                            enginePrefConfig.setKeyWirelessDbVersion(enginePrefConfig.getKeyWirelessDbVersion() + 1);
                            enginePrefConfig.setKeyPsnDbVersion(enginePrefConfig.getKeyPsnDbVersion() + 1);
                            parseDownloader.startDownLoad();
                            // 終わったらダイアログ消去
                            enginePrefConfig.setKeyLastArea(enginePrefConfig.getKeyArea());
                            waitDialog.dismiss();
//                            showToast("Loaded");
//                        }
//                    }).start();

//                } else {
//                    showToast();
//                }

            }
        });
    }


    private void showToast(String messageString) {
        Toast.makeText(this, messageString, Toast.LENGTH_SHORT).show();
    }

    private void showToast() {
        Toast.makeText(this, "already downloaded", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        if (v == applyButton) {
            editPreferences();
            finish();
        }
    }

    protected void editPreferences() {
        if(!pdrCheckBox.isChecked() && !smCheckBox.isChecked() && !cmCheckBox.isChecked()) {
            return;
        }
        editor.putBoolean(METHOD_PDR_KEY, pdrCheckBox.isChecked());
        editor.putBoolean(METHOD_SM_KEY, smCheckBox.isChecked());
        editor.putBoolean(METHOD_CM_KEY, cmCheckBox.isChecked());
        editor.commit();
    }
}
