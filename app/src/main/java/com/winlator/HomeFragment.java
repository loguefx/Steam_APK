package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;
import com.winlator.gamehubpp.LaunchCoordinator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Game Hub-style home: Steam tile (opens Steam library) + grid of installed games (shortcuts).
 */
public class HomeFragment extends Fragment {
    private static final int TYPE_STEAM_TILE = 0;
    private static final int TYPE_SECTION_HEADER = 1;
    private static final int TYPE_SHORTCUT = 2;

    private RecyclerView recycler;
    private View emptyView;
    private ContainerManager containerManager;
    private final List<Shortcut> shortcuts = new ArrayList<>();
    private Container pendingImportContainer;
    private PreloaderDialog preloaderDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_fragment, container, false);
        recycler = root.findViewById(R.id.HomeRecycler);
        emptyView = root.findViewById(R.id.HomeEmpty);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.home);
        preloaderDialog = new PreloaderDialog(requireActivity());
        containerManager = new ContainerManager(requireContext());
        loadShortcuts();
        GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position <= 1) ? 2 : 1;
            }
        });
        recycler.setLayoutManager(glm);
        recycler.setAdapter(new HomeAdapter(shortcuts, this::onOpenSteam, this::onShortcutClick, this::onShortcutMenu, this::onAddGameClick));
        updateEmptyState();
    }

    private void loadShortcuts() {
        shortcuts.clear();
        shortcuts.addAll(containerManager.loadShortcuts());
    }

    private void updateEmptyState() {
        recycler.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void onOpenSteam() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) activity.show(new SteamLibraryFragment());
    }

    /** Game Hub–style: add game without creating a container first. Uses or creates default container. */
    private void onAddGameClick() {
        if (!com.winlator.xenvironment.ImageFs.find(requireContext()).isValid()) {
            Toast.makeText(requireContext(), R.string.unable_to_install_system_files, Toast.LENGTH_SHORT).show();
            return;
        }
        preloaderDialog.show(R.string.import_game_creating_container);
        containerManager.getOrCreateDefaultContainerAsync(container -> {
            preloaderDialog.close();
            if (container == null) {
                Toast.makeText(requireContext(), R.string.unable_to_install_system_files, Toast.LENGTH_SHORT).show();
                return;
            }
            pendingImportContainer = container;
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;
            activity.setOpenFileCallback(uri -> {
                if (uri == null || pendingImportContainer == null) return;
                onImportFileSelected(pendingImportContainer, uri);
                pendingImportContainer = null;
                activity.setOpenFileCallback(null);
            });
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityFromFragment(this, intent, MainActivity.OPEN_FILE_REQUEST_CODE);
        });
    }

    private void onImportFileSelected(Container container, Uri uri) {
        String androidPath = FileUtils.getFilePathFromUri(uri);
        if (androidPath == null || androidPath.isEmpty()) {
            Toast.makeText(requireContext(), R.string.import_game_no_path, Toast.LENGTH_LONG).show();
            return;
        }
        String drives = container.getDrives();
        String winPath = Container.androidPathToWindowsPath(drives, androidPath);
        if (winPath == null) {
            Toast.makeText(requireContext(), R.string.import_game_no_path, Toast.LENGTH_LONG).show();
            return;
        }
        String name = FileUtils.getBasename(androidPath);
        if (name.isEmpty()) name = getString(R.string.untitled);
        File desktopDir = container.getDesktopDir();
        if (!desktopDir.isDirectory()) desktopDir.mkdirs();
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        File desktopFile = new File(desktopDir, safeName + ".desktop");
        int n = 0;
        while (desktopFile.exists()) {
            desktopFile = new File(desktopDir, safeName + "_" + (++n) + ".desktop");
        }
        String content = "[Desktop Entry]\nType=Application\nName=" + name + "\nExec=wine \"" + winPath + "\"\n\n[Extra Data]\n";
        FileUtils.writeString(desktopFile, content);
        Toast.makeText(requireContext(), R.string.game_added, Toast.LENGTH_SHORT).show();
        loadShortcuts();
        if (recycler.getAdapter() != null) recycler.getAdapter().notifyDataSetChanged();
        updateEmptyState();
    }

    private void onShortcutClick(Shortcut shortcut) {
        Activity activity = getActivity();
        if (activity == null) return;
        if (!com.winlator.XrActivity.isSupported()) {
            Intent intent = new Intent(activity, com.winlator.XServerDisplayActivity.class);
            intent.putExtra("container_id", shortcut.container.id);
            intent.putExtra("shortcut_path", shortcut.file.getPath());
            activity.startActivity(intent);
        } else {
            com.winlator.XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
        }
    }

    private void onShortcutMenu(View anchor, Shortcut shortcut) {
        Context context = getContext();
        if (context == null) return;
        PopupMenu menu = new PopupMenu(context, anchor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) menu.setForceShowIcon(true);
        menu.inflate(R.menu.shortcut_popup_menu);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.shortcut_settings) {
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.FLFragmentContainer, GameSettingsFragment.newInstance(shortcut.container.id, shortcut.file.getPath()))
                    .addToBackStack(null)
                    .commit();
                return true;
            }
            if (id == R.id.shortcut_pin_lkg) {
                String gameId = "c" + shortcut.container.id + "_" + shortcut.file.getName();
                com.winlator.gamehubpp.ProfileStore store = LaunchCoordinator.get(context).getProfileStore();
                boolean newPinned = !store.getOrCreateGameState(gameId, "profile_safe_v1").pinned;
                store.setPinned(gameId, newPinned);
                Toast.makeText(context, newPinned ? R.string.pin_lkg_for_offline : R.string.unpin_lkg, Toast.LENGTH_SHORT).show();
                loadShortcuts();
                recycler.getAdapter().notifyDataSetChanged();
                return true;
            }
            if (id == R.id.shortcut_remove) {
                ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_shortcut, () -> {
                    if (shortcut.file.delete() && shortcut.iconFile != null) shortcut.iconFile.delete();
                    loadShortcuts();
                    recycler.getAdapter().notifyDataSetChanged();
                    updateEmptyState();
                });
                return true;
            }
            return false;
        });
        menu.show();
    }

    private static class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Shortcut> shortcuts;
        private final Runnable onSteamClick;
        private final OnShortcutAction onShortcutClick;
        private final OnShortcutMenu onShortcutMenu;
        private final Runnable onAddGameClick;

        interface OnShortcutMenu { void show(View anchor, Shortcut s); }
        interface OnShortcutAction { void onClick(Shortcut s); }

        HomeAdapter(List<Shortcut> shortcuts, Runnable onSteamClick,
                    OnShortcutAction onShortcutClick, OnShortcutMenu onShortcutMenu, Runnable onAddGameClick) {
            this.shortcuts = shortcuts;
            this.onSteamClick = onSteamClick;
            this.onShortcutClick = onShortcutClick;
            this.onShortcutMenu = onShortcutMenu;
            this.onAddGameClick = onAddGameClick;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_STEAM_TILE;
            if (position == 1) return TYPE_SECTION_HEADER;
            return TYPE_SHORTCUT;
        }

        @Override
        public int getItemCount() {
            return 2 + shortcuts.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_STEAM_TILE) {
                return new SteamHolder(inf.inflate(R.layout.home_steam_tile, parent, false));
            }
            if (viewType == TYPE_SECTION_HEADER) {
                return new HeaderHolder(inf.inflate(R.layout.home_section_header, parent, false));
            }
            return new ShortcutHolder(inf.inflate(R.layout.home_game_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof SteamHolder) {
                ((SteamHolder) holder).root.setOnClickListener(v -> onSteamClick.run());
                return;
            }
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).addGameButton.setOnClickListener(v -> { if (onAddGameClick != null) onAddGameClick.run(); });
                return;
            }
            Shortcut s = shortcuts.get(position - 2);
            ShortcutHolder h = (ShortcutHolder) holder;
            if (s.icon != null) h.imageView.setImageBitmap(s.icon);
            else h.imageView.setImageResource(android.R.drawable.ic_menu_my_calendar);
            h.title.setText(s.name);
            h.subtitle.setText(s.container.getName());
            h.innerArea.setOnClickListener(v -> onShortcutClick.onClick(s));
            h.menuButton.setOnClickListener(v -> onShortcutMenu.show(v, s));
        }

        static class SteamHolder extends RecyclerView.ViewHolder {
            final View root;
            SteamHolder(View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.HomeSteamTileRoot);
            }
        }

        static class HeaderHolder extends RecyclerView.ViewHolder {
            final android.widget.Button addGameButton;
            HeaderHolder(View itemView) {
                super(itemView);
                addGameButton = itemView.findViewById(R.id.BtnAddGame);
            }
        }

        static class ShortcutHolder extends RecyclerView.ViewHolder {
            final View innerArea;
            final ImageView imageView;
            final TextView title, subtitle;
            final ImageButton menuButton;
            ShortcutHolder(View itemView) {
                super(itemView);
                innerArea = itemView.findViewById(R.id.LLInnerArea);
                imageView = itemView.findViewById(R.id.ImageView);
                title = itemView.findViewById(R.id.TVTitle);
                subtitle = itemView.findViewById(R.id.TVSubtitle);
                menuButton = itemView.findViewById(R.id.BTMenu);
            }
        }
    }
}
