package com.sammyhawkrad.nextbin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.main_view, new MapFragment()).commit();

        // Request location permissions
        @SuppressLint("MissingPermission")
        ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((Boolean.FALSE.equals(fineLocationGranted)) && (Boolean.FALSE.equals(coarseLocationGranted))) {
                        Toast.makeText(this,
                                "Location cannot be obtained due to missing permission.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        String[] PERMISSIONS = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        locationPermissionRequest.launch(PERMISSIONS);

    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        if (item.getItemId() == R.id.nav_map)
            fragment = new MapFragment();

        if (item.getItemId() ==  R.id.nav_list)
            fragment = new BinsListFragment();

        if (item.getItemId() ==  R.id.nav_preferences)
            fragment = new PreferencesFragment();

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.main_view, fragment).commit();

        return true;
    }
}