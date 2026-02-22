package com.winlator.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Steam Web API: GetOwnedGames.
 * Requires API key from https://steamcommunity.com/dev/apikey
 */
public final class SteamWebApi {
    private static final String BASE = "https://api.steampowered.com";

    public static class Game {
        public final int appId;
        public final String name;
        public final long playtimeMinutes;
        public final String logoUrl;
        public final String headerUrl;

        Game(int appId, String name, long playtimeMinutes, String logoUrl) {
            this.appId = appId;
            this.name = name != null ? name : ("App " + appId);
            this.playtimeMinutes = playtimeMinutes;
            this.logoUrl = logoUrl;
            this.headerUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/header.jpg";
        }
    }

    /** Fetch owned games. Returns empty list on error or missing key. */
    public static List<Game> getOwnedGames(String apiKey, String steamId) {
        List<Game> out = new ArrayList<>();
        if (apiKey == null || apiKey.trim().isEmpty() || steamId == null || steamId.trim().isEmpty())
            return out;
        try {
            String u = BASE + "/IPlayerService/GetOwnedGames/v0001/?key=" + apiKey.trim() + "&steamid=" + steamId.trim() + "&format=json&include_appinfo=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) return out;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject response = root.optJSONObject("response");
            if (response == null) return out;
            JSONArray games = response.optJSONArray("games");
            if (games == null) return out;
            for (int i = 0; i < games.length(); i++) {
                JSONObject g = games.optJSONObject(i);
                if (g == null) continue;
                int appid = g.optInt("appid", 0);
                String name = g.optString("name", "");
                long playtime = g.optLong("playtime_forever", 0);
                String img = g.optString("img_logo_url", "");
                out.add(new Game(appid, name, playtime, img));
            }
        } catch (Throwable t) {
            // ignore
        }
        return out;
    }
}
