package com.sammyhawkrad.nextbin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeoUtils {

    private static final int EARTH_RADIUS_KM = 6371; // Earth radius in kilometers

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        return EARTH_RADIUS_KM * c;
    }

    public static List<JSONObject> convertOSMToGeoJSON(String osmData) {
        List<JSONObject> geoJsonFeatures = new ArrayList<>();

        try {
            JSONObject osmObject = new JSONObject(osmData);
            JSONArray elementsArray = osmObject.getJSONArray("elements");

            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject element = elementsArray.getJSONObject(i);
                JSONObject geoJsonFeature = new JSONObject();

                // Set type and geometry
                geoJsonFeature.put("type", "Feature");
                if (element.getString("type").equals("node")) {
                    geoJsonFeature.put("geometry", new JSONObject()
                            .put("type", "Point")
                            .put("coordinates", new JSONArray()
                                    .put(element.getDouble("lon"))
                                    .put(element.getDouble("lat"))));
                } else if (element.getString("type").equals("way")) {
                    geoJsonFeature.put("geometry", new JSONObject()
                            .put("type", "Point")
                            .put("coordinates", new JSONArray()
                                    .put(element.getJSONObject("center").getDouble("lon"))
                                    .put(element.getJSONObject("center").getDouble("lat"))));
                }
//                geoJsonFeature.put("geometry", new JSONObject()
//                        .put("type", "Point")
//                        .put("coordinates", new JSONArray()
//                                .put(element.getDouble("lon"))
//                                .put(element.getDouble("lat"))));

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
}