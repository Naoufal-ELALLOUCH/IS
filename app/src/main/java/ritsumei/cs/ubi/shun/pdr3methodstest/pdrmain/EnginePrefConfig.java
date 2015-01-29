package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.location.LocationRequest;

public class EnginePrefConfig {

    public final static String KEY_ENGINE = "engine";
    public final static String KEY_GOOGLE_ACCURACY = "google_acc";
    public final static String KEY_GOOGLE_INTERVAL = "google_interval";
    public final static String KEY_HITACHI_MODULE = "hitachi_module";
    public final static String KEY_HITACHI_BLUETOOTH = "hitachi_bluettooth";
    public final static String KEY_RITSUMEI_MODULE = "ritsumei_module";
    public final static String KEY_INITIAL_POSITION_LATITUDE = "initial_lat";
    public final static String KEY_INITIAL_POSITION_LONGITUDE = "initial_lng";
    public final static String KEY_INITIAL_DIRECTION = "initial_direction";
    public final static String KEY_INITIAL_LEVEL = "initial_level";
    public final static String KEY_AREA = "area";
    public final static String KEY_LAST_AREA = "last_area";

    /**
     * 歩幅のプリファレンスキー
     */
    public static final String KEY_STEP_LENGTH = "step_length";
    /**
     * 歩数計の閾値のプリファレンスキー
     */
    public static final String KEY_STEP_DIFF_THRESH = "step_diff_thresh";
    /**
     * 歩幅と歩調の相関を表すレートのプリファレンスキー
     */
    public static final String KEY_STEP_CADENCE = "step_cadence";
    /**
     * ジャイロスコープの各軸のオフセット値のプリファレンスキー
     */
    public static final String KEY_GYRO_OFFSET_X = "gyro_offset_x";
    public static final String KEY_GYRO_OFFSET_Y = "gyro_offset_y";
    public static final String KEY_GYRO_OFFSET_Z = "gyro_offset_z";

    /**
     * 歩行空間ネットワークと電波状況のデータベースのバージョン
     */
    public final static String KEY_PSN_DB_VERSION = "psn_db_version";
    public final static String KEY_WIRELESS_DB_VERSION = "wireless_db_version";

    private static SharedPreferences sharedPrefs;

