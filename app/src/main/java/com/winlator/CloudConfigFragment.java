package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.gamehubpp.ConfigPack;
import com.winlator.gamehubpp.DeviceInfoCollector;
import com.winlator.gamehubpp.LaunchCoordinator;
import com.winlator.gamehubpp.PackManager;

import java.util.List;

/**
 * Cloud config: check for updates, list cached packs, Install as Candidate.
 */
public class CloudConfigFragment extends Fragment {
    private static final String DEFAULT_PACK_BASE_URL = ""; // Set to your pack server or GitHub raw URL

    private TextView tvStatus;
    private TextView tvCachedPacks;
    private Button btInstallCandidate;
    private String selectedPackId;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.cloud_config);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cloud_config_fragment, container, false);
        tvStatus = view.findViewById(R.id.TVCloudStatus);
        tvCachedPacks = view.findViewById(R.id.TVCachedPacks);
        btInstallCandidate = view.findViewById(R.id.BTInstallCandidate);

        view.findViewById(R.id.BTCheckForUpdates).setOnClickListener(v -> checkForUpdates());
        btInstallCandidate.setOnClickListener(v -> installAsCandidate());

        refreshCachedList();
        return view;
    }

    private void checkForUpdates() {
        tvStatus.setText(getString(R.string.loading));
        new Thread(() -> {
            String url = DEFAULT_PACK_BASE_URL;
            if (url.isEmpty()) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    tvStatus.setText("No pack URL configured. Add DEFAULT_PACK_BASE_URL in CloudConfigFragment.");
                    refreshCachedList();
                });
                return;
            }
            PackManager pm = new PackManager(requireContext());
            String packId = pm.downloadPack(url);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (packId != null) {
                    tvStatus.setText("Downloaded pack: " + packId);
                    selectedPackId = packId;
                    btInstallCandidate.setVisibility(View.VISIBLE);
                } else {
                    tvStatus.setText("Download failed or no pack URL.");
                }
                refreshCachedList();
            });
        }).start();
    }

    private void refreshCachedList() {
        PackManager pm = new PackManager(requireContext());
        List<String> ids = pm.listCachedPackIds();
        if (ids.isEmpty()) {
            tvCachedPacks.setText(R.string.no_cached_packs);
            btInstallCandidate.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String id : ids) {
                ConfigPack pack = pm.loadCachedPack(id);
                sb.append(id);
                if (pack != null) sb.append(" (v").append(pack.profilesVersion).append(")\n");
                else sb.append("\n");
            }
            tvCachedPacks.setText(sb.toString());
            if (selectedPackId == null && !ids.isEmpty()) selectedPackId = ids.get(0);
            btInstallCandidate.setVisibility(View.VISIBLE);
        }
    }

    private void installAsCandidate() {
        if (selectedPackId == null) return;
        PackManager pm = new PackManager(requireContext());
        ConfigPack pack = pm.loadCachedPack(selectedPackId);
        if (pack == null) {
            Toast.makeText(requireContext(), "Pack not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pm.verifyPackChecksum(selectedPackId)) {
            Toast.makeText(requireContext(), "Pack checksum failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        LaunchCoordinator lc = LaunchCoordinator.get(requireContext());
        int n = pm.applyPackAsCandidateForAllGames(pack, lc.getProfileStore(), lc.getDeviceInfo());
        Toast.makeText(requireContext(), getString(R.string.pack_installed_as_candidate, n), Toast.LENGTH_SHORT).show();
    }
}
