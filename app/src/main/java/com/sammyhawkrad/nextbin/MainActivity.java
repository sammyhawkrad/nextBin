package com.sammyhawkrad.nextbin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    private static DataViewModel dataViewModel;

    // Example parameters (replace with your desired values)
    double radius = 1000;
    //double latitude = MapFragment.userLocationLat;
    //double longitude = MapFragment.userLocationLon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        dataViewModel = new ViewModelProvider(this).get(DataViewModel.class);

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

    private static class FetchDataAsyncTask extends AsyncTask<String, Void, String> {

        private WeakReference<MainActivity> activityReference;

        FetchDataAsyncTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            if (params.length == 0) return null;

            String urlString = params[0];
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                reader.close();
                inputStream.close();

                return stringBuilder.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            if (result != null) dataViewModel.setJsonData(result);
            else Toast.makeText(activity, "Failed to fetch data", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchDataAsync(double radius, double latitude, double longitude) {
        String apiUrl = "https://overpass-api.de/api/interpreter?data=[out:json][timeout:25];(nwr[\"amenity\"=\"waste_basket\"](around:"
                + radius + "," + latitude + "," + longitude + ");nwr[\"amenity\"=\"recycling\"][\"recycling_type\"=\"container\"](around:"
                + radius + "," + latitude + "," + longitude + ");nwr[\"vending\"=\"bottle_return\"](around:"
                + radius + "," + latitude + "," + longitude + "););out center;";

        new FetchDataAsyncTask(this).execute(apiUrl);
    }

    public void update(View view) {
        fetchDataAsync(radius, MapFragment.userLocationLat, MapFragment.userLocationLon);
    }
}