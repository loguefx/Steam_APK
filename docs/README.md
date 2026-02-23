# Docs (GitHub Pages) — required for Steam sign-in

The Steam sign-in callback uses this URL. **You must enable GitHub Pages** or you’ll get a 404 when Steam redirects here.

## Option A: Deploy from a branch (simplest)

1. In the repo: **Settings** → **Pages**
2. Under **Build and deployment** → **Source**: choose **Deploy from a branch**
3. **Branch**: `main` (or `master`) → **/docs** → **Save**
4. Wait 1–2 minutes. Open `https://loguefx.github.io/Steam_APK/steam-callback.html` in a browser — you should see “Redirecting back to app…” instead of 404.

## Option B: Deploy from GitHub Actions

If you prefer **GitHub Actions** as the source, use the **Deploy GitHub Pages** workflow (`.github/workflows/pages.yml`). It checks out with `submodules: false` so the OpenXR-SDK submodule is not initialized and the “No url found for submodule” error is avoided. In **Settings** → **Pages** → **Source** choose **GitHub Actions**, then select the **Deploy GitHub Pages** workflow.

## If you see “No url found for submodule” in CI

That happens when a workflow runs `git submodule update` and the repo has a root `.gitmodules` with a missing or invalid URL. Fixes:

- For **Pages**: use Option A above, or use the workflow in Option B (it does not init submodules).
- To remove the broken submodule from the repo: delete the root `.gitmodules` file (if it exists), run `git rm --cached app/src/main/cpp/OpenXR-SDK` from the repo root, commit and push. The APK release workflow already avoids submodules by checking out with `submodules: false` and cloning OpenXR-SDK manually.
