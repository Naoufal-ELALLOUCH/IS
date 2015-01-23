package ritsumei.cs.ubi.shun.pdr3methodstest.pdrmain;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

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

    public static final String METHOD_PDR_KEY = "methodPdr";
    public static final String METHOD_SM_KEY = "methodSm";
    public static final String METHOD_CM_KEY = "methodCm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        setContentView(R.layout.activity_select_method);

        pdrCheckBox = (CheckBox) findViewById(R.id.method_pdr);
        pdrCheckBox.setChecked(pref.getBoolean(METHOD_PDR_KEY, true));
        smCheckBox = (CheckBox) findViewById(R.id.method_sm);
        smCheckBox.setChecked(pref.getBoolean(METHOD_SM_KEY, false));
        cmCheckBox = (CheckBox) findViewById(R.id.method_cm);
        cmCheckBox.setChecked(pref.getBoolean(METHOD_CM_KEY, false));

        applyButton = (Button) findViewById(R.id.method_applay_button);
        applyButton.setOnClickListener(this);
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
