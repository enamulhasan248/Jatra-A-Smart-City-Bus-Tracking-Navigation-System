package com.example.jatradriver;

import static android.os.SystemClock.sleep;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



public class MainActivity extends AppCompatActivity {
    Button privateBus,publicBus;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        privateBus = findViewById(R.id.privateBus);
        publicBus = findViewById(R.id.publicBus);


        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isRegistered = sharedPreferences.getBoolean("isRegistered", false);

        // Check if the user is already registered
        if (isRegistered) {
            // Skip registration and go directly to DriverActivity
            Intent intent = new Intent(MainActivity.this, DriverActivityForTesting.class);
            startActivity(intent);
            finish();
        }

        publicBus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i  = new Intent(MainActivity.this,Registration_page. class);
                startActivity(i);
            }
        });

        privateBus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i  = new Intent(MainActivity.this,Registration_page_private. class);
                startActivity(i);
            }
        });





    }


}