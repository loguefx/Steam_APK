package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.io.File;
import java.util.List;

public class GameSettingsBackupTab extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";
    private Shortcut shortcut;

    public static GameSettingsBackupTab newInstance(int containerId, String shortcutPath) {
        GameSettingsBackupTab f = new GameSettingsBackupTab();
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
        return inflater.inflate(R.layout.game_settings_backup_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.GameSettingsBackupBtn).setOnClickListener(v -> {
            if (shortcut == null) return;
            new Thread(() -> {
                BackupManager.BackupOptions opts = new BackupManager.BackupOptions();
                opts.includeSaves = true;
                opts.includeShaderCache = false;
                opts.includeConfig = false;
                BackupManager bm = new BackupManager(requireContext());
                File zip = bm.createBackup(shortcut.container, shortcut, opts);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (zip != null) {
                            Toast.makeText(requireContext(), getString(R.string.backup_success, zip.getAbsolutePath()), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), R.string.backup_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });
        view.findViewById(R.id.GameSettingsRestoreBtn).setOnClickListener(v -> {
            if (shortcut == null) return;
            RestoreManager rm = new RestoreManager(requireContext());
            List<File> backups = rm.listBackups();
            if (backups.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_backups_found, Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[backups.size()];
            for (int i = 0; i < backups.size(); i++) names[i] = backups.get(i).getName();
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_from_local)
                .setItems(names, (dialog, which) -> {
                    File backupZip = backups.get(which);
                    new Thread(() -> {
                        boolean ok = rm.restore(backupZip, shortcut.container);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), ok ? R.string.restore_success : R.string.restore_failed, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .show();
        });
    }
}
