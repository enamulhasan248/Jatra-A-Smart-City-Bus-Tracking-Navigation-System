package com.example.jatradriver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashSet;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private static final int UPLOAD_INTERVAL = 5000; // 5 seconds for better accuracy

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Handler handler;
    private DatabaseReference databaseReference;

    // Get saved values
    private String type;
    private String driverName;
    private String busNumber; // public
    private String busName;   // public

    private String busNumberp;

    private String from; // private
    private String to;   // private
    private String mobile;        // private

    private String busKey; // final node name

    // Use LocationCallback instead of Runnable for fresh locations
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // For location smoothing
    private Location lastLocation = null;

    // Static flag to indicate running state
    public static volatile boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true; // mark service running
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("driver_location");
        handler = new Handler(Looper.getMainLooper());

        // Get saved values
        SharedPreferences shared = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        type = shared.getString("type", "publicBus");

        // Public bus fields
        busNumber = shared.getString("busNumber", null);
        busName = shared.getString("busName", null);

        // Private bus fields
        mobile = shared.getString("mobile", null);
        from = shared.getString("from", null);
        to = shared.getString("to", null);

        driverName = shared.getString("name", "Unknown");
        busNumberp = shared.getString("busNumberp","testfail");

        if (type.equals("publicBus") && busNumber != null) {
            busKey = "public_bus/" + busNumber;
        } else if (busNumberp != null) {
            busKey = "private_bus/" + busNumberp;
        } else {
            Log.e("LocationService", "Bus number or ID missing!");
            stopSelf();
        }



        // Initialize bus route
        initializeBusRoute();

        updateDriverStatus("active");

        // Setup location request and callback
        setupLocationUpdates();
    }

    private void setupLocationUpdates() {
        // Modern LocationRequest.Builder usage with improved accuracy settings
        try {
            locationRequest = new LocationRequest.Builder(UPLOAD_INTERVAL)
                    .setMinUpdateIntervalMillis(2000)  // Faster updates (2 seconds)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateDelayMillis(3000)     // Get batched updates faster
                    .setMinUpdateDistanceMeters(5f)    // Only update if moved 5 meters
                    .build();
        } catch (NoSuchMethodError e) {
            // Fallback (older APIs)
            locationRequest = LocationRequest.create();
            locationRequest.setInterval(UPLOAD_INTERVAL);
            locationRequest.setFastestInterval(2000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setSmallestDisplacement(5f);
        }

        // Create location callback with accuracy filtering
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null && isLocationAccurate(location)) {
                            float speed = location.hasSpeed() ? location.getSpeed() : 0f;

                            Log.d("LocationService", String.format(
                                    "Fresh Location: %.6f, %.6f | Speed: %.2f m/s | Accuracy: %.2fm",
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    speed,
                                    location.getAccuracy()
                            ));

                            uploadLocationToFirebase(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    speed
                            );
                        } else if (location != null) {
                            Log.w("LocationService", "Location rejected - Poor accuracy: "
                                    + location.getAccuracy() + "m");
                        }
                    }
                }
            }
        };
    }

    /**
     * Check if location accuracy is acceptable
     * @param location Location object to check
     * @return true if accuracy is good enough (≤ 20 meters)
     */
    private boolean isLocationAccurate(Location location) {
        // Reject locations with accuracy worse than 20 meters
        float accuracy = location.getAccuracy();

        // For bus tracking, 20m accuracy is reasonable
        // For more precise tracking, use 10m
        return accuracy > 0 && accuracy <= 20f;
    }

    /**
     * Optional: Smooth location data to reduce GPS jitter
     * @param newLocation Fresh location from GPS
     * @return Smoothed location
     */
    private Location smoothLocation(Location newLocation) {
        if (lastLocation == null) {
            lastLocation = newLocation;
            return newLocation;
        }

        // Only smooth if locations are close (prevents jumps)
        float distance = lastLocation.distanceTo(newLocation);

        if (distance < 100) { // Within 100 meters
            // Weighted average: 70% new, 30% old
            double lat = newLocation.getLatitude() * 0.7 + lastLocation.getLatitude() * 0.3;
            double lng = newLocation.getLongitude() * 0.7 + lastLocation.getLongitude() * 0.3;

            Location smoothed = new Location(newLocation);
            smoothed.setLatitude(lat);
            smoothed.setLongitude(lng);

            lastLocation = smoothed;
            return smoothed;
        }

        lastLocation = newLocation;
        return newLocation;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        startFreshLocationUpdates();

        return START_STICKY;
    }

    private void startFreshLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted");
            stopSelf(); // Stop service if no permission
            return;
        }

        // Request fresh location updates
        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );

        Log.d("LocationService", "Started fresh location updates with HIGH ACCURACY");
    }

    private void initializeBusRoute() {
        BusDriverInfo info;

        if (type.equals("publicBus")) {
            info = new BusDriverInfo(driverName, busName);
        } else {
            info = new BusDriverInfo(driverName, from, to, mobile);
        }

        databaseReference.child(busKey).child("Bus_Info")
                .setValue(info)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("LocationService", "Bus info saved");
                    } else {
                        Log.e("LocationService", "Failed to save bus info");
                    }
                });
    }

    // Upload counter for cleanup trigger
    private int uploadCounter = 0;

    private void uploadLocationToFirebase(double latitude, double longitude, float speed) {
        uploadCounter++;

        // Round to 6 decimal places (sufficient for ~11cm accuracy)
        double latRounded = Math.round(latitude * 1000000.0) / 1000000.0;
        double lngRounded = Math.round(longitude * 1000000.0) / 1000000.0;
        float speedRounded = Math.round(speed * 100f) / 100f;

        DatabaseReference shortRef = databaseReference.child(busKey).child("locations");
        DatabaseReference fullRef  = databaseReference.child(busKey).child("history_locations");

        Businfo data = new Businfo(latRounded, lngRounded, speedRounded);

        // =============== SHORT LIST UPLOAD =================
        String keyShort = shortRef.push().getKey();
        shortRef.child(keyShort).setValue(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UPLOAD_SHORT", String.format(
                                "Short list uploaded: key=%s, lat=%.6f, lng=%.6f, speed=%.2f m/s",
                                keyShort, latRounded, lngRounded, speedRounded
                        ));
                    } else {
                        Log.e("UPLOAD_SHORT", "FAILED to upload short list location");
                    }
                });

        // =============== FULL HISTORY UPLOAD ===============
        String keyFull = fullRef.push().getKey();
        fullRef.child(keyFull).setValue(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UPLOAD_HISTORY", String.format(
                                "History uploaded: key=%s, lat=%.6f, lng=%.6f, speed=%.2f m/s",
                                keyFull, latRounded, lngRounded, speedRounded
                        ));
                    } else {
                        Log.e("UPLOAD_HISTORY", "FAILED to upload history location");
                    }
                });

        // =============== CLEANUP CHECK EVERY 10 UPLOADS ===============
        if (uploadCounter >= 10) {
            uploadCounter = 0;
            Log.d("CLEANUP_CHECK", "Running cleanup trigger... (10 uploads reached)");
            cleanupShortLocations();
        }
    }

    private void cleanupShortLocations() {
        DatabaseReference shortRef = databaseReference.child(busKey).child("locations");

        shortRef.get().addOnSuccessListener(snapshot -> {
            long total = snapshot.getChildrenCount();
            Log.d("CLEANUP", "Total short list points BEFORE cleanup = " + total);

            if (total > 20) {
                shortRef.orderByKey()
                        .limitToLast(5)
                        .get()
                        .addOnSuccessListener(lastFiveSnapshot -> {
                            HashSet<String> keepKeys = new HashSet<>();

                            for (DataSnapshot child : lastFiveSnapshot.getChildren()) {
                                keepKeys.add(child.getKey());
                                Log.d("CLEANUP_KEEP", "Keeping key: " + child.getKey());
                            }

                            int delCount = 0;

                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (!keepKeys.contains(child.getKey())) {
                                    delCount++;
                                    Log.d("CLEANUP_DELETE", "Deleting key: " + child.getKey());
                                    child.getRef().removeValue();
                                }
                            }

                            Log.d("CLEANUP_RESULT", "Cleanup complete → deleted " + delCount + " old points");
                        });

            } else {
                Log.d("CLEANUP", "No cleanup needed (total <= 20)");
            }

        }).addOnFailureListener(e -> {
            Log.e("CLEANUP_ERROR", "Failed to read for cleanup: " + e.getMessage());
        });
    }

    /**
     * Update driver status in Firebase
     * @param status "active" or "inactive"
     */
    private void updateDriverStatus(String status) {
        if (busKey != null && !busKey.isEmpty()) {
            databaseReference.child(busKey).child("status").setValue(status)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("LocationService", "Status updated to: " + status);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LocationService", "Failed to update status", e);
                    });
        }
    }


    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, DriverActivityForTesting.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this,
                0, notificationIntent, android.app.PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Jatra Location Sharing")
                .setContentText("Sharing your current location to JATRA")
                .setSmallIcon(R.drawable.bus_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Set status to inactive when service is destroyed
        updateDriverStatus("inactive");
        // Stop location updates when service is destroyed
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        isRunning = false;
        lastLocation = null;
        Log.d("LocationService", "Service destroyed - location updates stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}