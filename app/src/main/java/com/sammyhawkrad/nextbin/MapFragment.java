package com.sammyhawkrad.nextbin;

import static com.sammyhawkrad.nextbin.GeoUtils.convertOSMToGeoJSON;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
    DataViewModel dataViewModel;

    static List<JSONObject> geoJsonFeatures;

    boolean IS_FIRST_TIME = true;

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

        //show search button on map
        Button btn_Search = ((MainActivity) requireActivity()).btn_Search;
        if (gMap.getMaxZoomLevel() > 14.0f) btn_Search.setVisibility(View.VISIBLE);


        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(true);


            // Zoom to current location
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null && IS_FIRST_TIME) {
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                        }
                        if (location != null && !IS_FIRST_TIME) {
                            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
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

        // Retrieve data from DataViewModel
        dataViewModel.getJsonData().observe(getViewLifecycleOwner(), jsonData -> {
            if (jsonData != null) {
                List<JSONObject> geoJsonFeatures = convertOSMToGeoJSON(jsonData);
                MapFragment.geoJsonFeatures = geoJsonFeatures;
                IS_FIRST_TIME = false;

                for (JSONObject feature : geoJsonFeatures) {
                    addMarkerToMap(feature);
                }
            }
        });

        customizeMapUI();
        gMap.setInfoWindowAdapter(this);
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
        dataViewModel.getJsonData().observe(getViewLifecycleOwner(), jsonData -> {
            // Update the map with jsonData
            if (jsonData != null && gMap != null) {
                List<JSONObject> geoJsonFeatures = convertOSMToGeoJSON(jsonData);
                IS_FIRST_TIME = false;

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


    static String markerTitle(JSONObject tags) throws JSONException {
        if (tags.get("amenity").toString().equals("recycling")) {
            return formatTag(tags.get("amenity") + " Bin");
        } else {
            return formatTag(tags.get("amenity").toString().replace("_", " "));
        }
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
                    .title(markerTitle(tags))
                    .snippet(getSnippetFromTags(tags))
                    .icon(getMarkerIcon(tags.get("amenity").toString()));

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

    private BitmapDescriptor getMarkerIcon(String dataAttribute) {
        int iconResourceId;

        // Determine the appropriate drawable resource based on data attribute
        if ("waste_basket".equals(dataAttribute)) {
            iconResourceId = R.drawable.sc_bin;
        } else if ("recycling".equals(dataAttribute)) {
            iconResourceId = R.drawable.sc_recycling;
        } else {
            iconResourceId = R.drawable.sc_bottle;
        }

        return BitmapDescriptorFactory.fromResource(iconResourceId);
    }

    private String getSnippetFromTags(JSONObject tags) {
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

    private void customizeMapUI() {
        // Customize zoom controls and my location button positions
        View zoomControls = getView().findViewById(0x1);
        View myLocationButton = getView().findViewById(0x2);

        if (zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams
                && myLocationButton != null && myLocationButton.getLayoutParams() instanceof RelativeLayout.LayoutParams) {

            RelativeLayout.LayoutParams zoomControlsParams = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();
            RelativeLayout.LayoutParams myLocationButtonParams = (RelativeLayout.LayoutParams) myLocationButton.getLayoutParams();

            // Switch the positions of the zoom controls and my location button
            myLocationButtonParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            myLocationButtonParams.removeRule(RelativeLayout.ALIGN_PARENT_END);

            zoomControlsParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            zoomControlsParams.removeRule(RelativeLayout.ALIGN_PARENT_START);

            myLocationButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            myLocationButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END);

            zoomControlsParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            zoomControlsParams.addRule(RelativeLayout.ALIGN_PARENT_END);

            zoomControlsParams.setMargins(0, 16, 16, 0);
            myLocationButtonParams.setMargins(0, 16, 16, 40);

            // Apply the updated layout parameters
            zoomControls.setLayoutParams(zoomControlsParams);
            myLocationButton.setLayoutParams(myLocationButtonParams);
        }
    }
}