package com.sammyhawkrad.nextbin;

import static com.sammyhawkrad.nextbin.PreferencesFragment.*;
import static com.sammyhawkrad.nextbin.PreferencesFragment.RADIUS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    Button btn_Search;
    private static DataViewModel dataViewModel;

    DatabaseHelper dbHelper = new DatabaseHelper(this);
    static SQLiteDatabase database = null;
    Cursor dbCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind
        btn_Search = findViewById(R.id.btn_search);
        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Initialise data view model
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

        // Initialise database
        try {dbHelper.createDataBase();} catch (IOException ioe) {}
        database = dbHelper.getDataBase();

        dbCursor = database.rawQuery("SELECT * FROM preferences", null);

        Log.d("MainActivity", "onCreate: " + dbCursor);

        // get values from database
        int index_preference = dbCursor.getColumnIndex("preference");
        int index_value = dbCursor.getColumnIndex("value");

        dbCursor.moveToFirst();
        RADIUS = dbCursor.getInt(index_value);

        dbCursor.moveToNext();
        WASTE_BASKET = Objects.equals(dbCursor.getString(index_preference), "waste_basket") && dbCursor.getInt(index_value) == 1;

        dbCursor.moveToNext();
        RECYCLING_BIN = Objects.equals(dbCursor.getString(index_preference), "recycling_bin") && dbCursor.getInt(index_value) == 1;

        dbCursor.moveToNext();
        VENDING_MACHINE = Objects.equals(dbCursor.getString(index_preference), "vending_machine") && dbCursor.getInt(index_value) == 1;



    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        if (item.getItemId() == R.id.nav_map)
            fragment = new MapFragment();

        if (item.getItemId() ==  R.id.nav_list)
            fragment = new BinsRecyclerViewFragment();

        if (item.getItemId() ==  R.id.nav_preferences)
            fragment = new PreferencesFragment();

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.main_view, fragment).commit();

        return true;
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

///////////////////////////////////////////////////////// HELPER METHODS ///////////////////////////////////////////////

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

            // Add result to data view model
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
        fetchDataAsync(RADIUS, MapFragment.userLocationLat, MapFragment.userLocationLon);
    }

    public void savePreferences(View view) {
        // Save preferences to database
        database.execSQL("UPDATE preferences SET value = " + RADIUS + " WHERE preference = 'radius';");
        database.execSQL("UPDATE preferences SET value = " + (WASTE_BASKET ? 1 : 0) + " WHERE preference = 'waste_basket';");
        database.execSQL("UPDATE preferences SET value = " + (RECYCLING_BIN ? 1 : 0) + " WHERE preference = 'recycling_bin';");
        database.execSQL("UPDATE preferences SET value = " + (VENDING_MACHINE ? 1 : 0) + " WHERE preference = 'vending_machine';");

        Toast.makeText(this, "Preferences saved", Toast.LENGTH_SHORT).show();

        // Return to map view
        //((MainActivity) requireActivity()).onNavigationItemSelected(((MainActivity) requireActivity()).navigationView.getMenu().getItem(0));
    }
}