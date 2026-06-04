package com.hong.xin.stock;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hong.xin.stock.ui.home.HomeFragment;
import com.hong.xin.stock.ui.trade.TradeFragment;
import com.hong.xin.stock.ui.trade.TradeViewModel;
import com.hong.xin.stock.ui.settings.SettingsFragment;
import com.hong.xin.stock.util.SettingsManager;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TradeViewModel.onPendingAlert = msg -> {
            SettingsManager sm = new SettingsManager(this);
            sm.clearPendingAlertDialog();
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("交易提醒")
                    .setMessage(msg)
                    .setPositiveButton("确定", null)
                    .show());
        };

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.nav_trade) {
                fragment = new TradeFragment();
            } else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();
            } else {
                return false;
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            showPendingAlert();
            return true;
        });
    }

    public void switchToTradeTab() {
        bottomNav.setSelectedItemId(R.id.nav_trade);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TradeViewModel.onPendingAlert = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showPendingAlert();
    }

    private void showPendingAlert() {
        SettingsManager sm = new SettingsManager(this);
        if (sm.hasPendingAlertDialog()) {
            String msg = sm.getPendingAlertDialog();
            sm.clearPendingAlertDialog();
            new AlertDialog.Builder(this)
                    .setTitle("交易提醒")
                    .setMessage(msg)
                    .setPositiveButton("确定", null)
                    .show();
        }
    }
}
