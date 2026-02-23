package com.winlator;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.InputControlsManager;

import java.io.File;
import java.util.ArrayList;

public class GameSettingsTouchTab extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";
    /** Profile ID for "Default controller layout" (Virtual Gamepad). */
    private static final int DEFAULT_CONTROLLER_PROFILE_ID = 3;
    /** Profile ID for "Default keyboard and mouse layout" (RTS / KBM). */
    private static final int DEFAULT_KBM_PROFILE_ID = 1;

    private Shortcut shortcut;

    public static GameSettingsTouchTab newInstance(int containerId, String shortcutPath) {
        GameSettingsTouchTab f = new GameSettingsTouchTab();
        Bundle args = new Bundle();
        args.putInt(ARG_CONTAINER_ID, containerId);
        args.putString(ARG_SHORTCUT_PATH, shortcutPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;
        ContainerManager cm = new ContainerManager(requireContext());
        Container container = cm.getContainerById(args.getInt(ARG_CONTAINER_ID, 0));
        String path = args.getString(ARG_SHORTCUT_PATH, "");
        if (container != null && path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.isFile()) shortcut = new Shortcut(container, f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_settings_touch_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (shortcut == null) return;
        Spinner spinner = view.findViewById(R.id.GameSettingsTouchLayout);
        InputControlsManager mgr = new InputControlsManager(requireContext());
        ArrayList<ControlsProfile> profiles = mgr.getProfiles(true);
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Integer> profileIds = new ArrayList<>();
        labels.add(getString(R.string.default_controller_layout));
        profileIds.add(DEFAULT_CONTROLLER_PROFILE_ID);
        labels.add(getString(R.string.default_kbm_layout));
        profileIds.add(DEFAULT_KBM_PROFILE_ID);
        for (ControlsProfile p : profiles) {
            if (p.id != DEFAULT_CONTROLLER_PROFILE_ID && p.id != DEFAULT_KBM_PROFILE_ID) {
                labels.add(p.getName());
                profileIds.add(p.id);
            }
        }
        spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
        int savedId = Integer.parseInt(shortcut.getExtra("controlsProfile", "0"));
        if (savedId == 0) savedId = DEFAULT_CONTROLLER_PROFILE_ID;
        int pos = 0;
        for (int i = 0; i < profileIds.size(); i++) {
            if (profileIds.get(i) == savedId) { pos = i; break; }
        }
        spinner.setSelection(pos);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                int pid = profileIds.get(position);
                shortcut.putExtra("controlsProfile", String.valueOf(pid));
                shortcut.saveData();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        view.findViewById(R.id.GameSettingsTouchEditLayout).setOnClickListener(v -> {
            int sel = spinner.getSelectedItemPosition();
            if (sel >= 0 && sel < profileIds.size()) {
                Intent intent = new Intent(requireContext(), ControlsEditorActivity.class);
                intent.putExtra("profile_id", profileIds.get(sel));
                startActivity(intent);
            }
        });
    }
}
