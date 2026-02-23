package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
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

import com.winlator.contentdialog.ContentDialog;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.util.ArrayList;
import java.util.List;

/**
 * Game Hub–style home screen: grid of Steam tile + installed game shortcuts.
 * Steam tile opens My Games (Steam library); game cards launch via XServerDisplayActivity.
 */
public class HomeFragment extends Fragment {
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_SPACING_DP = 12;
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
        int spacingPx = (int) (GRID_SPACING_DP * getResources().getDisplayMetrics().density);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), GRID_COLUMNS);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 2 : 1; // Hero: Steam tile spans 2 columns
            }
        });
        grid.setLayoutManager(glm);
        grid.addItemDecoration(new GridSpacingItemDecoration(GRID_COLUMNS, spacingPx));
        grid.setAdapter(new HomeAdapter(shortcuts, this::onSteamTileClick, this::onGameClick));

        view.findViewById(R.id.TabDiscover).setOnClickListener(v -> openSteamStore(true));
        view.findViewById(R.id.TabFindGames).setOnClickListener(v -> openSteamStore(false));
        view.findViewById(R.id.BtnSearch).setOnClickListener(v -> openSteamStore(false));

        view.findViewById(R.id.TabMy).setSelected(true); // Pill highlight for My
        updateAccountStrip(view);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.home);
        }
        loadShortcuts();
        showFirstTimeSetupIfNeeded();
    }

    private static final String PREFS_FIRST_TIME = "gamehub_first_time";
    private static final String KEY_SETUP_SHOWN = "first_time_setup_shown";

    private void showFirstTimeSetupIfNeeded() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_FIRST_TIME, Activity.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SETUP_SHOWN, false)) return;
        ContentDialog dialog = new ContentDialog(requireContext());
        dialog.setTitle(R.string.first_time_setup_title);
        dialog.setMessage(R.string.first_time_setup_message);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.setOnConfirmCallback(() ->
            prefs.edit().putBoolean(KEY_SETUP_SHOWN, true).apply());
        dialog.show();
    }

    private void updateAccountStrip(View root) {
        SteamAuthPrefs prefs = new SteamAuthPrefs(requireContext());
        TextView username = root.findViewById(R.id.AccountStripUsername);
        String name = prefs.getDisplayName();
        username.setText(name != null && !name.isEmpty() ? name : getString(R.string.account_not_signed_in));
        View signOut = root.findViewById(R.id.BtnAccountSignOut);
        signOut.setOnClickListener(v -> {
            prefs.signOut();
            username.setText(getString(R.string.account_not_signed_in));
        });
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

    /** Discover = store front; Find games / Y Search = store search (Game Hub behavior). */
    private void openSteamStore(boolean discover) {
        Intent i = new Intent(requireContext(), SteamStoreActivity.class);
        i.putExtra(SteamStoreActivity.EXTRA_URL,
            discover ? getString(R.string.steam_store_url) : getString(R.string.steam_store_search_url));
        i.putExtra(SteamStoreActivity.EXTRA_TITLE,
            discover ? getString(R.string.steam_discover) : getString(R.string.steam_find_games));
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
                setupFocusAnimation(v);
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
            private final TextView action;

            GameHolder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.HomeGameImage);
                name = itemView.findViewById(R.id.HomeGameName);
                action = itemView.findViewById(R.id.HomeGameAction);
                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos > 0 && pos <= shortcuts.size()) {
                        onGameClick.onGameClick(shortcuts.get(pos - 1));
                    }
                });
                setupFocusAnimation(itemView);
            }

            void bind(Shortcut s) {
                name.setText(s.name);
                action.setText(itemView.getContext().getString(R.string.tile_action_play)); // Ready
                if (s.icon != null) {
                    image.setImageBitmap(s.icon);
                } else {
                    image.setImageDrawable(null);
                }
            }
        }

        /** 10-foot UI: scale 1.0 → 1.06 and elevation when focused. */
        private static void setupFocusAnimation(View v) {
            v.setOnFocusChangeListener((view, hasFocus) -> {
                view.animate().scaleX(hasFocus ? 1.06f : 1f).scaleY(hasFocus ? 1.06f : 1f)
                    .setDuration(150).start();
                if (view instanceof androidx.cardview.widget.CardView) {
                    ((androidx.cardview.widget.CardView) view).setCardElevation(hasFocus ? 12f : 8f);
                }
            });
        }
    }

    /** Even spacing between grid items (Game Hub style). */
    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacingPx;

        GridSpacingItemDecoration(int spanCount, int spacingPx) {
            this.spanCount = spanCount;
            this.spacingPx = spacingPx;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column;
            boolean firstRow = true;
            if (parent.getLayoutManager() instanceof GridLayoutManager) {
                GridLayoutManager gm = (GridLayoutManager) parent.getLayoutManager();
                column = gm.getSpanSizeLookup().getSpanIndex(position, gm.getSpanCount());
                int sum = 0;
                for (int i = 0; i <= position && i < parent.getAdapter().getItemCount(); i++) {
                    sum += gm.getSpanSizeLookup().getSpanSize(i);
                    if (sum > gm.getSpanCount()) {
                        firstRow = false;
                        break;
                    }
                    if (i == position) break;
                }
            } else {
                column = position % spanCount;
                firstRow = position < spanCount;
            }
            outRect.left = column * spacingPx / spanCount;
            outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
            outRect.top = firstRow ? 0 : spacingPx;
            outRect.bottom = spacingPx;
        }
    }
}
