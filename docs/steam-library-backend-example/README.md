# Steam library backend — one-time setup

You set this up **once**. After that, every user of your app just signs in with Steam and sees their library; no one else configures anything.

---

## One-time setup (you do this once)

### Step 1: Get a Steam Web API key

1. Open **https://steamcommunity.com/dev/apikey** in a browser and sign in to Steam.
2. Register a key (domain can be `localhost` or your app’s domain).
3. Copy the key (e.g. `ABCD1234...`). You’ll use it only in Step 2.

### Step 2: Deploy the backend (Cloudflare Worker, free)

1. Create a **Cloudflare** account: https://dash.cloudflare.com → sign up.
2. Go to **Workers & Pages** → **Create** → **Create Worker**.
3. Name it (e.g. `steam-library`). Click **Deploy** (keep the default “Hello World” for now).
4. Click **Edit code**. Delete the default code and paste in the full contents of **`worker.js`** from this folder. Click **Save and deploy**.
5. Add your API key as a secret:  
   In the Worker page, open **Settings** → **Variables** → **Add variable**  
   - **Variable name:** `STEAM_WEB_API_KEY`  
   - **Value:** your key from Step 1  
   - **Type:** Secret (encrypted)  
   Click **Save**.
6. Copy your Worker URL (e.g. `https://steam-library.your-subdomain.workers.dev`). You’ll use it in Step 3.

### Step 3: Point the app at your backend

1. In the app project, open **`app/src/main/res/values/strings.xml`**.
2. Find the line:
   ```xml
   <string name="steam_library_backend_url"></string>
   ```
3. Set it to your Worker URL (no trailing slash), for example:
   ```xml
   <string name="steam_library_backend_url">https://steam-library.your-subdomain.workers.dev</string>
   ```
4. Save the file, **rebuild the app**, and distribute it (e.g. APK or store).

**Done.** From now on, every user only signs in with Steam; the app calls your backend and their library loads. No other user needs to configure anything.

---

## How it works

1. User signs in with Steam in the app (OpenID). App has their Steam ID.
2. App calls: `GET your-backend-url?steamid=76561198...`
3. Your backend calls Steam: `https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=YOUR_KEY&steamid=76561198...&format=json&include_appinfo=1`
4. Your backend returns the same JSON (or `{"response":{"games":[...]}}`) to the app.
5. App shows the library. No API key in the app.

## Option 1: Cloudflare Worker (free)

1. Create a [Cloudflare](https://dash.cloudflare.com) account and go to Workers & Pages.
2. Create a Worker and paste the code from `worker.js`.
3. Add a secret: Workers → your Worker → Settings → Variables → `STEAM_WEB_API_KEY` = your key from https://steamcommunity.com/dev/apikey
4. Deploy. Your URL is like `https://your-worker.your-subdomain.workers.dev`.
5. In the app’s `strings.xml`, set:
   ```xml
   <string name="steam_library_backend_url">https://your-worker.your-subdomain.workers.dev</string>
   ```
6. Rebuild the app. When users sign in, the app will call your Worker and the library will load automatically.

## Option 2: Any HTTP server

Your endpoint must:

- Accept: `GET /?steamid=STEAMID64` (or `/games?steamid=...` — use the same base URL in the app).
- Return JSON in this shape (same as Steam’s response):
  ```json
  {
    "response": {
      "games": [
        { "appid": 123, "name": "Game Name", "playtime_forever": 120, "img_logo_url": "..." }
      ]
    }
  }
  ```
- Your server calls `https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=YOUR_KEY&steamid=...&format=json&include_appinfo=1` and returns the response (or the `response` part).

Set that base URL in `steam_library_backend_url` in the app.
