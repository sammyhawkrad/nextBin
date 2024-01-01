package com.sammyhawkrad.nextbin;

import static com.sammyhawkrad.nextbin.MainActivity.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.Locale;


public class PreferencesFragment extends Fragment {

    static  int RADIUS = 300;
    static boolean WASTE_BASKET;
    static boolean RECYCLING_BIN;
    static boolean VENDING_MACHINE;

    Button btn_SavePreferences;

    public PreferencesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preferences, container, false);
        //hide search button in preferences view
        Button btn_Search = ((MainActivity) requireActivity()).btn_Search;
        btn_Search.setVisibility(View.GONE);

        // Handle SeekBar changes
        SeekBar seekBar = view.findViewById(R.id.seekBar);
        TextView tv_seekBarValue = view.findViewById(R.id.tv_SeekBarValue);

        // Set the initial value
        seekBar.setProgress(RADIUS);
        tv_seekBarValue.setText(String.format(Locale.US, getResources().getString(R.string.search_radius_0m), RADIUS));

        // Set up a listener for SeekBar changes
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update the TextView with the selected value
                tv_seekBarValue.setText(String.format(Locale.US, getResources().getString(R.string.search_radius_0m), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Handle touch event start if needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Handle touch event stop if needed
                RADIUS = seekBar.getProgress();
            }
        });

        // Handle checkbox changes
        CheckBox cb_WasteBasket = view.findViewById(R.id.cb_WasteBasket);
        CheckBox cb_RecyclingBin = view.findViewById(R.id.cb_RecyclingBin);
        CheckBox cb_VendingMachine = view.findViewById(R.id.cb_VendingMachine);

        // Set the initial values
        cb_WasteBasket.setChecked(WASTE_BASKET);
        cb_RecyclingBin.setChecked(RECYCLING_BIN);
        cb_VendingMachine.setChecked(VENDING_MACHINE);

        // Set up a listener for checkbox changes
        cb_WasteBasket.setOnCheckedChangeListener((buttonView, isChecked) -> WASTE_BASKET = isChecked);
        cb_RecyclingBin.setOnCheckedChangeListener((buttonView, isChecked) -> RECYCLING_BIN = isChecked);
        cb_VendingMachine.setOnCheckedChangeListener((buttonView, isChecked) -> VENDING_MACHINE = isChecked);

        // Handle save preferences button
        btn_SavePreferences = view.findViewById(R.id.btn_SavePreferences);
        btn_SavePreferences.setOnClickListener(this::savePreferences);

        return view;
    }

    public void savePreferences(View view) {
        // Save preferences to database
        database.execSQL("UPDATE preferences SET value = " + RADIUS + " WHERE preference = 'radius';");
        database.execSQL("UPDATE preferences SET value = " + (WASTE_BASKET ? 1 : 0) + " WHERE preference = 'waste_basket';");
        database.execSQL("UPDATE preferences SET value = " + (RECYCLING_BIN ? 1 : 0) + " WHERE preference = 'recycling_bin';");
        database.execSQL("UPDATE preferences SET value = " + (VENDING_MACHINE ? 1 : 0) + " WHERE preference = 'vending_machine';");

        Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show();

    }
}