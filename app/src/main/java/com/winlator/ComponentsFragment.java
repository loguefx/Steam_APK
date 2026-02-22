package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.ComponentApiClient;

/**
 * Shows compatibility component info from gamehub_api (Box64, DXVK, drivers).
 * Used to verify API connectivity; full download/install can be added later.
 */
public class ComponentsFragment extends Fragment {
    private Button refreshButton;
    private TextView statusText;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.components_compatibility_title);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.components_fragment, container, false);
        refreshButton = view.findViewById(R.id.BTRefreshComponents);
        statusText = view.findViewById(R.id.TVComponentsStatus);
        refreshButton.setText(R.string.refresh_components);
        refreshButton.setEnabled(true);
        statusText.setText(getString(R.string.components_compatibility_title) + "\n\nTap Refresh to load component counts from Game Hub API.");

        refreshButton.setOnClickListener((v) -> loadManifests());
        return view;
    }

    private void loadManifests() {
        refreshButton.setEnabled(false);
        refreshButton.setText(R.string.loading);
        statusText.setText("Loading…");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            ComponentApiClient.ManifestResult box64 = ComponentApiClient.fetchManifest("box64_manifest");
            if (box64 != null) {
                sb.append("Box64: ").append(box64.total).append(" components\n");
            } else {
                sb.append("Box64: failed to load\n");
            }
            ComponentApiClient.ManifestResult dxvk = ComponentApiClient.fetchManifest("dxvk_manifest");
            if (dxvk != null) {
                sb.append("DXVK: ").append(dxvk.total).append(" components\n");
            } else {
                sb.append("DXVK: failed to load\n");
            }
            ComponentApiClient.ManifestResult drivers = ComponentApiClient.fetchManifest("drivers_manifest");
            if (drivers != null) {
                sb.append("Drivers: ").append(drivers.total).append(" components\n");
            } else {
                sb.append("Drivers: failed to load\n");
            }
            final String result = sb.toString();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    statusText.setText(result);
                    refreshButton.setEnabled(true);
                    refreshButton.setText(R.string.refresh_components);
                });
            }
        }).start();
    }
}
