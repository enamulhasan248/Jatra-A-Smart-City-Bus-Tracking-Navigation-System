package com.example.jatradriver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Registration_page_private extends AppCompatActivity {

    private DatabaseReference databaseReference;

    // Input Fields (EditTexts)
    private TextInputEditText etName, etPhone, etStart, etEnd, etVehicle;

    // Layouts (Required for setting Hints)
    private TextInputLayout layoutName, layoutPhone, layoutStart, layoutEnd, layoutVehicle;

    // Buttons & Views
    private MaterialButton btnSubmit, btnBangla, btnEnglish;
    private MaterialButtonToggleGroup languageToggleGroup;
    private TextView headerTitle, subHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration_page_private);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("driver_location/private_bus");

        // 1. Initialize all Views
        initializeViews();

        // 2. SharedPreferences Check (If already logged in)
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isRegistered = sharedPreferences.getBoolean("isRegistered", false);

        if (isRegistered) {
            goToDriverActivity();
        }

        // 3. Setup Language Toggle Logic
        setupLanguageToggle();

        // 4. Submit Button Logic
        btnSubmit.setOnClickListener(v -> {
            // Get text from inputs
            String name = etName.getText().toString().trim();
            String mobile = etPhone.getText().toString().trim();
            String from = etStart.getText().toString().trim(); // Will be "East West University"
            String to = etEnd.getText().toString().trim();
            String rawbusnuber = etVehicle.getText().toString().trim();
            String type = "privateBus";
            String busNumberp = toFirebaseKey(rawbusnuber);

            // Validation
            if (name.isEmpty() || mobile.isEmpty() || from.isEmpty() || to.isEmpty() || busNumberp.isEmpty()) {
                // Show error message based on selected language
                String errorMsg = (languageToggleGroup.getCheckedButtonId() == R.id.btnBangla)
                        ? "অনুগ্রহ করে সব তথ্য পূরণ করুন"
                        : "Please fill in all fields";
                Toast.makeText(Registration_page_private.this, errorMsg, Toast.LENGTH_SHORT).show();
            } else {
                String oldBusnumber = sharedPreferences.getString("busNumberp", null);
                // Delete old driver location from Firebase if it exists
                if (oldBusnumber != null && !oldBusnumber.isEmpty()) {
                    databaseReference.child(oldBusnumber).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Registration_page_private.this,
                                        "Old driver data removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Registration_page_private.this,
                                        "Failed to remove old data", Toast.LENGTH_SHORT).show();
                            });
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                // Save data to SharedPreferences

                editor.putString("name", name);
                editor.putString("mobile", mobile);  // Added Phone
                editor.putString("from", from);
                editor.putString("to", to);
                editor.putString("busNumberp", busNumberp);
                editor.putString("type", type);
                editor.putBoolean("isRegistered", true);
                editor.apply();

                // Navigate to next screen
                goToDriverActivity();
            }
        });
    }

    private void initializeViews() {
        // Layouts (for hints)
        layoutName = findViewById(R.id.layoutName);
        layoutPhone = findViewById(R.id.layoutPhone);
        layoutStart = findViewById(R.id.layoutStart);
        layoutEnd = findViewById(R.id.layoutEnd);
        layoutVehicle = findViewById(R.id.layoutVehicle);

        // EditTexts (for text input)
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etStart = findViewById(R.id.etStart);
        etEnd = findViewById(R.id.etEnd);
        etVehicle = findViewById(R.id.etVehicle);

        // Buttons & Headers
        btnSubmit = findViewById(R.id.btnSubmit);
        languageToggleGroup = findViewById(R.id.languageToggleGroup);
        btnBangla = findViewById(R.id.btnBangla);
        btnEnglish = findViewById(R.id.btnEnglish);
        headerTitle = findViewById(R.id.headerTitle);

    }

    private void setupLanguageToggle() {
        // Set default selection to English
        languageToggleGroup.check(R.id.btnEnglish);
        updateLanguageUI(R.id.btnEnglish);

        // Listener for changes
        languageToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateLanguageUI(checkedId);
            }
        });
    }

    private void updateLanguageUI(int checkedId) {
        if (checkedId == R.id.btnBangla) {
            // --- SWITCH TO BANGLA ---

            // Highlight Button (Light overlay)
            btnBangla.setBackgroundColor(Color.parseColor("#4DFFFFFF"));
            btnEnglish.setBackgroundColor(Color.parseColor("#15314F")); // Reset to default theme color

            // Update Texts
            headerTitle.setText("একাউন্ট খুলুন");


            layoutName.setHint("চালকের নাম");
            layoutPhone.setHint("মোবাইল নাম্বার");
            layoutStart.setHint("রুট-শুরু");
            layoutEnd.setHint("রুট-শেষ");
            layoutVehicle.setHint("ট্রান্সপোর্ট  নাম্বার");

            btnSubmit.setText("জমা দিন");

        } else if (checkedId == R.id.btnEnglish) {
            // --- SWITCH TO ENGLISH ---

            // Highlight Button
            btnEnglish.setBackgroundColor(Color.parseColor("#4DFFFFFF"));
            btnBangla.setBackgroundColor(Color.parseColor("#15314F"));

            // Update Texts
            headerTitle.setText("Create Account");


            layoutName.setHint("Driver Name");
            layoutPhone.setHint("Phone Number");
            layoutStart.setHint("Start Location");
            layoutEnd.setHint("End Location");
            layoutVehicle.setHint("Transport Number");

            btnSubmit.setText("Submit");
        }
    }

    private void goToDriverActivity() {
        Intent intent = new Intent(Registration_page_private.this, DriverActivityForTesting.class);
        startActivity(intent);
        finish();
    }
    private String toFirebaseKey(String raw) {
        if (raw == null) return null;

        String key = raw.trim();
        // Replace forbidden chars: . # $ [ ]
        return key.replaceAll("[.#$\\[\\]]", "_");
    }


}