package com.example.mobilecomputing;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeedTestManager {
    private static final String DOWNLOAD_TEST_URL = "https://www.google.com";  // Replace with your test server
    private static final String UPLOAD_TEST_URL = "https://www.google.com";    // Replace with your test server
    private static final int PING_COUNT = 10;
    private SpeedTestListener listener;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isTestRunning;

    public interface SpeedTestListener {
        void onDownloadProgress(double speed);
        void onUploadProgress(double speed);
        void onPingResult(long ping);
        void onJitterResult(double jitter);
        void onPacketLossResult(double packetLoss);
        void onTestComplete();
    }

    public SpeedTestManager(SpeedTestListener listener) {
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startSpeedTest() {
        if (isTestRunning) return;
        isTestRunning = true;
        
        executorService.execute(() -> {
            // Measure ping and jitter
            measurePingAndJitter();
            
            // Measure download speed
            measureDownloadSpeed();
            
            // Measure upload speed
            measureUploadSpeed();
            
            // Calculate packet loss
            calculatePacketLoss();
            
            isTestRunning = false;
            mainHandler.post(() -> listener.onTestComplete());
        });
    }

    private void measurePingAndJitter() {
        List<Long> pingTimes = new ArrayList<>();
        
        for (int i = 0; i < PING_COUNT; i++) {
            try {
                long startTime = System.currentTimeMillis();
                URL url = new URL(DOWNLOAD_TEST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.connect();
                long endTime = System.currentTimeMillis();
                
                long pingTime = endTime - startTime;
                pingTimes.add(pingTime);
                
                final long finalPingTime = pingTime;
                mainHandler.post(() -> listener.onPingResult(finalPingTime));
                
                connection.disconnect();
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Calculate jitter
        double jitter = calculateJitter(pingTimes);
        mainHandler.post(() -> listener.onJitterResult(jitter));
    }

    private void measureDownloadSpeed() {
        try {
            URL url = new URL(DOWNLOAD_TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            long startTime = System.currentTimeMillis();
            long bytesRead = 0;
            byte[] buffer = new byte[1024];
            int read;
            
            while ((read = connection.getInputStream().read(buffer)) != -1) {
                bytesRead += read;
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 0) {
                    double speed = (bytesRead * 8.0 / 1000000.0) / (duration / 1000.0); // Mbps
                    final double finalSpeed = speed;
                    mainHandler.post(() -> listener.onDownloadProgress(finalSpeed));
                }
            }
            
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void measureUploadSpeed() {
        try {
            URL url = new URL(UPLOAD_TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            
            byte[] data = new byte[1024 * 1024]; // 1MB of test data
            long startTime = System.currentTimeMillis();
            long bytesSent = 0;
            
            while (bytesSent < data.length) {
                connection.getOutputStream().write(data, 0, 1024);
                bytesSent += 1024;
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 0) {
                    double speed = (bytesSent * 8.0 / 1000000.0) / (duration / 1000.0); // Mbps
                    final double finalSpeed = speed;
                    mainHandler.post(() -> listener.onUploadProgress(finalSpeed));
                }
            }
            
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculatePacketLoss() {
        int successfulPings = 0;
        int totalPings = PING_COUNT;
        
        for (int i = 0; i < totalPings; i++) {
            try {
                URL url = new URL(DOWNLOAD_TEST_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.connect();
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    successfulPings++;
                }
                
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        double packetLoss = ((totalPings - successfulPings) * 100.0) / totalPings;
        mainHandler.post(() -> listener.onPacketLossResult(packetLoss));
    }

    private double calculateJitter(List<Long> pingTimes) {
        if (pingTimes.size() < 2) return 0;
        
        double totalJitter = 0;
        for (int i = 1; i < pingTimes.size(); i++) {
            totalJitter += Math.abs(pingTimes.get(i) - pingTimes.get(i-1));
        }
        
        return totalJitter / (pingTimes.size() - 1);
    }

    public void stopSpeedTest() {
        isTestRunning = false;
    }
}
