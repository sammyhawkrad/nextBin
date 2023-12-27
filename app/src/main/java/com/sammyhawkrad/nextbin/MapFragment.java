package com.sammyhawkrad.nextbin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    Integer DEFAULT_ZOOM = 17;
    static Double userLocationLat;
    static Double userLocationLon;

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
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
        gMap.setInfoWindowAdapter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Observe changes in the data
        DataViewModel dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        dataViewModel.getJsonData().observe(getViewLifecycleOwner(), jsonData -> {
            // Update the map with jsonData
            if (jsonData != null && gMap != null) {
                List<JSONObject> geoJsonFeatures = convertOSMToGeoJSON(jsonData);

                for (JSONObject feature : geoJsonFeatures) {
                    addMarkerToMap(feature);
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
        // Update user location variables
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    // Update user location variables
                    userLocationLat = location.getLatitude();
                    userLocationLon = location.getLongitude();
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

    private List<JSONObject> convertOSMToGeoJSON(String osmData) {
        List<JSONObject> geoJsonFeatures = new ArrayList<>();

        try {
            JSONObject osmObject = new JSONObject(osmData);
            JSONArray elementsArray = osmObject.getJSONArray("elements");

            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject element = elementsArray.getJSONObject(i);
                JSONObject geoJsonFeature = new JSONObject();

                // Set type and geometry
                geoJsonFeature.put("type", "Feature");
                geoJsonFeature.put("geometry", new JSONObject()
                        .put("type", "Point")
                        .put("coordinates", new JSONArray()
                                .put(element.getDouble("lon"))
                                .put(element.getDouble("lat"))));

                // Set properties
                JSONObject properties = new JSONObject();
                properties.put("id", element.getLong("id"));

                // Add tags to properties
                if (element.has("tags")) {
                    JSONObject tags = element.getJSONObject("tags");
                    properties.put("tags", tags);
                }

                geoJsonFeature.put("properties", properties);

                geoJsonFeatures.add(geoJsonFeature);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return geoJsonFeatures;
    }

    private void addMarkerToMap(JSONObject feature) {
        try {
            JSONObject geometry = feature.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            JSONObject tags = feature.getJSONObject("properties").optJSONObject("tags");
            double lat = coordinates.getDouble(1);
            double lon = coordinates.getDouble(0);

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .title(formatTag(tags.get("amenity").toString().replace("_", " ")))
                    .snippet(getSnippetFromTags(tags));

            gMap.addMarker(markerOptions);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        // Return null to use the default info window
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // Inflate the custom info window layout
        View view = getLayoutInflater().inflate(R.layout.bin_info_window, null);

        // Set the content for the custom info window
        TextView titleTextView = view.findViewById(R.id.tvInfoWindowTitle);
        TextView snippetTextView = view.findViewById(R.id.tvInfoWindowSnippet);

        titleTextView.setText(marker.getTitle());
        snippetTextView.setText(marker.getSnippet());

        return view;
    }

    static String getSnippetFromTags(JSONObject tags) {
        String snippet = "";

        try {snippet += "\nName: " + tags.get("name");} catch (JSONException ignored) {}
        try {snippet += "\nOperator: " + tags.get("operator");} catch (JSONException ignored) {}
        try {snippet += "\nWaste: " + formatTag(tags.get("waste").toString().replace("_", " "));} catch (JSONException ignored) {}
        try {snippet += "\nMaterial: " + formatTag(tags.get("material").toString().replace("_", " "));} catch (JSONException ignored) {}
        try {snippet += "\nColour: " + formatTag(tags.get("colour").toString().replace("_", " "));} catch (JSONException ignored) {}
        try {snippet += "\nVending: " + formatTag(tags.get("vending").toString().replace("_", " "));} catch (JSONException ignored) {}
        try {snippet += "\nOpening Hours: " + tags.get("opening_hours");} catch (JSONException ignored) {}
        try {snippet += "\nDescription: " + tags.get("description");} catch (JSONException ignored) {}
        try {snippet += "\nNote: " + tags.get("note");} catch (JSONException ignored) {}
        try {snippet += "\nBatteries: " + formatTag(tags.get("recycling:batteries").toString());} catch (JSONException ignored) {}
        try {snippet += "\nCans: " + formatTag(tags.get("recycling:cans").toString());} catch (JSONException ignored) {}
        try {snippet += "\nClothes: " + formatTag(tags.get("recycling:clothes").toString());} catch (JSONException ignored) {}
        try {snippet += "\nGlass Bottles: " + formatTag(tags.get("recycling:glass_bottles").toString());} catch (JSONException ignored) {}
        try {snippet += "\nPlastic Bottles: " + formatTag(tags.get("recycling:plastic_bottles").toString());} catch (JSONException ignored) {}
        try {snippet += "\nPaper: " + formatTag(tags.get("recycling:paper").toString());} catch (JSONException ignored) {}

        return snippet;
    }

    static String formatTag(String input) {
        return Arrays.stream(input.split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}