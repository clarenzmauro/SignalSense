package com.example.mobilecomputing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;

public class MapActivity extends AppCompatActivity implements LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000; // 1 second
    private static final float MIN_DISTANCE_CHANGE = 1; // 1 meter

    private MapView map;
    private LocationManager locationManager;
    private Location currentLocation;
    private GeoPoint vpnLocation;
    private TextView locationText;
    private TextView ipAddressText;
    private Button pauseButton;
    private Button disconnectButton;
    private Marker currentLocationMarker;
    private Marker vpnLocationMarker;
    private Polygon currentLocationCircle;
    private Polygon vpnLocationCircle;
    private LocationListener locationListener;
    private WifiManager wifiManager;
    private Handler wifiReconnectHandler;
    private static final long PAUSE_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReconnectHandler = new Handler(Looper.getMainLooper());

        // Initialize views
        locationText = findViewById(R.id.locationText);
        ipAddressText = findViewById(R.id.ipAddressText);
        pauseButton = findViewById(R.id.pauseButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        map = findViewById(R.id.map);

        // Configure map
        setupMap();

        // Set up button listeners
        pauseButton.setOnClickListener(v -> pauseConnection());
        disconnectButton.setOnClickListener(v -> disconnectConnection());

        // For demo purposes, set Philippines location
        vpnLocation = new GeoPoint(14.5995, 120.9842); // Manila, Philippines coordinates
        locationText.setText("Philippines #9776");
        ipAddressText.setText("IP: 123.456.789.012");

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize current location marker
        currentLocationMarker = new Marker(map);
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_location_blue));
        currentLocationMarker.setTitle("Current Location");
        map.getOverlays().add(currentLocationMarker);

        // Request location permissions and start tracking if granted
        requestLocationPermissions();

        // Update connection info initially
        updateConnectionInfo();

        // Set up periodic connection info updates
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateConnectionInfo();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        }, 5000);

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_map) {
                // Already on map activity
                return true;
            } else if (itemId == R.id.navigation_speed) {
                startActivity(new Intent(this, SpeedTestActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_stats) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Set the map item as selected
        bottomNav.setSelectedItemId(R.id.navigation_map);
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        
        IMapController mapController = map.getController();
        mapController.setZoom(4.0);
        
        // Set initial position to Philippines
        mapController.setCenter(new GeoPoint(14.5995, 120.9842));
    }

    private void requestLocationPermissions() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    updateMapLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            // Request updates from both GPS and network provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    locationListener,
                    Looper.getMainLooper()
                );
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    locationListener,
                    Looper.getMainLooper()
                );
            }

            // Get last known location to update map immediately
            Location lastKnownLocation = locationManager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                updateMapLocation(lastKnownLocation);
            }
        }
    }

    private void updateMapLocation(Location location) {
        currentLocation = location;
        GeoPoint currentPosition = new GeoPoint(location.getLatitude(), location.getLongitude());
        
        // Update marker position
        currentLocationMarker.setPosition(currentPosition);
        
        // Center map on current location
        IMapController mapController = map.getController();
        mapController.setCenter(currentPosition);
        if (map.getZoomLevelDouble() < 15) {
            mapController.setZoom(15.0);
        }
        
        // Force map refresh
        map.invalidate();
    }

    private void pauseConnection() {
        if (wifiManager.isWifiEnabled()) {
            // Save the current WiFi state
            boolean wasConnected = wifiManager.getConnectionInfo().getNetworkId() != -1;
            
            // Disable WiFi
            wifiManager.setWifiEnabled(false);
            
            // Update UI immediately
            updateConnectionInfo();
            
            Toast.makeText(this, "Connection paused for 5 minutes", Toast.LENGTH_SHORT).show();

            // Schedule WiFi re-enable after 5 minutes
            wifiReconnectHandler.postDelayed(() -> {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                    // Add a small delay to allow WiFi to fully connect before updating UI
                    wifiReconnectHandler.postDelayed(() -> {
                        updateConnectionInfo();
                        Toast.makeText(MapActivity.this, "WiFi reconnected", Toast.LENGTH_SHORT).show();
                    }, 1000); // 1 second delay
                }
            }, PAUSE_DURATION);
        } else {
            Toast.makeText(this, "WiFi is already disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectConnection() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
            // Update UI immediately
            updateConnectionInfo();
            Toast.makeText(this, "WiFi disconnected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "WiFi is already disabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        // Update connection info when resuming
        updateConnectionInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        // Stop location updates to save battery
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        updateMap();
    }

    private void updateMap() {
        if (map == null) return;

        // Clear previous overlays
        map.getOverlays().clear();

        // Add current location marker
        if (currentLocation != null) {
            GeoPoint currentPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            addLocationMarker(currentPoint, R.drawable.ic_location_blue, "Current Location");
            addLocationCircle(currentPoint, android.graphics.Color.BLUE);
        }

        // Add VPN location marker
        addLocationMarker(vpnLocation, R.drawable.ic_location_green, "VPN Location");
        addLocationCircle(vpnLocation, android.graphics.Color.GREEN);

        // Show both points
        if (currentLocation != null) {
            showBothLocations(
                new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()),
                vpnLocation
            );
        }

        map.invalidate();
    }

    private void addLocationMarker(GeoPoint position, int iconRes, String title) {
        Marker marker = new Marker(map);
        marker.setPosition(position);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(iconRes));
        marker.setTitle(title);
        map.getOverlays().add(marker);
    }

    private void addLocationCircle(GeoPoint center, int color) {
        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(center, 100000)); // 100km radius
        circle.setFillColor(color & 0x20FFFFFF);
        circle.setStrokeWidth(0);
        map.getOverlays().add(circle);
    }

    private void showBothLocations(GeoPoint point1, GeoPoint point2) {
        double north = Math.max(point1.getLatitude(), point2.getLatitude());
        double south = Math.min(point1.getLatitude(), point2.getLatitude());
        double east = Math.max(point1.getLongitudeE6() / 1E6, point2.getLongitudeE6() / 1E6);
        double west = Math.min(point1.getLongitudeE6() / 1E6, point2.getLongitudeE6() / 1E6);

        map.zoomToBoundingBox(
            new BoundingBox(north, east, south, west),
            true,
            100
        );
    }

    private void updateConnectionInfo() {
        String networkType = getNetworkType();
        
        // Update UI with connection info
        TextView ipAddressText = findViewById(R.id.ipAddressText);
        TextView locationText = findViewById(R.id.locationText);
        
        if (networkType.equals("No Connection")) {
            ipAddressText.setText("Connection disconnected");
            locationText.setText("No Connection");
        } else {
            String ipAddress = getDeviceIpAddress();
            ipAddressText.setText("IP: " + ipAddress);
            locationText.setText("Connection: " + networkType);
        }
    }

    private String getDeviceIpAddress() {
        try {
            // Check WiFi IP first
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled()) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                if (ipInt != 0) {
                    return String.format("%d.%d.%d.%d",
                            (ipInt & 0xff), (ipInt >> 8 & 0xff),
                            (ipInt >> 16 & 0xff), (ipInt >> 24 & 0xff));
                }
            }

            // Check all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        
        return "Unknown IP";
    }

    private String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Mobile Data";
                }
            }
        }
        
        return "No Connection";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onPause();
        // Remove any pending WiFi reconnection callbacks
        wifiReconnectHandler.removeCallbacksAndMessages(null);
    }
}
