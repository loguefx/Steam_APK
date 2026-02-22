package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.gamehubpp.ClearCacheHelper;
import com.winlator.gamehubpp.DeviceInfo;
import com.winlator.gamehubpp.LaunchCoordinator;
import com.winlator.gamehubpp.Profile;
import com.winlator.gamehubpp.ProfileStore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shows device info, compatibility explanation, clear cache, and export/import profile.
 */
public class CompatibilityFragment extends Fragment {
    private static final int REQUEST_IMPORT_PROFILE = 30;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.compatibility);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_PROFILE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(data.getData());
                if (is == null) return;
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                is.close();
                Profile p = Profile.fromJson(new JSONObject(sb.toString()));
                LaunchCoordinator.get(requireContext()).getProfileStore().saveProfile(p);
                Toast.makeText(requireContext(), getString(R.string.import_profile_bundle) + " OK", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Import failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compatibility_fragment, container, false);
        TextView tvDevice = view.findViewById(R.id.TVDeviceInfo);
        DeviceInfo device = LaunchCoordinator.get(requireContext()).getDeviceInfo();
        tvDevice.setText(getString(R.string.compatibility_device_info, device.deviceModel, device.androidSdk, device.gpuFamily));

        view.findViewById(R.id.BTClearTempCache).setOnClickListener(v -> {
            ClearCacheHelper.clearTempCache(requireContext());
            Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.BTClearShaderCache).setOnClickListener(v -> {
            ClearCacheHelper.clearShaderCache(requireContext());
            Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.BTExportProfile).setOnClickListener(v -> {
            ProfileStore store = LaunchCoordinator.get(requireContext()).getProfileStore();
            Profile safe = store.loadProfile("profile_safe_v1");
            if (safe == null) safe = ProfileStore.createDefaultSafeProfile();
            try {
                File out = new File(requireContext().getExternalFilesDir(null), "exported_safe_profile.json");
                try (FileOutputStream os = new FileOutputStream(out)) {
                    os.write(safe.toJson().toString().getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(requireContext(), "Exported to " + out.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.BTImportProfile).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");
            startActivityForResult(i, REQUEST_IMPORT_PROFILE);
        });
        return view;
    }
}
