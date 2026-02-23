package com.winlator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.FileUtils;
import com.winlator.core.SteamWebApi;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Main screen: Steam library grid. Fetches owned games via Steam Web API.
 * Play = launch container with Steam -applaunch APPID (creates temp .desktop and runs it).
 */
public class SteamLibraryFragment extends Fragment {
    private static final int GRID_COLUMNS = 2;

    /** Used by SteamGameAdapter and SteamRecentAdapter for launch callback. */
    interface OnPlayListener { void onPlay(SteamWebApi.Game game); }

    private static final int RECENTLY_PLAYED_MAX = 10;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView grid;
    private RecyclerView recentRecycler;
    private TextView emptyText;
    private TextView labelRecentlyPlayed;
    private TextView gameCountText;
    private List<SteamWebApi.Game> games = new ArrayList<>();
    private List<SteamWebApi.Game> recentlyPlayed = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.steam_library_fragment, container, false);
        refreshLayout = root.findViewById(R.id.SteamLibraryRefresh);
        grid = root.findViewById(R.id.SteamLibraryGrid);
        recentRecycler = root.findViewById(R.id.SteamRecentlyPlayed);
        emptyText = root.findViewById(R.id.SteamLibraryEmpty);
        labelRecentlyPlayed = root.findViewById(R.id.LabelRecentlyPlayed);
        gameCountText = root.findViewById(R.id.SteamGameCount);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), GRID_COLUMNS));
        recentRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        refreshLayout.setOnRefreshListener(this::loadLibrary);

        root.findViewById(R.id.TabStore).setOnClickListener(v -> openSteamStore());
        root.findViewById(R.id.BtnSearch).setOnClickListener(v -> openSteamStore());
        root.findViewById(R.id.TabDiscover).setOnClickListener(v -> openSteamStore());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.steam_library);
        loadLibrary();
    }

    private void loadLibrary() {
        refreshLayout.setRefreshing(true);
        executor.execute(() -> {
            SteamAuthPrefs prefs = new SteamAuthPrefs(requireContext());
            String apiKey = prefs.getWebApiKey();
            if (apiKey == null || apiKey.trim().isEmpty())
                apiKey = BuildConfig.STEAM_WEB_API_KEY != null ? BuildConfig.STEAM_WEB_API_KEY : "";
            String steamId = prefs.getSteamId();
            boolean hasKey = apiKey != null && !apiKey.trim().isEmpty();
            boolean hasSteamId = steamId != null && !steamId.trim().isEmpty();
            List<SteamWebApi.Game> list = SteamWebApi.getOwnedGames(apiKey, steamId);
            List<SteamWebApi.Game> recent = new ArrayList<>();
            for (SteamWebApi.Game g : list) if (g.playtimeMinutes > 0) recent.add(g);
            Collections.sort(recent, (a, b) -> Long.compare(b.playtimeMinutes, a.playtimeMinutes));
            if (recent.size() > RECENTLY_PLAYED_MAX) recent = recent.subList(0, RECENTLY_PLAYED_MAX);
            final List<SteamWebApi.Game> recentFinal = recent;
            final boolean hasKeyFinal = hasKey;
            final boolean hasSteamIdFinal = hasSteamId;
            requireActivity().runOnUiThread(() -> {
                games = list;
                recentlyPlayed = recentFinal;
                grid.setAdapter(new SteamGameAdapter(games, this::onPlayGame));
                recentRecycler.setAdapter(new SteamRecentAdapter(recentlyPlayed, this::onPlayGame));
                labelRecentlyPlayed.setVisibility(recentlyPlayed.isEmpty() ? View.GONE : View.VISIBLE);
                recentRecycler.setVisibility(recentlyPlayed.isEmpty() ? View.GONE : View.VISIBLE);
                gameCountText.setText(String.valueOf(games.size()));
                if (games.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    if (!hasKeyFinal)
                        emptyText.setText(R.string.steam_library_no_key);
                    else if (!hasSteamIdFinal)
                        emptyText.setText(R.string.steam_library_empty);
                    else
                        emptyText.setText(R.string.steam_library_no_games);
                } else {
                    emptyText.setVisibility(View.GONE);
                }
                refreshLayout.setRefreshing(false);
            });
        });
    }

    private void openSteamStore() {
        try {
            Intent i = new Intent(requireContext(), SteamStoreActivity.class);
            i.putExtra(SteamStoreActivity.EXTRA_URL, getString(R.string.steam_store_url));
            i.putExtra(SteamStoreActivity.EXTRA_TITLE, getString(R.string.steam_find_games));
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), R.string.steam_find_games, Toast.LENGTH_SHORT).show();
        }
    }

    private void onPlayGame(SteamWebApi.Game game) {
        ContainerManager cm = new ContainerManager(requireContext());
        List<Container> containers = cm.getContainers();
        if (containers == null || containers.isEmpty()) {
            Toast.makeText(requireContext(), R.string.steam_need_container, Toast.LENGTH_LONG).show();
            return;
        }
        Container container = containers.get(0);
        File desktopDir = container.getDesktopDir();
        if (!desktopDir.isDirectory()) desktopDir.mkdirs();
        String safeName = "Steam " + game.appId + ".desktop";
        File desktopFile = new File(desktopDir, safeName);
        // Windows path for Steam; execArgs in Extra Data for -applaunch
        String steamWinPath = "C:\\Program Files (x86)\\Steam\\steam.exe";
        String content = "[Desktop Entry]\nType=Application\nName=" + game.name + "\nExec=wine \"" + steamWinPath + "\"\n\n[Extra Data]\nexecArgs=-applaunch " + game.appId + "\n";
        FileUtils.writeString(desktopFile, content);
        Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        intent.putExtra("shortcut_path", desktopFile.getPath());
        startActivity(intent);
    }

    private static class SteamGameAdapter extends RecyclerView.Adapter<SteamGameAdapter.Holder> {
        private final List<SteamWebApi.Game> list;
        private final OnPlayListener onPlay;

        SteamGameAdapter(List<SteamWebApi.Game> list, OnPlayListener onPlay) {
            this.list = list;
            this.onPlay = onPlay;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.steam_game_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            SteamWebApi.Game g = list.get(position);
            h.name.setText(g.name);
            long hrs = g.playtimeMinutes / 60;
            h.playtime.setText(hrs > 0 ? h.itemView.getContext().getString(R.string.steam_playtime, (int) hrs) : "");
            h.playtime.setVisibility(hrs > 0 ? View.VISIBLE : View.GONE);
            h.image.setImageDrawable(null);
            loadImage(h.image, g.headerUrl);
            h.play.setOnClickListener(v -> onPlay.onPlay(g));
            h.itemView.setOnClickListener(v -> onPlay.onPlay(g));
        }

        private void loadImage(ImageView iv, String url) {
            loadImageStatic(iv, url);
        }

        static void loadImageStatic(ImageView iv, String url) {
            iv.setTag(url);
            new Thread(() -> {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(5000);
                    InputStream in = c.getInputStream();
                    Bitmap b = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                    if (b != null && url.equals(iv.getTag()))
                        iv.post(() -> { if (url.equals(iv.getTag())) iv.setImageBitmap(b); });
                } catch (Throwable t) { /* ignore */ }
            }).start();
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name;
            final TextView playtime;
            final Button play;

            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.SteamGameImage);
                name = itemView.findViewById(R.id.SteamGameName);
                playtime = itemView.findViewById(R.id.SteamGamePlaytime);
                play = itemView.findViewById(R.id.SteamGamePlay);
            }
        }
    }

    /** Horizontal list adapter for recently played (compact cards). */
    private static class SteamRecentAdapter extends RecyclerView.Adapter<SteamRecentAdapter.Holder> {
        private final List<SteamWebApi.Game> list;
        private final OnPlayListener onPlay;

        SteamRecentAdapter(List<SteamWebApi.Game> list, OnPlayListener onPlay) {
            this.list = list;
            this.onPlay = onPlay;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.steam_game_card_recent, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            SteamWebApi.Game g = list.get(position);
            h.name.setText(g.name);
            h.playtime.setText(g.playtimeMinutes >= 60 ? h.itemView.getContext().getString(R.string.steam_playtime, (int)(g.playtimeMinutes / 60)) : g.playtimeMinutes + " min");
            h.image.setImageDrawable(null);
            SteamGameAdapter.loadImageStatic(h.image, g.headerUrl);
            h.itemView.setOnClickListener(v -> onPlay.onPlay(g));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView name, playtime;
            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.SteamGameImage);
                name = itemView.findViewById(R.id.SteamGameName);
                playtime = itemView.findViewById(R.id.SteamGamePlaytime);
            }
        }
    }
}
