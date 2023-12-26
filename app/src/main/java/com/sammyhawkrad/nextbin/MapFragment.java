package com.sammyhawkrad.nextbin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    Integer DEFAULT_ZOOM = 17;

    private GoogleMap gMap;

    static Double userLocationLat;
    static Double userLocationLon;


    private static DataViewModel dataViewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap gMap) {
        this.gMap = gMap;
        LatLng defaultLocation = new LatLng(5.58063, -0.19458);
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setZoomGesturesEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12.0f));

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(true);


            // Zoom to current location
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                        }
                    });

        } else {
            Toast.makeText(requireContext(),
                    "This app requires location permission.",
                    Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        setLocationUpdateListener();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Observe changes in the data
        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        dataViewModel.getJsonData().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String jsonData) {
                // Update the map with jsonData
                if (jsonData != null && gMap != null) {
                    List<LatLng> latLngs = parseOSMData(jsonData);
                    addMarkersToMap(latLngs);
                }
            }
        });


        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());

    }

    private void setLocationUpdateListener() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (android.location.Location location : locationResult.getLocations()) {
                        // Update user location variables
                        userLocationLat = location.getLatitude();
                        userLocationLon = location.getLongitude();
                    }
                }
            }
        };

        // Check location permission and request if still necessary
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest()
                .setInterval(5000) // 5 seconds
                .setFastestInterval(3000) // 3 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    private List<LatLng> parseOSMData(String jsonData) {

        List<LatLng> latLngs = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray elementsArray = jsonObject.getJSONArray("elements");

            if (elementsArray.length() == 0) {
                Toast.makeText(requireContext(), "No bins nearby", Toast.LENGTH_LONG).show();
            }
            else {

                for (int i = 0; i < elementsArray.length(); i++) {
                    JSONObject element = elementsArray.getJSONObject(i);

                    if (element.has("type") && element.getString("type").equals("node")) {
                        double lat = element.getDouble("lat");
                        double lon = element.getDouble("lon");

                        LatLng latLng = new LatLng(lat, lon);
                        latLngs.add(latLng);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return latLngs;
    }

    private void addMarkersToMap(List<LatLng> latLngs) {
        // Add markers to the map
        for (LatLng latLng : latLngs) {
            gMap.addMarker(new MarkerOptions().position(latLng));
        }
    }

}