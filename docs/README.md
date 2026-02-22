# Docs (GitHub Pages)

Enable **GitHub Pages** for this repo so the Steam sign-in callback URL works:

1. In the repo: **Settings** → **Pages**
2. **Source**: Deploy from a branch
3. **Branch**: `main` (or `master`) → **/docs** → Save

Then `https://loguefx.github.io/Steam_APK/steam-callback.html` will be live and Steam OpenID can redirect there after sign-in.
