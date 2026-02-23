package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.io.File;

/**
 * Game Hub-style Game Settings: single screen with five tabs (General, Compatibility, Steam, Backup/Restore, Touch Controls).
 * Launched with container id and shortcut path; loads Container and Shortcut for persistence.
 */
public class GameSettingsFragment extends Fragment {
    private static final String ARG_CONTAINER_ID = "container_id";
    private static final String ARG_SHORTCUT_PATH = "shortcut_path";

    private int containerId;
    private String shortcutPath;
    private Container container;
    private Shortcut shortcut;

    public static GameSettingsFragment newInstance(int containerId, String shortcutPath) {
        GameSettingsFragment f = new GameSettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONTAINER_ID, containerId);
        args.putString(ARG_SHORTCUT_PATH, shortcutPath != null ? shortcutPath : "");
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            containerId = args.getInt(ARG_CONTAINER_ID, 0);
            shortcutPath = args.getString(ARG_SHORTCUT_PATH, "");
        }
        ContainerManager cm = new ContainerManager(requireContext());
        container = cm.getContainerById(containerId);
        if (container != null && shortcutPath != null && !shortcutPath.isEmpty()) {
            File f = new File(shortcutPath);
            if (f.isFile()) shortcut = new Shortcut(container, f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.game_settings_title);
        if (shortcut != null) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setSubtitle(shortcut.name);
        }

        ViewPager2 pager = view.findViewById(R.id.GameSettingsPager);
        TabLayout tabLayout = view.findViewById(R.id.GameSettingsTabLayout);
        pager.setAdapter(new GameSettingsPagerAdapter(this, containerId, shortcutPath, container, shortcut));
        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText(R.string.general); break;
                case 1: tab.setText(R.string.compatibility); break;
                case 2: tab.setText(R.string.steam_tile_title); break;
                case 3: tab.setText(R.string.backup_restore_tab); break;
                case 4: tab.setText(R.string.touch_controls_tab); break;
                default: tab.setText("—");
            }
        }).attach();
    }

    public Container getContainer() { return container; }
    public Shortcut getShortcut() { return shortcut; }

    private static class GameSettingsPagerAdapter extends FragmentStateAdapter {
        private final int containerId;
        private final String shortcutPath;
        private final Container container;
        private final Shortcut shortcut;

        GameSettingsPagerAdapter(Fragment fa, int containerId, String shortcutPath, Container container, Shortcut shortcut) {
            super(fa);
            this.containerId = containerId;
            this.shortcutPath = shortcutPath;
            this.container = container;
            this.shortcut = shortcut;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return GameSettingsGeneralTab.newInstance(containerId, shortcutPath);
                case 1: return GameSettingsCompatibilityTab.newInstance(containerId, shortcutPath);
                case 2: return GameSettingsSteamTab.newInstance(containerId, shortcutPath);
                case 3: return GameSettingsBackupTab.newInstance(containerId, shortcutPath);
                case 4: return GameSettingsTouchTab.newInstance(containerId, shortcutPath);
                default: return new Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
