package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.io.File;

public class GameSettingsSteamTab extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";
    private Shortcut shortcut;

    public static GameSettingsSteamTab newInstance(int containerId, String shortcutPath) {
        GameSettingsSteamTab f = new GameSettingsSteamTab();
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
        return inflater.inflate(R.layout.game_settings_steam_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (shortcut == null) return;
        CheckBox offline = view.findViewById(R.id.GameSettingsSteamOffline);
        CheckBox cloud = view.findViewById(R.id.GameSettingsSteamCloudSync);
        CheckBox input = view.findViewById(R.id.GameSettingsSteamInput);
        offline.setChecked("1".equals(shortcut.getExtra("steamOffline", "0")));
        cloud.setChecked(!"0".equals(shortcut.getExtra("steamCloudSync", "1")));
        input.setChecked("1".equals(shortcut.getExtra("steamInput", "0")));
        offline.setOnCheckedChangeListener((v, c) -> { shortcut.putExtra("steamOffline", c ? "1" : null); shortcut.saveData(); });
        cloud.setOnCheckedChangeListener((v, c) -> { shortcut.putExtra("steamCloudSync", c ? "1" : "0"); shortcut.saveData(); });
        input.setOnCheckedChangeListener((v, c) -> { shortcut.putExtra("steamInput", c ? "1" : null); shortcut.saveData(); });
    }
}
