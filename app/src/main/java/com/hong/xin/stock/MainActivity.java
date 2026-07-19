package com.hong.xin.stock;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hong.xin.stock.data.SelectedStockManager;
import com.hong.xin.stock.data.StockListCache;
import com.hong.xin.stock.data.api.EastMoneyApi;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SEARCH = 1;

    private HomeFragment homeFragment;
    private ViewPager2 viewPager;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EastMoneyApi.init(StockListCache.getInstance(this));

        StrategyNotificationHelper.createChannel(this);
        StrategyAlarmScheduler.scheduleAlarms(this);

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_nav);
        fabAdd = findViewById(R.id.fab_add);

        PagerAdapter adapter = new PagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNav.getMenu().getItem(position).setChecked(true);
                fabAdd.setVisibility(FloatingActionButton.VISIBLE);
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, false);
                return true;
            } else if (itemId == R.id.nav_settings) {
                viewPager.setCurrentItem(1, false);
                return true;
            }
            return false;
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StockSearchActivity.class);
            startActivityForResult(intent, REQUEST_SEARCH);
        });

        int targetTab = getIntent().getIntExtra("tab", 0);
        if (targetTab > 0) {
            viewPager.setCurrentItem(Math.min(targetTab, 1), false);
        }
    }

    public void switchToTab(int position) {
        if (viewPager != null) {
            viewPager.setCurrentItem(Math.min(position, 1), false);
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            viewPager.setCurrentItem(0, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int targetTab = intent.getIntExtra("tab", -1);
        if (targetTab >= 0 && viewPager != null) {
            viewPager.setCurrentItem(Math.min(targetTab, 1), false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SEARCH && resultCode == RESULT_OK) {
            if (homeFragment != null) {
                homeFragment.refreshList();
            }
        }
    }

    private class PagerAdapter extends FragmentStateAdapter {

        PagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                homeFragment = new HomeFragment();
                return homeFragment;
            }
            return new SettingsFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
