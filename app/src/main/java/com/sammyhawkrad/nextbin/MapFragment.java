package com.sammyhawkrad.nextbin;

import static com.sammyhawkrad.nextbin.GeoUtils.convertOSMToGeoJSON;
import static com.sammyhawkrad.nextbin.PreferencesFragment.RECYCLING_BIN;
import static com.sammyhawkrad.nextbin.PreferencesFragment.VENDING_MACHINE;
import static com.sammyhawkrad.nextbin.PreferencesFragment.WASTE_BASKET;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    Integer DEFAULT_ZOOM = 17;
    static Double userLocationLat;
    static Double userLocationLon;
    DataViewModel dataViewModel;

    static List<JSONObject> geoJsonFeatures;

    // Used to determine whether last location is known
    boolean IS_FIRST_TIME = true;

    // Used to determine whether map is being opened from list view
    boolean FROM_LIST = false;

    private GoogleMap gMap;

    LocationManager locationManager;

    LocationListener locationListener;

    @SuppressLint({"MissingPermission", "PotentialBehaviorOverride"})
    @Override
    public void onMapReady(GoogleMap gMap) {
        this.gMap = gMap;

        // Set map style
        gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

        LatLng defaultLocation = new LatLng(5.58063, -0.19458);
        gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setZoomGesturesEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);

        if (IS_FIRST_TIME) gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12.0f));

        //show search button on map
        Button btn_Search = ((MainActivity) requireActivity()).btn_Search;
        if (gMap.getMaxZoomLevel() > 14.0f) btn_Search.setVisibility(View.VISIBLE);
        else btn_Search.setVisibility(View.GONE);
        ((MainActivity) requireActivity()).btn_info.setVisibility(View.VISIBLE);

        locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {

                if (location != null) {
                    userLocationLat = location.getLatitude();
                    userLocationLon = location.getLongitude();
                }

                if (location != null && IS_FIRST_TIME && !FROM_LIST) {
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                }
                if (location != null && !IS_FIRST_TIME && !FROM_LIST) {
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Retrieve data from DataViewModel
        dataViewModel.getJsonData().observe(getViewLifecycleOwner(), jsonData -> {
            if (jsonData != null) {
                List<JSONObject> geoJsonFeatures = convertOSMToGeoJSON(jsonData);

                // Filter geoJsonFeatures based on preferences
                List<JSONObject> filteredGeoJsonFeatures = new ArrayList<>();

                for (JSONObject feature : geoJsonFeatures) {
                    boolean isWasteBasket = false;
                    boolean isRecyclingBin = false;
                    boolean isVendingMachine = false;

                    JSONObject tags;

                    try {
                        tags = feature.getJSONObject("properties").optJSONObject("tags");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    assert tags != null;
                    try {isWasteBasket = tags.get("amenity").toString().equals("waste_basket");} catch (JSONException ignored) {}
                    try {isRecyclingBin = tags.get("amenity").toString().equals("recycling"); } catch (JSONException ignored) {}
                    try {isVendingMachine = tags.get("vending").toString().equals("bottle_return");} catch (JSONException ignored) {}

                    if (isWasteBasket && WASTE_BASKET) filteredGeoJsonFeatures.add(feature);
                    if (isRecyclingBin && RECYCLING_BIN) filteredGeoJsonFeatures.add(feature);
                    if (isVendingMachine && VENDING_MACHINE) filteredGeoJsonFeatures.add(feature);
                }

                // Update geoJsonFeatures
                MapFragment.geoJsonFeatures = filteredGeoJsonFeatures;
                IS_FIRST_TIME = false;

                // Add markers to map
                for (JSONObject feature : MapFragment.geoJsonFeatures) addMarkerToMap(feature);
            }
        });


        Bundle args = getArguments();
        if (args != null) {
            int receivedBinIndex = args.getInt("binPosition", -1);

            if (receivedBinIndex >= 0) {

                FROM_LIST = true;

                // Get bin data at binPosition from geoJsonFeatures
                JSONObject receivedBin = geoJsonFeatures.get(receivedBinIndex);
                double receivedBinLat = 0;
                double receivedBinLon = 0;

                try {
                    receivedBinLat = receivedBin.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1);
                    receivedBinLon = receivedBin.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0);
                } catch (JSONException ignored) {}

                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(receivedBinLat, receivedBinLon), DEFAULT_ZOOM + 2));

                // Show info window for bin
                try {
                    Marker marker = gMap.addMarker(new MarkerOptions()
                            .position(new LatLng(receivedBinLat, receivedBinLon))
                            .title(markerTitle(Objects.requireNonNull(receivedBin.getJSONObject("properties").optJSONObject("tags"))))
                            .snippet(getSnippetFromTags(Objects.requireNonNull(receivedBin.getJSONObject("properties").optJSONObject("tags"))))
                            .icon(getMarkerIcon(receivedBin.getJSONObject("properties").optJSONObject("tags").get("amenity").toString())));
                    assert marker != null;
                    marker.showInfoWindow();
                } catch (JSONException ignored) {}
            }
        }

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

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        @SuppressLint("MissingPermission") ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
                        gMap.setMyLocationEnabled(true);
                        gMap.getUiSettings().setMyLocationButtonEnabled(true);
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                        gMap.setMyLocationEnabled(true);
                        gMap.getUiSettings().setMyLocationButtonEnabled(true);
                    } else {
                        Toast.makeText(requireContext(), "Location cannot be obtained due to missing permission.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        locationPermissionRequest.launch(PERMISSIONS);
    }

    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        // Return null to use the default info window
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // Inflate the custom info window layout
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.bin_info_window, null);

        // Set the content for the custom info window
        TextView titleTextView = view.findViewById(R.id.tvInfoWindowTitle);
        TextView snippetTextView = view.findViewById(R.id.tvInfoWindowSnippet);

        titleTextView.setText(marker.getTitle());
        snippetTextView.setText(marker.getSnippet());

        return view;
    }

    // stop location updates on destroy
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    /////////////////////////////////////////////////// HELPER METHODS //////////////////////////////////////////////////

    static String markerTitle(JSONObject tags) throws JSONException {
        if (tags.toString().contains("recycling")) {
            return formatTag(tags.get("amenity") + " Bin");
        } else if(tags.toString().contains("waste_basket")) {
            return formatTag(tags.get("amenity").toString().replace("_", " "));
        } else {
            return "Vending Machine";
        }
    }
    private void addMarkerToMap(JSONObject feature) {
        try {
            JSONObject geometry = feature.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            JSONObject tags = feature.getJSONObject("properties").optJSONObject("tags");
            double lat = coordinates.getDouble(1);
            double lon = coordinates.getDouble(0);

            assert tags != null;
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(lat, lon))
                    .title(markerTitle(tags))
                    .snippet(getSnippetFromTags(tags))
                    .icon( tags.toString().contains("amenity") ? getMarkerIcon(tags.get("amenity").toString()) : getMarkerIcon(tags.get("vending").toString()));

            gMap.addMarker(markerOptions);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private BitmapDescriptor getMarkerIcon(String binType) {
        int iconResourceId;

        // Determine the appropriate marker icon based on bin type
        if ("waste_basket".equals(binType)) {
            iconResourceId = R.drawable.sc_bin;
        } else if ("recycling".equals(binType)) {
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
        @SuppressLint("ResourceType") View zoomControls = getView().findViewById(0x1);
        @SuppressLint("ResourceType") View myLocationButton = getView().findViewById(0x2);

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