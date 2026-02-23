package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Game Hub–style home screen: grid of Steam tile + installed game shortcuts.
 * Steam tile opens My Games (Steam library); game cards launch via XServerDisplayActivity.
 */
public class HomeFragment extends Fragment {
    private static final int GRID_COLUMNS = 2;
    private static final int VIEW_TYPE_STEAM_TILE = 0;
    private static final int VIEW_TYPE_GAME = 1;

    private RecyclerView grid;
    private ContainerManager containerManager;
    private ArrayList<Shortcut> shortcuts = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        containerManager = new ContainerManager(requireContext());
        grid = view.findViewById(R.id.HomeGrid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        grid.setAdapter(new HomeAdapter(shortcuts, this::onSteamTileClick, this::onGameClick));

        view.findViewById(R.id.TabDiscover).setOnClickListener(v -> openSteamStore());
        view.findViewById(R.id.TabFindGames).setOnClickListener(v -> openSteamStore());
        view.findViewById(R.id.BtnSearch).setOnClickListener(v -> openSteamStore());

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.home);
        }
        loadShortcuts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShortcuts();
    }

    private void loadShortcuts() {
        shortcuts.clear();
        shortcuts.addAll(containerManager.loadShortcuts());
        if (grid.getAdapter() != null) {
            grid.getAdapter().notifyDataSetChanged();
        }
    }

    private void onSteamTileClick() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showFragment(new SteamLibraryFragment());
        }
    }

    private void onGameClick(Shortcut shortcut) {
        Activity activity = getActivity();
        if (activity == null) return;
        if (!XrActivity.isSupported()) {
            Intent intent = new Intent(activity, XServerDisplayActivity.class);
            intent.putExtra("container_id", shortcut.container.id);
            intent.putExtra("shortcut_path", shortcut.file.getPath());
            activity.startActivity(intent);
        } else {
            XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
        }
    }

    private void openSteamStore() {
        Intent i = new Intent(requireContext(), SteamStoreActivity.class);
        i.putExtra(SteamStoreActivity.EXTRA_URL, getString(R.string.steam_store_url));
        i.putExtra(SteamStoreActivity.EXTRA_TITLE, getString(R.string.steam_find_games));
        startActivity(i);
    }

    private static class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Shortcut> shortcuts;
        private final Runnable onSteamClick;
        private final OnGameClickListener onGameClick;

        interface OnGameClickListener {
            void onGameClick(Shortcut shortcut);
        }

        HomeAdapter(List<Shortcut> shortcuts, Runnable onSteamClick, OnGameClickListener onGameClick) {
            this.shortcuts = shortcuts;
            this.onSteamClick = onSteamClick;
            this.onGameClick = onGameClick;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_STEAM_TILE : VIEW_TYPE_GAME;
        }

        @Override
        public int getItemCount() {
            return 1 + shortcuts.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_STEAM_TILE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_steam_tile, parent, false);
                v.setOnClickListener(v2 -> onSteamClick.run());
                return new RecyclerView.ViewHolder(v) {};
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_game_card, parent, false);
                return new GameHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof GameHolder) {
                Shortcut s = shortcuts.get(position - 1);
                ((GameHolder) holder).bind(s);
            }
        }

        private class GameHolder extends RecyclerView.ViewHolder {
            private final ImageView image;
            private final TextView name;

            GameHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.HomeGameImage);
                name = itemView.findViewById(R.id.HomeGameName);
                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos > 0 && pos <= shortcuts.size()) {
                        onGameClick.onGameClick(shortcuts.get(pos - 1));
                    }
                });
            }

            void bind(Shortcut s) {
                name.setText(s.name);
                if (s.icon != null) {
                    image.setImageBitmap(s.icon);
                } else {
                    image.setImageDrawable(null);
                }
            }
        }
    }
}
