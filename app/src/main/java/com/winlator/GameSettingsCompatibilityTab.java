package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.core.AppUtils;
import com.winlator.core.StringUtils;
import com.winlator.box86_64.Box86_64PresetManager;

import java.io.File;

public class GameSettingsCompatibilityTab extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";
    private Container container;
    private Shortcut shortcut;

    public static GameSettingsCompatibilityTab newInstance(int containerId, String shortcutPath) {
        GameSettingsCompatibilityTab f = new GameSettingsCompatibilityTab();
        Bundle args = new Bundle();
        args.putInt(ARG_CONTAINER_ID, containerId);
        args.putString(ARG_SHORTCUT_PATH, shortcutPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadContainerAndShortcut();
    }

    private void loadContainerAndShortcut() {
        Bundle args = getArguments();
        if (args == null) return;
        ContainerManager cm = new ContainerManager(requireContext());
        container = cm.getContainerById(args.getInt(ARG_CONTAINER_ID, 0));
        String path = args.getString(ARG_SHORTCUT_PATH, "");
        if (container != null && path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.isFile()) shortcut = new Shortcut(container, f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_settings_compatibility_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (shortcut == null) return;
        Spinner sGraphics = view.findViewById(R.id.GameSettingsCompatGraphicsDriver);
        Spinner sDxWrapper = view.findViewById(R.id.GameSettingsCompatDxWrapper);
        Spinner sAudio = view.findViewById(R.id.GameSettingsCompatAudioDriver);
        Spinner sBox86 = view.findViewById(R.id.GameSettingsCompatBox86Preset);
        Spinner sBox64 = view.findViewById(R.id.GameSettingsCompatBox64Preset);
        EditText eSurfaceFormat = view.findViewById(R.id.GameSettingsCompatSurfaceFormat);
        EditText eCpuCoreLimit = view.findViewById(R.id.GameSettingsCompatCpuCoreLimit);
        EditText eVramLimit = view.findViewById(R.id.GameSettingsCompatVramLimit);
        AppUtils.setSpinnerSelectionFromIdentifier(sGraphics, shortcut.getExtra("graphicsDriver", shortcut.container.getGraphicsDriver()));
        AppUtils.setSpinnerSelectionFromIdentifier(sDxWrapper, shortcut.getExtra("dxwrapper", shortcut.container.getDXWrapper()));
        AppUtils.setSpinnerSelectionFromIdentifier(sAudio, shortcut.getExtra("audioDriver", shortcut.container.getAudioDriver()));
        Box86_64PresetManager.loadSpinner("box86", sBox86, shortcut.getExtra("box86Preset", shortcut.container.getBox86Preset()));
        Box86_64PresetManager.loadSpinner("box64", sBox64, shortcut.getExtra("box64Preset", shortcut.container.getBox64Preset()));
        eSurfaceFormat.setText(shortcut.getExtra("surface_format", ""));
        eCpuCoreLimit.setText(shortcut.getExtra("cpu_core_limit", ""));
        eVramLimit.setText(shortcut.getExtra("vram_limit", ""));
    }

    @Override
    public void onPause() {
        super.onPause();
        saveIfNeeded();
    }

    private void saveIfNeeded() {
        if (shortcut == null || getView() == null) return;
        Spinner sGraphics = getView().findViewById(R.id.GameSettingsCompatGraphicsDriver);
        Spinner sDxWrapper = getView().findViewById(R.id.GameSettingsCompatDxWrapper);
        Spinner sAudio = getView().findViewById(R.id.GameSettingsCompatAudioDriver);
        Spinner sBox86 = getView().findViewById(R.id.GameSettingsCompatBox86Preset);
        Spinner sBox64 = getView().findViewById(R.id.GameSettingsCompatBox64Preset);
        EditText eSurfaceFormat = getView().findViewById(R.id.GameSettingsCompatSurfaceFormat);
        EditText eCpuCoreLimit = getView().findViewById(R.id.GameSettingsCompatCpuCoreLimit);
        EditText eVramLimit = getView().findViewById(R.id.GameSettingsCompatVramLimit);
        String g = StringUtils.parseIdentifier(sGraphics.getSelectedItem());
        String d = StringUtils.parseIdentifier(sDxWrapper.getSelectedItem());
        String a = StringUtils.parseIdentifier(sAudio.getSelectedItem());
        String b86 = Box86_64PresetManager.getSpinnerSelectedId(sBox86);
        String b64 = Box86_64PresetManager.getSpinnerSelectedId(sBox64);
        String surfaceFormat = eSurfaceFormat.getText() != null ? eSurfaceFormat.getText().toString().trim() : "";
        String cpuCoreLimit = eCpuCoreLimit.getText() != null ? eCpuCoreLimit.getText().toString().trim() : "";
        String vramLimit = eVramLimit.getText() != null ? eVramLimit.getText().toString().trim() : "";
        shortcut.putExtra("graphicsDriver", g.equals(shortcut.container.getGraphicsDriver()) ? null : g);
        shortcut.putExtra("dxwrapper", d.equals(shortcut.container.getDXWrapper()) ? null : d);
        shortcut.putExtra("audioDriver", a.equals(shortcut.container.getAudioDriver()) ? null : a);
        shortcut.putExtra("box86Preset", b86.equals(shortcut.container.getBox86Preset()) ? null : b86);
        shortcut.putExtra("box64Preset", b64.equals(shortcut.container.getBox64Preset()) ? null : b64);
        shortcut.putExtra("surface_format", surfaceFormat.isEmpty() ? null : surfaceFormat);
        shortcut.putExtra("cpu_core_limit", cpuCoreLimit.isEmpty() ? null : cpuCoreLimit);
        shortcut.putExtra("vram_limit", vramLimit.isEmpty() ? null : vramLimit);
        shortcut.saveData();
    }
}
