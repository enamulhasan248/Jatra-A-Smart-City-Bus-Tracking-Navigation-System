package com.example.jatradriver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Registration_page extends AppCompatActivity {

    private EditText nameEditText, busNumberEditText, busNameEditText;
    private Button registerButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration_page);
        databaseReference = FirebaseDatabase.getInstance().getReference("driver_location/public_bus");


        // Initialize the EditTexts and Button
        nameEditText = findViewById(R.id.name_edit_text);
        busNumberEditText = findViewById(R.id.bus_number_edit_text);
        busNameEditText = findViewById(R.id.bus_name_edit_text);
        registerButton = findViewById(R.id.register_button);

        // SharedPreferences to store registration data
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isRegistered = sharedPreferences.getBoolean("isRegistered", false);


        // Check if the user is already registered
        if (isRegistered) {
            // Skip registration and go directly to DriverActivity
            Intent intent = new Intent(Registration_page.this, DriverActivityForTesting.class);
            startActivity(intent);
            finish();
        }

        // Register Button click listener
        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String busNumber = busNumberEditText.getText().toString();
            String busName = busNameEditText.getText().toString();
            String type = "publicBus";

            // Validate inputs
            if (name.isEmpty() || busNumber.isEmpty() || busName.isEmpty()) {
                Toast.makeText(Registration_page.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {

                String oldBusId = sharedPreferences.getString("busNumber", null);

                // Delete old driver location from Firebase if it exists
                if (oldBusId != null && !oldBusId.isEmpty()) {
                    databaseReference.child(oldBusId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Registration_page.this,
                                        "Old driver data removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Registration_page.this,
                                        "Failed to remove old data", Toast.LENGTH_SHORT).show();
                            });
                }
                // Save data to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("name", name);
                editor.putString("busNumber", busNumber);
                editor.putString("busName", busName);
                editor.putBoolean("isRegistered", true);
                editor.putString("type",type);
                editor.apply();

                // Navigate to DriverActivity
                Intent intent = new Intent(Registration_page.this, DriverActivityForTesting.class);
                startActivity(intent);
                finish();
            }
        });


    }
}