    public EnginePrefConfig(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getKeyArea() {
        return sharedPrefs.getString(KEY_AREA, "梅田地下街");
    }

    public void setKeyArea(String area) {
        sharedPrefs.edit().putString(KEY_AREA, area).apply();
    }

    public String getKeyLastArea() {
        return sharedPrefs.getString(KEY_LAST_AREA, "null");
    }

    public void setKeyLastArea(String lastArea) {
        sharedPrefs.edit().putString(KEY_LAST_AREA, lastArea).apply();
    }

//    public String getKeyEngine() {
//        return sharedPrefs.getString(KEY_ENGINE, GLocationRequest.Provider.GOOGLE.toString());
//    }

    public void setKeyEngine(String engine) {
        sharedPrefs.edit().putString(KEY_ENGINE, engine).apply();
    }

    public int getKeyGoogleAccuracy() {
        return sharedPrefs.getInt(KEY_GOOGLE_ACCURACY, LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void setKeyGoogleAccuracy(int acc) {
        sharedPrefs.edit().putInt(KEY_GOOGLE_ACCURACY, acc).apply();
    }

    public long getKeyGoogleInterval() {
        return sharedPrefs.getLong(KEY_GOOGLE_INTERVAL, 2000);
    }

    public void setKeyGoogleInterval(long interval) {
        sharedPrefs.edit().putLong(KEY_GOOGLE_INTERVAL, interval).apply();
    }

//    public String getKeyHitachiModule() {
//        return sharedPrefs.getString(KEY_HITACHI_MODULE, H2lEngineManager.H2L_MODE.IMES_DR.toString());
//    }

    public void setKeyHitachiModule(String module) {
        sharedPrefs.edit().putString(KEY_HITACHI_MODULE, module).apply();
    }

    public String getKeyHitachiBluetooth() {
        return sharedPrefs.getString(KEY_HITACHI_BLUETOOTH, "need bluetooth device address");
    }

    public void setKeyHitachiBluetooth(String address) {
        sharedPrefs.edit().putString(KEY_HITACHI_BLUETOOTH, address).apply();
    }

//    public String getKeyRitsumeiModule() {
//        return sharedPrefs.getString(KEY_RITSUMEI_MODULE, U2lEngineManager.U2L_MODE.CROSS_ASSISTIVE.toString());
//    }

    public void setKeyRitsumeiModule(String module) {
        sharedPrefs.edit().putString(KEY_RITSUMEI_MODULE, module).apply();
    }

    public double getKeyInitialPositionLatitude() {
        return Double.parseDouble(sharedPrefs.getString(KEY_INITIAL_POSITION_LATITUDE, "34.97948739467665"));
    }

    public void setKeyInitialPositionLatitude(double lat) {
        sharedPrefs.edit().putString(KEY_INITIAL_POSITION_LATITUDE, String.valueOf(lat)).apply();
    }

    public double getKeyInitialPositionLongitude() {
        return Double.parseDouble(sharedPrefs.getString(KEY_INITIAL_POSITION_LONGITUDE, "135.96445966511965"));
    }

    public void setKeyInitialPositionLongitude(double lng) {
        sharedPrefs.edit().putString(KEY_INITIAL_POSITION_LONGITUDE, String.valueOf(lng)).apply();
    }

    public double getKeyInitialDirection() {
        return Double.parseDouble(sharedPrefs.getString(KEY_INITIAL_DIRECTION, "-3.0f"));
    }

    public void setKeyInitialDirection(double direction) {
        sharedPrefs.edit().putString(KEY_INITIAL_DIRECTION, String.valueOf(direction)).apply();
    }

    public double getKeyInitialLevel() {
        return Double.parseDouble(sharedPrefs.getString(KEY_INITIAL_LEVEL, "-1.0"));
    }

    public void setKeyInitialLevel(double level) {
        sharedPrefs.edit().putString(KEY_INITIAL_LEVEL, String.valueOf(level)).apply();
    }

    public double getKeyStepLength() {
        return Double.parseDouble(sharedPrefs.getString(KEY_STEP_LENGTH, "70.0"));
    }

    public void setKeyStepLength(double stepLength) {
        sharedPrefs.edit().putString(KEY_STEP_LENGTH, String.valueOf(stepLength)).apply();
    }

    public double getKeyStepDiffThresh() {
        return Double.parseDouble(sharedPrefs.getString(KEY_STEP_DIFF_THRESH, "1.0"));
    }

    public void setKeyStepDiffThresh(double stepDiffThresh) {
        sharedPrefs.edit().putString(KEY_STEP_DIFF_THRESH, String.valueOf(stepDiffThresh)).apply();
    }

    public double getKeyStepCadence() {
        return Double.parseDouble(sharedPrefs.getString(KEY_STEP_CADENCE, "0.4"));
    }

    public void setKeyStepCadence(double stepCadence) {
        sharedPrefs.edit().putString(KEY_STEP_CADENCE, String.valueOf(stepCadence)).apply();
    }

    public double getKeyGyroOffsetX() {
        return Double.parseDouble(sharedPrefs.getString(KEY_GYRO_OFFSET_X, "0"));
    }

    public void setKeyGyroOffsetX(double gyroOffsetX) {
        sharedPrefs.edit().putString(KEY_GYRO_OFFSET_X, String.valueOf(gyroOffsetX)).apply();
    }

    public double getKeyGyroOffsetY() {
        return Double.parseDouble(sharedPrefs.getString(KEY_GYRO_OFFSET_Y, "0"));
    }

    public void setKeyGyroOffsetY(double gyroOffsetY) {
        sharedPrefs.edit().putString(KEY_GYRO_OFFSET_Y, String.valueOf(gyroOffsetY)).apply();
    }

    public double getKeyGyroOffsetZ() {
        return Double.parseDouble(sharedPrefs.getString(KEY_GYRO_OFFSET_Z, "0"));
    }

    public void setKeyGyroOffsetZ(double gyroOffsetZ) {
        sharedPrefs.edit().putString(KEY_GYRO_OFFSET_Z, String.valueOf(gyroOffsetZ)).apply();
    }

    public int getKeyPsnDbVersion() {
        return Integer.parseInt(sharedPrefs.getString(KEY_PSN_DB_VERSION, "0"));
    }

    public void setKeyPsnDbVersion(int psnDbVersion) {
        sharedPrefs.edit().putString(KEY_PSN_DB_VERSION, String.valueOf(psnDbVersion)).apply();
    }

    public int getKeyWirelessDbVersion() {
        return Integer.parseInt(sharedPrefs.getString(KEY_WIRELESS_DB_VERSION, "0"));
    }

    public void setKeyWirelessDbVersion(int wirelessDbVersion) {
        sharedPrefs.edit().putString(KEY_WIRELESS_DB_VERSION, String.valueOf(wirelessDbVersion)).apply();
    }
}
