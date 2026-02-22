package com.winlator;

import android.os.Build;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.inputcontrols.ExternalController;

import java.util.ArrayList;
import java.util.List;

/**
 * Device center: shows connected game controllers (gamepads/joysticks).
 * Game Hub–style screen with controller status.
 */
public class DeviceFragment extends Fragment {
    private RecyclerView deviceList;
    private View emptyView;
    private final List<InputDevice> controllers = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.device_title);
        refreshDevices();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.device_fragment, container, false);
        deviceList = root.findViewById(R.id.DeviceList);
        deviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDevices();
    }

    private void refreshDevices() {
        controllers.clear();
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && ExternalController.isGameController(device)) {
                controllers.add(device);
            }
        }
        if (deviceList != null && emptyView != null) {
            deviceList.setAdapter(new DeviceAdapter(controllers));
            emptyView.setVisibility(controllers.isEmpty() ? View.VISIBLE : View.GONE);
            deviceList.setVisibility(controllers.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private static final class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        private final List<InputDevice> list;

        DeviceAdapter(List<InputDevice> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InputDevice device = list.get(position);
            String name = device.getName();
            if (name == null || name.isEmpty()) {
                name = holder.itemView.getContext().getString(R.string.input_controls);
            }
            holder.name.setText(name);
            holder.status.setText(holder.itemView.getContext().getString(R.string.controller_connected));
            holder.icon.setContentDescription(name);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView status;

            ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.DeviceIcon);
                name = itemView.findViewById(R.id.DeviceName);
                status = itemView.findViewById(R.id.DeviceStatus);
            }
        }
    }
}
