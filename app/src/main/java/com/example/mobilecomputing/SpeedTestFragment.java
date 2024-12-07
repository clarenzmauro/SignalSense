package com.example.mobilecomputing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.text.DecimalFormat;

public class SpeedTestFragment extends Fragment implements SpeedTestManager.SpeedTestListener {
    private TextView uploadSpeedText;
    private TextView downloadSpeedText;
    private TextView currentSpeedText;
    private TextView pingText;
    private TextView jitterText;
    private TextView lossText;
    private Button startTestButton;
    private CircularProgressIndicator speedMeter;
    private SpeedTestManager speedTestManager;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speed_test, container, false);
        initializeViews(view);
        return view;
    }

    private void initializeViews(View view) {
        uploadSpeedText = view.findViewById(R.id.uploadSpeedText);
        downloadSpeedText = view.findViewById(R.id.downloadSpeedText);
        currentSpeedText = view.findViewById(R.id.currentSpeedText);
        pingText = view.findViewById(R.id.pingText);
        jitterText = view.findViewById(R.id.jitterText);
        lossText = view.findViewById(R.id.lossText);
        startTestButton = view.findViewById(R.id.startTestButton);
        speedMeter = view.findViewById(R.id.speedMeter);

        speedTestManager = new SpeedTestManager(this);

        startTestButton.setOnClickListener(v -> {
            resetUI();
            startTestButton.setEnabled(false);
            startTestButton.setText("Testing...");
            speedTestManager.startSpeedTest();
        });
    }

    private void resetUI() {
        uploadSpeedText.setText("0.00");
        downloadSpeedText.setText("0.00");
        currentSpeedText.setText("0.00");
        pingText.setText("0ms");
        jitterText.setText("0ms");
        lossText.setText("0%");
        speedMeter.setProgress(0);
    }

    @Override
    public void onDownloadProgress(double speed) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String formattedSpeed = decimalFormat.format(speed);
            downloadSpeedText.setText(formattedSpeed + "mbps");
            currentSpeedText.setText(formattedSpeed);
            speedMeter.setProgress((int) (speed * 100 / 150)); // Assuming max speed of 150 Mbps
        });
    }

    @Override
    public void onUploadProgress(double speed) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            String formattedSpeed = decimalFormat.format(speed);
            uploadSpeedText.setText(formattedSpeed + "mbps");
            currentSpeedText.setText(formattedSpeed);
            speedMeter.setProgress((int) (speed * 100 / 150)); // Assuming max speed of 150 Mbps
        });
    }

    @Override
    public void onPingResult(long ping) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> 
            pingText.setText(ping + "ms"));
    }

    @Override
    public void onJitterResult(double jitter) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> 
            jitterText.setText(decimalFormat.format(jitter) + "ms"));
    }

    @Override
    public void onPacketLossResult(double packetLoss) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> 
            lossText.setText(decimalFormat.format(packetLoss) + "%"));
    }

    @Override
    public void onTestComplete() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            startTestButton.setEnabled(true);
            startTestButton.setText("START TEST");
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speedTestManager.stopSpeedTest();
    }
}
