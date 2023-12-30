package com.sammyhawkrad.nextbin;

import static com.sammyhawkrad.nextbin.MapFragment.formatTag;
import static com.sammyhawkrad.nextbin.MapFragment.geoJsonFeatures;
import static com.sammyhawkrad.nextbin.MapFragment.markerTitle;
import static com.sammyhawkrad.nextbin.MapFragment.userLocationLat;
import static com.sammyhawkrad.nextbin.MapFragment.userLocationLon;
import static com.sammyhawkrad.nextbin.PreferencesFragment.RECYCLING_BIN;
import static com.sammyhawkrad.nextbin.PreferencesFragment.VENDING_MACHINE;
import static com.sammyhawkrad.nextbin.PreferencesFragment.WASTE_BASKET;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class BinRecyclerViewAdapter extends RecyclerView.Adapter<BinRecyclerViewAdapter.ViewHolder> {

    boolean isWasteBasket;
    boolean isRecyclingBin;
    boolean isVendingMachine;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_bin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject bin = geoJsonFeatures.get(position);


        try {
            JSONObject tags = bin.getJSONObject("properties").optJSONObject("tags");
            JSONArray coordinates = bin.getJSONObject("geometry").getJSONArray("coordinates");
            double lat = coordinates.getDouble(1);
            double lon = coordinates.getDouble(0);

            assert tags != null;
            try {isWasteBasket = tags.get("amenity").toString().equals("waste_basket");} catch (JSONException ignored) {}
            try {isRecyclingBin = tags.get("amenity").toString().equals("recycling"); } catch (JSONException ignored) {}
            try {isVendingMachine = tags.get("vending").toString().equals("bottle_return");} catch (JSONException ignored) {}

            // Bind data to views
            if (isWasteBasket && WASTE_BASKET) bindDataViews(holder, tags, lat, lon);
            else if (isRecyclingBin && RECYCLING_BIN) bindDataViews(holder, tags, lat, lon);
            else if (isVendingMachine && VENDING_MACHINE) bindDataViews(holder, tags, lat, lon);
            else holder.itemView.setVisibility(View.INVISIBLE);

//            holder.lf_tvAmenity.setText(markerTitle(tags));
//            holder.lf_tvSnippet.setText(getSnippetFromTags(tags));
//            if (tags.has("amenity")) holder.lf_ivIcon.setImageResource(getBinIcon(tags.get("amenity").toString()));
//            else holder.lf_ivIcon.setImageResource(getBinIcon(tags.get("vending").toString()));
//            holder.lf_tvDistance.setText(String.format(Locale.UK,"%.2f", GeoUtils.calculateDistance(lat, lon, userLocationLat, userLocationLon)));
//
//            // Set visibility of views
//            if (isWasteBasket && !WASTE_BASKET) holder.itemView.setVisibility(View.GONE);
//            else if (isRecyclingBin && !RECYCLING_BIN) holder.itemView.setVisibility(View.GONE);
//            else if (isVendingMachine && !VENDING_MACHINE) holder.itemView.setVisibility(View.GONE);
//            else holder.itemView.setVisibility(View.VISIBLE);
//
//            // Subtract position from geoJsonFeatures.size() if item is not visible
//            if (holder.itemView.getVisibility() == View.GONE) {
//                geoJsonFeatures.remove(position);
//            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void bindDataViews(ViewHolder holder, JSONObject tags, double lat, double lon) throws JSONException {
        // Bind data to views
        holder.lf_tvAmenity.setText(markerTitle(tags));
        holder.lf_tvSnippet.setText(getSnippetFromTags(tags));
        if (tags.has("amenity")) holder.lf_ivIcon.setImageResource(getBinIcon(tags.get("amenity").toString()));
        else holder.lf_ivIcon.setImageResource(getBinIcon(tags.get("vending").toString()));
        holder.lf_tvDistance.setText(String.format(Locale.UK,"%.2f", GeoUtils.calculateDistance(lat, lon, userLocationLat, userLocationLon)));
    }

    @Override
    public int getItemCount() {
        if (geoJsonFeatures == null) {
            return 0;
        }
        return geoJsonFeatures.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView lf_tvAmenity;
        TextView lf_tvSnippet;

        TextView lf_tvDistance;

        ImageView lf_ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            lf_tvAmenity = itemView.findViewById(R.id.lf_tvAmenity);
            lf_tvSnippet = itemView.findViewById(R.id.lf_tvSnippet);
            lf_ivIcon = itemView.findViewById(R.id.lf_ivIcon);
            lf_tvDistance = itemView.findViewById(R.id.lf_tvDistance);
        }
    }

    private int getBinIcon(String dataAttribute) {
        int binDrawable;

        // Determine the appropriate drawable resource based on data attribute
        if ("waste_basket".equals(dataAttribute)) {
            binDrawable = R.drawable.sc_bin;
        } else if ("recycling".equals(dataAttribute)) {
            binDrawable = R.drawable.sc_recycling;
        } else {
            binDrawable = R.drawable.sc_bottle;
        }

        return binDrawable;
    }


    private String getSnippetFromTags(JSONObject tags) {
        String snippet = "";

        try {snippet += "Name: " + tags.get("name") + "  ";} catch (JSONException ignored) {}
        try {snippet += "Operator: " + tags.get("operator") + "  ";} catch (JSONException ignored) {}
        try {snippet += "Waste: " + formatTag(tags.get("waste").toString().replace("_", " ")) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Material: " + formatTag(tags.get("material").toString().replace("_", " ")) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Colour: " + formatTag(tags.get("colour").toString().replace("_", " ")) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Vending: " + formatTag(tags.get("vending").toString().replace("_", " ")) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Batteries: " + formatTag(tags.get("recycling:batteries").toString()) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Cans: " + formatTag(tags.get("recycling:cans").toString()) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Clothes: " + formatTag(tags.get("recycling:clothes").toString()) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Glass Bottles: " + formatTag(tags.get("recycling:glass_bottles").toString()) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Plastic Bottles: " + formatTag(tags.get("recycling:plastic_bottles").toString()) + "  ";} catch (JSONException ignored) {}
        try {snippet += "Paper: " + formatTag(tags.get("recycling:paper").toString());} catch (JSONException ignored) {}

        return snippet;
    }
}