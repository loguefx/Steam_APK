package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.steam.InstalledSteamGame;
import com.winlator.steam.InstalledSteamGamesRepository;
import com.winlator.steam.SteamRuntimeState;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Game Hub–style home: Steam tile + installed games only (from Steam library scan).
 * Steam tile = install gateway (provision container/Steam, then launch Steam client).
 */
public class HomeFragment extends Fragment {
    private static final int GRID_COLUMNS = 3;
    private static final int GRID_SPACING_DP = 12;
    private static final int VIEW_TYPE_STEAM_TILE = 0;
    private static final int VIEW_TYPE_GAME = 1;

    private RecyclerView grid;
    private View homeEmptyState;
    private ContainerManager containerManager;
    private Container container;
    private InstalledSteamGamesRepository repository;
    private SteamRuntimeState steamState = SteamRuntimeState.NO_CONTAINER;
    private final List<InstalledSteamGame> installedGames = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        homeEmptyState = view.findViewById(R.id.HomeEmptyState);
        int spacingPx = (int) (GRID_SPACING_DP * getResources().getDisplayMetrics().density);
        GridLayoutManager glm = new GridLayoutManager(requireContext(), GRID_COLUMNS);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? 2 : 1;
            }
        });
        grid.setLayoutManager(glm);
        grid.addItemDecoration(new GridSpacingItemDecoration(GRID_COLUMNS, spacingPx));
        grid.setAdapter(new HomeAdapter(installedGames, steamState, this::onSteamTileClick, this::onGameClick));

        view.findViewById(R.id.TabDiscover).setOnClickListener(v -> openSteamStore(true));
        view.findViewById(R.id.TabFindGames).setOnClickListener(v -> openSteamStore(false));
        view.findViewById(R.id.BtnSearch).setOnClickListener(v -> openSteamStore(false));
        view.findViewById(R.id.TabMy).setSelected(true);
        updateAccountStrip(view);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.home);
        }
        loadInstalledGames();
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
        if (name != null && !name.isEmpty()) {
            username.setText(name);
        } else if (prefs.isSignedIn() || (prefs.getSteamId() != null && !prefs.getSteamId().isEmpty())) {
            username.setText(getString(R.string.account_signed_in));
        } else {
            username.setText(getString(R.string.account_not_signed_in));
        }
        View signOut = root.findViewById(R.id.BtnAccountSignOut);
        signOut.setOnClickListener(v -> {
            prefs.signOut();
            username.setText(getString(R.string.account_not_signed_in));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInstalledGames();
    }

    private void loadInstalledGames() {
        executor.execute(() -> {
            Container c = containerManager.getDefaultContainer();
            InstalledSteamGamesRepository repo = c != null ? new InstalledSteamGamesRepository(c) : null;
            InstalledSteamGamesRepository.Result result = repo != null ? repo.getInstalledGames() : new InstalledSteamGamesRepository.Result(SteamRuntimeState.NO_CONTAINER, new ArrayList<>());
            if (c == null) {
                result = new InstalledSteamGamesRepository.Result(SteamRuntimeState.NO_CONTAINER, new ArrayList<>());
            }
            final SteamRuntimeState state = result.state;
            final List<InstalledSteamGame> games = result.games;
            final Container cont = c;
            requireActivity().runOnUiThread(() -> {
                container = cont;
                repository = repo;
                steamState = state;
                installedGames.clear();
                installedGames.addAll(games);
                if (grid.getAdapter() != null) {
                    ((HomeAdapter) grid.getAdapter()).setState(state);
                    grid.getAdapter().notifyDataSetChanged();
                }
                homeEmptyState.setVisibility(games.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void onSteamTileClick() {
        if (steamState == SteamRuntimeState.NO_CONTAINER) {
            containerManager.getOrCreateDefaultContainerAsync(c -> {
                if (c == null) {
                    showPreparingFailedDialog();
                    return;
                }
                executor.execute(() -> {
                    InstalledSteamGamesRepository.Result r = new InstalledSteamGamesRepository(c).getInstalledGames();
                    requireActivity().runOnUiThread(() -> {
                        container = c;
                        repository = new InstalledSteamGamesRepository(c);
                        steamState = r.state;
                        installedGames.clear();
                        installedGames.addAll(r.games);
                        if (grid.getAdapter() != null) {
                            ((HomeAdapter) grid.getAdapter()).setState(steamState);
                            grid.getAdapter().notifyDataSetChanged();
                        }
                        homeEmptyState.setVisibility(installedGames.isEmpty() ? View.VISIBLE : View.GONE);
                        if (steamState == SteamRuntimeState.STEAM_READY) {
                            SteamLibraryFragment.launchSteamClient(HomeFragment.this, c);
                        } else if (steamState == SteamRuntimeState.STEAM_NOT_INSTALLED) {
                            showInstallSteamDialog(c);
                        }
                    });
                });
            });
            return;
        }
        if (steamState == SteamRuntimeState.STEAM_NOT_INSTALLED) {
            showInstallSteamDialog(container);
            return;
        }
        if (steamState == SteamRuntimeState.STEAM_READY && container != null) {
            SteamLibraryFragment.launchSteamClient(this, container);
        }
    }

    private void showInstallSteamDialog(Container c) {
        ContentDialog dialog = new ContentDialog(requireContext());
        dialog.setTitle(R.string.tile_action_install_steam);
        dialog.setMessage(R.string.install_steam_in_container);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.show();
    }

    private void showPreparingFailedDialog() {
        ContentDialog dialog = new ContentDialog(requireContext());
        dialog.setMessage(R.string.preparing_environment_failed);
        dialog.getContentView().findViewById(R.id.BTConfirm).setContentDescription(getString(R.string.retry));
        ((android.widget.Button) dialog.getContentView().findViewById(R.id.BTConfirm)).setText(R.string.retry);
        dialog.findViewById(R.id.BTCancel).setVisibility(View.GONE);
        dialog.setOnConfirmCallback(this::onSteamTileClick);
        dialog.show();
    }

    private void onGameClick(InstalledSteamGame game) {
        if (container == null) return;
        if (XrActivity.isSupported()) {
            java.io.File desktopFile = new java.io.File(container.getDesktopDir(), "Steam " + game.appId + ".desktop");
            if (!container.getDesktopDir().isDirectory()) container.getDesktopDir().mkdirs();
            String steamWinPath = "C:\\Program Files (x86)\\Steam\\steam.exe";
            String content = "[Desktop Entry]\nType=Application\nName=" + game.name + "\nExec=wine \"" + steamWinPath + "\"\n\n[Extra Data]\nexecArgs=-applaunch " + game.appId + "\n";
            com.winlator.core.FileUtils.writeString(desktopFile, content);
            XrActivity.openIntent(requireActivity(), container.id, desktopFile.getPath());
        } else {
            SteamLibraryFragment.launchSteamGameByAppId(this, container, game.appId, game.name);
        }
    }

    private void openSteamStore(boolean discover) {
        Intent i = new Intent(requireContext(), SteamStoreActivity.class);
        i.putExtra(SteamStoreActivity.EXTRA_URL,
            discover ? getString(R.string.steam_store_url) : getString(R.string.steam_store_search_url));
        i.putExtra(SteamStoreActivity.EXTRA_TITLE,
            discover ? getString(R.string.steam_discover) : getString(R.string.steam_find_games));
        startActivity(i);
    }

    private static class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<InstalledSteamGame> installedGames;
        private SteamRuntimeState steamState;
        private final Runnable onSteamClick;
        private final OnGameClickListener onGameClick;

        interface OnGameClickListener {
            void onGameClick(InstalledSteamGame game);
        }

        HomeAdapter(List<InstalledSteamGame> installedGames, SteamRuntimeState steamState, Runnable onSteamClick, OnGameClickListener onGameClick) {
            this.installedGames = installedGames;
            this.steamState = steamState;
            this.onSteamClick = onSteamClick;
            this.onGameClick = onGameClick;
        }

        void setState(SteamRuntimeState state) {
            this.steamState = state;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_STEAM_TILE : VIEW_TYPE_GAME;
        }

        @Override
        public int getItemCount() {
            return 1 + installedGames.size();
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
            if (position == 0) {
                TextView hint = holder.itemView.findViewById(R.id.SteamTileHint);
                if (hint != null) {
                    int resId;
                    if (steamState == SteamRuntimeState.STEAM_READY) resId = R.string.tile_action_open_steam;
                    else if (steamState == SteamRuntimeState.STEAM_NOT_INSTALLED) resId = R.string.tile_action_install_steam;
                    else resId = R.string.tile_action_create_container;
                    hint.setText(resId);
                }
                return;
            }
            InstalledSteamGame game = installedGames.get(position - 1);
            ((GameHolder) holder).bind(game);
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
                    if (pos != RecyclerView.NO_POSITION && pos > 0 && pos <= installedGames.size()) {
                        onGameClick.onGameClick(installedGames.get(pos - 1));
                    }
                });
                setupFocusAnimation(itemView);
            }

            void bind(InstalledSteamGame game) {
                name.setText(game.name);
                action.setText(itemView.getContext().getString(R.string.tile_action_play));
                String url = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + game.appId + "/header.jpg";
                loadImage(image, url);
            }
        }

        private static void loadImage(ImageView iv, String url) {
            iv.setTag(url);
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    InputStream in = c.getInputStream();
                    Bitmap b = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                    if (b != null && url.equals(iv.getTag())) {
                        iv.post(() -> { if (url.equals(iv.getTag())) iv.setImageBitmap(b); });
                    }
                } catch (Throwable t) { /* ignore */ }
            }).start();
        }

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
