package com.example.mobilecomputing;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {
    private Context context;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private Handler mainHandler;
    private ExecutorService executorService;
    private ConnectionListener listener;
    private long connectionStartTime;

    public interface ConnectionListener {
        void onConnectionInfoUpdated(ConnectionInfo info);
    }

    public static class ConnectionInfo {
        public String networkName;
        public String ipAddress;
        public String connectionTime;
        public long uptime;
        public double uploadSpeed;
        public double downloadSpeed;
        public boolean isConnected;
        public String networkType;
    }

    public ConnectionManager(Context context, ConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        this.connectionStartTime = System.currentTimeMillis();
    }

    public void startMonitoring() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    updateConnectionInfo();
                    try {
                        Thread.sleep(1000); // Update every second
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    public void stopMonitoring() {
        executorService.shutdownNow();
    }

    private void updateConnectionInfo() {
        final ConnectionInfo info = new ConnectionInfo();
        Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);

        info.isConnected = (capabilities != null);
        if (info.isConnected) {
            // Get network type
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                info.networkType = "WiFi";
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                info.networkName = wifiInfo.getSSID().replace("\"", "");
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                info.networkType = "Mobile Data";
                info.networkName = "Cellular Network";
            } else {
                info.networkType = "Other";
                info.networkName = "Unknown Network";
            }

            // Get IP Address
            info.ipAddress = getIPAddress();

            // Calculate connection time
            long duration = System.currentTimeMillis() - connectionStartTime;
            info.uptime = duration;
            info.connectionTime = formatDuration(duration);

            // Get network speeds
            info.downloadSpeed = capabilities.getLinkDownstreamBandwidthKbps() / 1000.0; // Convert to Mbps
            info.uploadSpeed = capabilities.getLinkUpstreamBandwidthKbps() / 1000.0; // Convert to Mbps
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onConnectionInfoUpdated(info);
                }
            }
        });
    }

    private String getIPAddress() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    private String formatDuration(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
        return String.format(Locale.getDefault(), "%dh %dmin %ds", hours, minutes, seconds);
    }
}
