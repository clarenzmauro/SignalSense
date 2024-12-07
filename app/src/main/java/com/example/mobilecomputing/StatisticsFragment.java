package com.example.mobilecomputing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class StatisticsFragment extends Fragment implements ConnectionManager.ConnectionListener {
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    private String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE
    };

    private TextView vpnStatusText;
    private TextView uploadSpeedText;
    private TextView downloadSpeedText;
    private TextView ipAddressText;
    private TextView connectionTimeText;
    private TextView weeklyTimeText;
    private TextView currentStreakText;
    private TextView longestWeekText;
    private TextView longestConnectionText;
    private BarChart weeklyChart;
    private ConnectionManager connectionManager;
    private SharedPreferences prefs;
    private long longestConnectionDuration = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        longestConnectionDuration = prefs.getLong("longest_connection", 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        initializeViews(view);
        setupWeeklyChart();
        
        if (checkAndRequestPermissions()) {
            initializeConnectionManager();
        }
        
        return view;
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsNeeded.toArray(new String[0]),
                PERMISSIONS_REQUEST_CODE
            );
            return false;
        }
        
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                initializeConnectionManager();
            } else {
                Toast.makeText(requireContext(),
                    "Network monitoring requires all permissions to function properly",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeConnectionManager() {
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(requireContext(), this);
            connectionManager.startMonitoring();
        }
    }

    private void initializeViews(View view) {
        vpnStatusText = view.findViewById(R.id.vpnStatusText);
        uploadSpeedText = view.findViewById(R.id.uploadSpeedText);
        downloadSpeedText = view.findViewById(R.id.downloadSpeedText);
        ipAddressText = view.findViewById(R.id.ipAddressText);
        connectionTimeText = view.findViewById(R.id.connectionTimeText);
        weeklyTimeText = view.findViewById(R.id.weeklyTimeText);
        currentStreakText = view.findViewById(R.id.currentStreakText);
        longestWeekText = view.findViewById(R.id.longestWeekText);
        longestConnectionText = view.findViewById(R.id.longestConnectionText);
        weeklyChart = view.findViewById(R.id.weeklyChart);

        // Initialize with default values
        uploadSpeedText.setText("0.0");
        downloadSpeedText.setText("0.0");
        ipAddressText.setText("Not connected");
        connectionTimeText.setText("0h 0min 0s");
        weeklyTimeText.setText("0h 0min");
        currentStreakText.setText("0 days");
        longestWeekText.setText("0 days");
        longestConnectionText.setText(formatDuration(longestConnectionDuration));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (connectionManager != null && checkAndRequestPermissions()) {
            connectionManager.startMonitoring();
            updateWeeklyStats();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (connectionManager != null) {
            connectionManager.stopMonitoring();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectionManager != null) {
            connectionManager.stopMonitoring();
        }
    }

    @Override
    public void onConnectionInfoUpdated(ConnectionManager.ConnectionInfo info) {
        if (getActivity() == null || !isAdded()) return;

        getActivity().runOnUiThread(() -> {
            // Update network status
            vpnStatusText.setText(info.networkName);
            
            // Update speeds
            uploadSpeedText.setText(String.format("%.1f", info.uploadSpeed));
            downloadSpeedText.setText(String.format("%.1f", info.downloadSpeed));
            
            // Update IP
            ipAddressText.setText(info.ipAddress);
            
            // Update connection time
            connectionTimeText.setText(info.connectionTime);
            
            // Update longest connection if current is longer
            if (info.uptime > longestConnectionDuration) {
                longestConnectionDuration = info.uptime;
                prefs.edit().putLong("longest_connection", longestConnectionDuration).apply();
                longestConnectionText.setText(formatDuration(longestConnectionDuration));
            }
            
            // Update current connection stats
            updateConnectionStats(info.uptime);
        });
    }

    private void updateConnectionStats(long currentUptime) {
        if (!isAdded()) return;
        
        // Update current streak
        int currentStreak = prefs.getInt("current_streak", 0);
        currentStreakText.setText(currentStreak + " days");
        
        // Update weekly time
        long weeklyTime = prefs.getLong("weekly_time", 0) + 1000; // Add 1 second
        prefs.edit().putLong("weekly_time", weeklyTime).apply();
        weeklyTimeText.setText(formatDuration(weeklyTime));
        
        // Update longest week
        long longestWeek = prefs.getLong("longest_week", 0);
        if (weeklyTime > longestWeek) {
            longestWeek = weeklyTime;
            prefs.edit().putLong("longest_week", longestWeek).apply();
        }
        longestWeekText.setText(formatDuration(longestWeek));
    }

    private void setupWeeklyChart() {
        if (weeklyChart == null) return;

        weeklyChart.getDescription().setEnabled(false);
        weeklyChart.setDrawGridBackground(false);
        weeklyChart.setDrawBarShadow(false);
        weeklyChart.setHighlightFullBarEnabled(false);
        weeklyChart.setDrawBorders(false);
        weeklyChart.getLegend().setEnabled(false);
        weeklyChart.setTouchEnabled(false);

        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.WHITE);
        String[] days = new String[]{"S", "M", "T", "W", "T", "F", "S"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));

        weeklyChart.getAxisLeft().setDrawGridLines(false);
        weeklyChart.getAxisLeft().setTextColor(Color.WHITE);
        weeklyChart.getAxisRight().setEnabled(false);

        updateWeeklyChart();
    }

    private void updateWeeklyChart() {
        if (weeklyChart == null || !isAdded()) return;

        List<BarEntry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        
        for (int i = 0; i < 7; i++) {
            int dayIndex = (currentDay - Calendar.SUNDAY + i) % 7;
            long timeForDay = prefs.getLong("day_" + dayIndex + "_time", 0);
            entries.add(new BarEntry(i, TimeUnit.MILLISECONDS.toHours(timeForDay)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Usage");
        dataSet.setColor(Color.parseColor("#4CAF50")); // Material Green color
        dataSet.setDrawValues(false); // Don't show the values on top of bars
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        weeklyChart.setData(barData);
        
        // Customize X axis
        XAxis xAxis = weeklyChart.getXAxis();
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGridColor(Color.GRAY);
        xAxis.setAxisLineColor(Color.BLACK);
        
        // Customize Y axis
        YAxis leftAxis = weeklyChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setGridColor(Color.GRAY);
        leftAxis.setAxisLineColor(Color.BLACK);
        
        // Disable right Y axis
        weeklyChart.getAxisRight().setEnabled(false);
        
        weeklyChart.invalidate();
    }

    private void updateWeeklyStats() {
        if (!isAdded()) return;
        
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        long currentDayTime = prefs.getLong("day_" + currentDay + "_time", 0);
        prefs.edit().putLong("day_" + currentDay + "_time", currentDayTime + 1000).apply();
        updateWeeklyChart();
    }

    private String formatDuration(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        return String.format("%dh %dmin", hours, minutes);
    }
}
