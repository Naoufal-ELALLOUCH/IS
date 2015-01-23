/*
 * Copyright (C) 2013 Ritsumeikan University Nishio Laboratory All Rights Reserved.
 */
package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import jp.ac.ritsumei.cs.ubi.shun.library.pdr.GyroCalibratedListener;
import jp.ac.ritsumei.cs.ubi.shun.library.pdr.GyroOffsetCalculator;
import jp.ac.ritsumei.cs.ubi.shun.library.pdr.PDRParametersObject;
import jp.ac.ritsumei.cs.ubi.shun.library.pdr.StepCalibratedListener;
import jp.ac.ritsumei.cs.ubi.shun.library.pdr.StepThreshCalculator;
import ritsumei.cs.ubi.shun.pdr3methodstest.R;


/**
 * PDRのパラメータ設定を行うクラス
 */
public class SettingsActivity extends Activity implements OnClickListener, SensorEventListener, StepCalibratedListener, GyroCalibratedListener {

	/**
	 * ユーザに歩いてもらう歩数
	 */
	private final int STEPS = 20;

	/**
	 * 端末を放置させておく秒数
	 */
	private final int TIME = 5;

	private SensorManager manager;
	private StepThreshCalculator stepThreshCalculator;
	private GyroOffsetCalculator gyroOffsetCalculator;
	private TextView stepLengthTextView;
	private Button stepCalibration;
	private Button gyroCalibration;
	private Button applyChange;

	/**
	 * 歩幅のプリファレンスキー
	 */
	public static final String STEP_LENGTH_KEY = "stepLength";

	/**
	 * 歩数計の閾値のプリファレンスキー
	 */
	public static final String STEP_DIFF_THRESH_KEY  = "stepThresh";

	/**
	 * 歩幅と歩調の相関を表すレートのプリファレンスキー
	 */
	public static final String STEP_RATE_KEY = "stepRate";

	/**
	 * ジャイロスコープの各軸のオフセット値のプリファレンスキー
	 */
	public static final String GYRO_OFFSET_X = "gyroOffsetX";
	public static final String GYRO_OFFSET_Y = "gyroOffsetY";
	public static final String GYRO_OFFSET_Z = "gyroOffsetZ";

	private SharedPreferences pref;
	private SharedPreferences.Editor editor;

	private AlertDialog.Builder AlertDlgBldr;
	private AlertDialog AlertDlg;

	private PDRParametersObject mPDRParametersObject;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
	    editor = pref.edit();

	    mPDRParametersObject = new PDRParametersObject();

	    setContentView(R.layout.activity_settings);

	    manager = (SensorManager)getSystemService(SENSOR_SERVICE);

	    /**
	     * 歩数計の閾値、歩調を調査(20歩)
	     */
	    stepThreshCalculator = new StepThreshCalculator(STEPS);
	    stepThreshCalculator.addListener(this);

	    /**
	     * ジャイロスコープのオフセット値を調査(5秒)
	     * ジャイロスコープ安定時に自動でキャリブレーションを終了させる機能をオン
	     */
	    gyroOffsetCalculator = new GyroOffsetCalculator(TIME, true);
	    gyroOffsetCalculator.addListener(this);

	    /**
	     * 歩幅入力用テキストボックス
	     */
	    stepLengthTextView = (TextView) findViewById(R.id.stepLengthValueText);
        stepLengthTextView.setText(Float.toString((pref.getFloat(STEP_LENGTH_KEY, 75.0f))));

        /**
         * 歩数計の閾値、歩調を調査を開始するボタン
         */
        stepCalibration = (Button) findViewById(R.id.stepCalibrationButton);
        stepCalibration.setOnClickListener(this);

        /**
         * ジャイロスコープのオフセット値の調査を開始するボタン
         */
        gyroCalibration = (Button) findViewById(R.id.gyroCalibrateButton);
        gyroCalibration.setOnClickListener(this);

        /**
         * 閾値などをプリファレンスに反映するボタン
         */
        applyChange = (Button) findViewById(R.id.applyChangesButton);
        applyChange.setOnClickListener(this);

		AlertDlgBldr = new AlertDialog.Builder(this);

	}

	@Override
	public void onClick(View v) {
		if (v == stepCalibration) {
			startAccSensor();
	        AlertDlgBldr.setTitle("");
	        AlertDlgBldr.setMessage("20歩歩いてください。");
	        AlertDlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		@Override
        		public void onClick(DialogInterface dialog, int which) {
        			stepThreshCalculator.finishCalibration();
        		}
        	});

	        AlertDlg = AlertDlgBldr.create();
	        AlertDlg.show();
		} else if (v == gyroCalibration) {
			startGyroSensor();
			AlertDlgBldr.setTitle("");
			AlertDlgBldr.setMessage("端末を平坦な場所に5秒間放置してください。");
			AlertDlgBldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					gyroOffsetCalculator.finishCalibration();
				}
			});
			AlertDlg = AlertDlgBldr.create();
			AlertDlg.show();

		}	else if (v == applyChange) {

			/**
			 * テキストボックスから歩幅を取得
			 */
			mPDRParametersObject.setStepLength(Float.valueOf(stepLengthTextView.getText().toString()));

			/**
			 * 歩幅と歩調の相関を表すレートを算出
			 */
			mPDRParametersObject.calculateRate();

			/**
			 * プリファレンスに反映
			 */
			editPreferences();
			finish();
		}

	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			stepThreshCalculator.logStep(event.values, event.timestamp);

		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			gyroOffsetCalculator.logGyro(event.values, event.timestamp);

		}
	}



	public void startAccSensor() {
		manager.registerListener(
				this,
				manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	public void startGyroSensor(){
		manager.registerListener(
				this,
				manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_FASTEST);

	}

	public void stopAccSensor() {
		manager.unregisterListener(this,
				manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

	}

	public void stopGyroSensor() {
		manager.unregisterListener(this,
				manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

	}

	@Override
	public void onGyroCalibrated(float offset[]) {
		/**
		 * オフセットの取得
		 */
		mPDRParametersObject.setOffset(offset);

		stopGyroSensor();
		AlertDlg.dismiss();

	}

	@Override
	public void onStepCalibrated(float stepDiffThresh, long pace) {
		/**
		 * 歩調、歩数計の閾値を取得
		 */
		mPDRParametersObject.setStepLength(Float.valueOf(stepLengthTextView.getText().toString()));
		mPDRParametersObject.setPace(pace);
		mPDRParametersObject.calculateRate();
		mPDRParametersObject.setStepDiffThresh(stepDiffThresh);

		stopAccSensor();
		AlertDlg.dismiss();

	}

	/**
	 * 設定した内容を反映するメソッド
	 */
	protected void editPreferences() {
		editor.putFloat(STEP_LENGTH_KEY, mPDRParametersObject.getStepLength());
		editor.putFloat(STEP_DIFF_THRESH_KEY, mPDRParametersObject.getStepDiffThresh());
		editor.putFloat(STEP_RATE_KEY, mPDRParametersObject.getRate());
		editor.putFloat(GYRO_OFFSET_X, mPDRParametersObject.getOffset()[0]);
		editor.putFloat(GYRO_OFFSET_Y, mPDRParametersObject.getOffset()[1]);
		editor.putFloat(GYRO_OFFSET_Z, mPDRParametersObject.getOffset()[2]);
        editor.commit();
	}

}
