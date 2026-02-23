package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.gamehubpp.GameProfileState;
import com.winlator.gamehubpp.LaunchCoordinator;
import com.winlator.gamehubpp.PackManager;
import com.winlator.gamehubpp.ProfileStore;

import java.io.File;

public class GameSettingsGeneralTab extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";

    private Container container;
    private Shortcut shortcut;

    public static GameSettingsGeneralTab newInstance(int containerId, String shortcutPath) {
        GameSettingsGeneralTab f = new GameSettingsGeneralTab();
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
        int containerId = args.getInt(ARG_CONTAINER_ID, 0);
        String path = args.getString(ARG_SHORTCUT_PATH, "");
        ContainerManager cm = new ContainerManager(requireContext());
        container = cm.getContainerById(containerId);
        if (container != null && path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.isFile()) shortcut = new Shortcut(container, f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_settings_general_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (shortcut == null) return;

        EditText etName = view.findViewById(R.id.GameSettingsGeneralName);
        EditText etExecArgs = view.findViewById(R.id.GameSettingsGeneralExecArgs);
        Spinner sScreenSize = view.findViewById(R.id.GameSettingsGeneralScreenSize);
        CheckBox pinLkg = view.findViewById(R.id.GameSettingsGeneralPinLkg);

        etName.setText(shortcut.name);
        etExecArgs.setText(shortcut.getExtra("execArgs"));
        String screenSize = shortcut.getExtra("screenSize", shortcut.container.getScreenSize());
        for (int i = 0; i < sScreenSize.getCount(); i++) {
            String item = sScreenSize.getItemAtPosition(i).toString();
            if (item.startsWith(screenSize) || (item.contains("x") && item.split(" ")[0].equals(screenSize))) {
                sScreenSize.setSelection(i);
                break;
            }
        }
        String gameId = "c" + shortcut.container.id + "_" + shortcut.file.getName();
        ProfileStore store = new ProfileStore(requireContext());
        GameProfileState state = store.getOrCreateGameState(gameId, "profile_safe_v1");
        pinLkg.setChecked(state.pinned);

        view.findViewById(R.id.GameSettingsGeneralUpdateCloud).setOnClickListener(v -> {
            String gid = "c" + shortcut.container.id + "_" + shortcut.file.getName();
            new Thread(() -> {
                PackManager pm = new PackManager(requireContext());
                java.util.List<String> ids = pm.listCachedPackIds();
                final boolean applied;
                final int msgId;
                if (ids.isEmpty()) {
                    applied = false;
                    msgId = R.string.no_cloud_pack;
                } else {
                    com.winlator.gamehubpp.ConfigPack pack = pm.loadCachedPack(ids.get(0));
                    if (pack == null || !pm.verifyPackChecksum(ids.get(0))) {
                        msgId = R.string.no_cloud_pack;
                        applied = false;
                    } else {
                        LaunchCoordinator lc = LaunchCoordinator.get(requireContext());
                        applied = pm.applyPackAsCandidateForGame(pack, gid, null, lc.getProfileStore(), lc.getDeviceInfo());
                        msgId = applied ? R.string.config_updated_from_cloud : R.string.no_matching_preset;
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), msgId, Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        pinLkg.setOnCheckedChangeListener((v, checked) -> {
            store.setPinned(gameId, checked);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        saveIfNeeded();
    }

    private void saveIfNeeded() {
        if (shortcut == null || getView() == null) return;
        EditText etName = getView().findViewById(R.id.GameSettingsGeneralName);
        EditText etExecArgs = getView().findViewById(R.id.GameSettingsGeneralExecArgs);
        Spinner sScreenSize = getView().findViewById(R.id.GameSettingsGeneralScreenSize);
        String name = etName.getText().toString().trim();
        if (!name.isEmpty() && !name.equals(shortcut.name)) {
            java.io.File parent = shortcut.file.getParentFile();
            java.io.File newFile = new java.io.File(parent, name + ".desktop");
            if (!newFile.exists()) shortcut.file.renameTo(newFile);
        }
        String execArgs = etExecArgs.getText().toString();
        shortcut.putExtra("execArgs", execArgs.isEmpty() ? null : execArgs);
        String sel = sScreenSize.getSelectedItem().toString();
        String screenSize = sel.contains(" ") ? sel.split(" ")[0] : sel;
        shortcut.putExtra("screenSize", screenSize.equals(shortcut.container.getScreenSize()) ? null : screenSize);
        shortcut.saveData();
    }
}
