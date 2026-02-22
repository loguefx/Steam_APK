package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.gamehubpp.LaunchCoordinator;
import com.winlator.gamehubpp.Profile;
import com.winlator.gamehubpp.ProfileStore;

import java.util.HashMap;
import java.util.Map;

/**
 * Translation params: Presets (Compatible/Stable/Performance). Saves to Safe profile so Resolver uses it.
 */
public class TranslationParamsFragment extends Fragment {
    private static final String[] PRESET_IDS = new String[] { "compatibility", "stable", "performance" };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.translation_params);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.translation_params_fragment, container, false);
        Spinner preset = view.findViewById(R.id.SPreset);
        Button reset = view.findViewById(R.id.BTResetToPreset);
        Button unlock = view.findViewById(R.id.BTUnlockAdvanced);
        LinearLayout advanced = view.findViewById(R.id.LLAdvanced);

        ProfileStore store = LaunchCoordinator.get(requireContext()).getProfileStore();
        Profile safe = store.loadProfile("profile_safe_v1");
        if (safe != null && safe.settings.containsKey("translation_preset")) {
            String p = String.valueOf(safe.settings.get("translation_preset"));
            for (int i = 0; i < PRESET_IDS.length; i++) if (PRESET_IDS[i].equals(p)) { preset.setSelection(i); break; }
        } else preset.setSelection(1);

        preset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savePresetToSafe(store, PRESET_IDS[position]);
            }
        });

        reset.setOnClickListener(v -> {
            preset.setSelection(1);
            savePresetToSafe(store, "stable");
        });
        unlock.setOnClickListener(v -> {
            advanced.setVisibility(advanced.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            unlock.setText(advanced.getVisibility() == View.VISIBLE ? R.string.advanced : R.string.unlock_advanced);
        });
        return view;
    }

    private void savePresetToSafe(ProfileStore store, String presetId) {
        Profile safe = store.loadProfile("profile_safe_v1");
        if (safe == null) safe = ProfileStore.createDefaultSafeProfile();
        Map<String, Object> set = new HashMap<>(safe.settings);
        set.put("translation_preset", presetId);
        Profile updated = new Profile(safe.id, safe.name, safe.source, safe.components, set, safe.constraints);
        store.saveProfile(updated);
    }
}
