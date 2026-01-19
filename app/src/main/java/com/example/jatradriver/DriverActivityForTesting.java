package com.example.jatradriver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverActivityForTesting extends AppCompatActivity {

    private final int FINE_PERMISSION_CODE = 1;
    private final int BACKGROUND_PERMISSION_CODE = 2;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private DatabaseReference databaseReference;
    private SwitchCompat backgroundServiceSwitch;
    private TextView switchStatusText;
    private Handler handler;
    private CircleImageView profileIcon;
    private SharedPreferences sharedPreferences;

    // ActivityResultLauncher for location settings
    private ActivityResultLauncher<IntentSenderRequest> locationSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_for_testing);

        profileIcon = findViewById(R.id.profileIcon);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        profileIcon.setOnClickListener(v -> showProfileOptions());

        // Initialize services
        databaseReference = FirebaseDatabase.getInstance().getReference("driver_location");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());

        // Initialize views
        backgroundServiceSwitch = findViewById(R.id.backgroundServiceSwitch);
        switchStatusText = findViewById(R.id.switchStatusText);

        // Initialize location settings launcher
        locationSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // User enabled location
                        Toast.makeText(this, "✅ Location enabled", Toast.LENGTH_SHORT).show();
                        // Now proceed with starting the service
                        proceedToStartService();
                    } else {
                        // User declined
                        Toast.makeText(this, "❌ Location is required for tracking", Toast.LENGTH_LONG).show();
                        backgroundServiceSwitch.setChecked(false);
                        updateSwitchState();
                    }
                }
        );

        // Set initial state
        updateSwitchState();

        // Set switch listener
        backgroundServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // only act when user toggles (prevents programmatic loops)
                if (buttonView.isPressed()) {
                    if (isChecked) {
                        checkLocationPermission();
                    } else {
                        stopLocationService();
                    }
                }
            }
        });
    }

    private void showProfileOptions() {
        PopupMenu popup = new PopupMenu(this, profileIcon);

        popup.getMenu().add("Re-register");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                logoutUser();
            } else if (item.getTitle().equals("Re-register")) {
                reRegisterUser();
            }
            return true;
        });
        popup.show();
    }

    private void logoutUser() {
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        finishAffinity();  // Exit app
    }

    private void reRegisterUser() {
        // Stop the tracking service first
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);

        // Don't clear SharedPreferences here anymore
        // Just mark that re-registration is needed
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isRegistered", false);
        editor.apply();

        Toast.makeText(this, "Re-registration required", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, Registration_page_private.class);
        startActivity(intent);
        finish();
    }

    /**
     * Request/verify permissions. We request fine location first; if granted and Android
     * Q+ we also check/request background location.
     */
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_PERMISSION_CODE);
        } else {
            // FINE granted; check BACKGROUND if needed (Android Q+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // request background permission separately
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            BACKGROUND_PERMISSION_CODE);
                    // don't return; we'll start once callbacks resolve
                    return;
                }
            }
            // Permissions granted, now check if location is enabled
            checkAndRequestLocationSettings();
        }
    }

    /**
     * Check if location is enabled and prompt user to enable it if not
     */
    private void checkAndRequestLocationSettings() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            // Location is OFF - Use Google Play Services to prompt user
            promptEnableLocation();
        } else {
            // Location is already ON
            proceedToStartService();
        }
    }

    /**
     * Use Google Play Services LocationSettingsRequest to prompt user
     * This shows a native dialog to enable location without leaving the app
     */
    private void promptEnableLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // Location is already enabled
            proceedToStartService();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location is not enabled, but we can show a dialog to enable it
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                            resolvable.getResolution().getIntentSender()).build();
                    locationSettingsLauncher.launch(intentSenderRequest);
                } catch (Exception sendEx) {
                    // Failed to show dialog, fall back to manual settings
                    showManualLocationSettingsDialog();
                }
            } else {
                // Something else went wrong
                showManualLocationSettingsDialog();
            }
        });
    }

    /**
     * Fallback: Show dialog to manually open location settings
     */
    private void showManualLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Required")
                .setMessage("Please enable location services to start tracking.\n\nGo to Settings → Location and turn it ON.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    backgroundServiceSwitch.setChecked(false);
                    updateSwitchState();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Actually start the location tracking service
     */
    private void proceedToStartService() {
        startLocationService();
    }

    private void startLocationService() {
        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "🚀 Live tracking started!", Toast.LENGTH_SHORT).show();

                // Update UI after a short delay
                handler.postDelayed(this::updateSwitchState, 1000);

            } catch (Exception e) {
                Toast.makeText(this, "❌ Failed to start service", Toast.LENGTH_SHORT).show();
                backgroundServiceSwitch.setChecked(false);
                updateSwitchState();
            }
        } else {
            updateSwitchState();
            Toast.makeText(this, "✅ Tracking already active", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            try {
                stopService(serviceIntent);
                Toast.makeText(this, "🛑 Tracking stopped", Toast.LENGTH_SHORT).show();

                // Update UI after a short delay
                handler.postDelayed(this::updateSwitchState, 1000);

            } catch (Exception e) {
                Toast.makeText(this, "❌ Failed to stop service", Toast.LENGTH_SHORT).show();
                backgroundServiceSwitch.setChecked(true);
                updateSwitchState();
            }
        } else {
            updateSwitchState();
            Toast.makeText(this, "ℹ️ Tracking not active", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Replaced the unreliable getRunningServices approach with a static flag in the Service.
     */
    private boolean isServiceRunning() {
        return LocationTrackingService.isRunning;
    }

    private void updateSwitchState() {
        boolean running = isServiceRunning();

        // Update switch position
        backgroundServiceSwitch.setChecked(running);

        // Update status text
        if (running) {
            switchStatusText.setText("ACTIVE");
            switchStatusText.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            switchStatusText.setText("INACTIVE");
            switchStatusText.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // fine granted — check background (if needed) in checkLocationPermission()
                checkLocationPermission();
            } else {
                // Permission denied, uncheck the switch
                backgroundServiceSwitch.setChecked(false);
                updateSwitchState();
                Toast.makeText(this, "❌ Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == BACKGROUND_PERMISSION_CODE) {
            // Background permission callback: if granted great; if denied we warn and still start service (fine is already granted)
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Background location granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Background location NOT granted", Toast.LENGTH_LONG).show();
            }
            // Start service anyway if FINE was granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                checkAndRequestLocationSettings();
            } else {
                backgroundServiceSwitch.setChecked(false);
                updateSwitchState();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update switch state when activity resumes
        updateSwitchState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}