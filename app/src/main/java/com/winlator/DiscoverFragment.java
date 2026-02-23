package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Game Hub Discover: our own curated page (not Steam store).
 * Data from assets/discover.json.
 */
public class DiscoverFragment extends Fragment {
    private RecyclerView recycler;
    private View emptyView;
    private final List<DiscoverSection> sections = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.discover_fragment, container, false);
        recycler = root.findViewById(R.id.DiscoverRecycler);
        emptyView = root.findViewById(R.id.DiscoverEmpty);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.steam_discover);
        loadDiscover();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(new DiscoverAdapter(sections));
        emptyView.setVisibility(sections.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadDiscover() {
        sections.clear();
        try {
            String json = FileUtils.readString(requireContext(), "discover.json");
            if (json == null) return;
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("sections");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject sec = arr.getJSONObject(i);
                String id = sec.optString("id", "");
                String title = sec.optString("title", "");
                List<DiscoverItem> items = new ArrayList<>();
                JSONArray itemsArr = sec.optJSONArray("items");
                if (itemsArr != null) {
                    for (int j = 0; j < itemsArr.length(); j++) {
                        JSONObject it = itemsArr.getJSONObject(j);
                        items.add(new DiscoverItem(
                            it.optString("id", ""),
                            it.optString("title", ""),
                            it.optString("subtitle", ""),
                            it.optString("badge", ""),
                            it.optString("appId", "")
                        ));
                    }
                }
                sections.add(new DiscoverSection(id, title, items));
            }
        } catch (Exception ignored) {
        }
    }

    static final class DiscoverSection {
        final String id;
        final String title;
        final List<DiscoverItem> items;

        DiscoverSection(String id, String title, List<DiscoverItem> items) {
            this.id = id;
            this.title = title;
            this.items = items != null ? items : new ArrayList<>();
        }
    }

    static final class DiscoverItem {
        final String id;
        final String title;
        final String subtitle;
        final String badge;
        final String appId;

        DiscoverItem(String id, String title, String subtitle, String badge, String appId) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
            this.appId = appId;
        }
    }

    private static class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.SectionHolder> {
        private final List<DiscoverSection> sections;

        DiscoverAdapter(List<DiscoverSection> sections) {
            this.sections = sections;
        }

        @NonNull
        @Override
        public SectionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.discover_section, parent, false);
            return new SectionHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SectionHolder h, int position) {
            DiscoverSection sec = sections.get(position);
            h.title.setText(sec.title);
            h.recycler.setLayoutManager(new LinearLayoutManager(h.recycler.getContext(), LinearLayoutManager.HORIZONTAL, false));
            h.recycler.setAdapter(new DiscoverItemAdapter(sec.items));
        }

        @Override
        public int getItemCount() {
            return sections.size();
        }

        static class SectionHolder extends RecyclerView.ViewHolder {
            final TextView title;
            final RecyclerView recycler;

            SectionHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.DiscoverSectionTitle);
                recycler = itemView.findViewById(R.id.DiscoverSectionRecycler);
            }
        }
    }

    private static class DiscoverItemAdapter extends RecyclerView.Adapter<DiscoverItemAdapter.ItemHolder> {
        private final List<DiscoverItem> items;

        DiscoverItemAdapter(List<DiscoverItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.discover_item, parent, false);
            return new ItemHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder h, int position) {
            DiscoverItem it = items.get(position);
            h.title.setText(it.title);
            h.subtitle.setText(it.subtitle);
            h.subtitle.setVisibility(it.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
            h.badge.setText(it.badge);
            h.badge.setVisibility(it.badge.isEmpty() ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ItemHolder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView subtitle;
            final TextView badge;

            ItemHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.DiscoverItemTitle);
                subtitle = itemView.findViewById(R.id.DiscoverItemSubtitle);
                badge = itemView.findViewById(R.id.DiscoverItemBadge);
            }
        }
    }
}
