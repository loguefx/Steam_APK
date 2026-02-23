/**
 * Cloudflare Worker: proxy Steam GetOwnedGames so the app never needs an API key.
 * Set secret STEAM_WEB_API_KEY in the Worker's settings.
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const steamid = url.searchParams.get('steamid');
    if (!steamid || !/^\d+$/.test(steamid)) {
      return new Response(JSON.stringify({ response: { games: [] } }), {
        headers: { 'Content-Type': 'application/json' },
        status: 400
      });
    }
    const key = env.STEAM_WEB_API_KEY;
    if (!key) {
      return new Response(JSON.stringify({ response: { games: [] } }), {
        headers: { 'Content-Type': 'application/json' },
        status: 500
      });
    }
    const steamUrl = 'https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key=' +
      encodeURIComponent(key) + '&steamid=' + steamid + '&format=json&include_appinfo=1';
    const res = await fetch(steamUrl);
    const text = await res.text();
    return new Response(text, {
      headers: { 'Content-Type': 'application/json' },
      status: res.status
    });
  }
};
