package com.hong.xin.stock.ui.analysis;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hong.xin.stock.R;
import com.hong.xin.stock.util.SettingsManager;

public class AnalysisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        SettingsManager sm = new SettingsManager(this);
        sm.setAnalysisRequested(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnalysisFragment())
                    .commit();
        }
    }
}
