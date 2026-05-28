package com.example.frontend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeagueAdapter extends RecyclerView.Adapter<LeagueAdapter.LeagueViewHolder> {

    private List<League> leagues;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(League league);
    }

    public LeagueAdapter(List<League> leagues, OnItemClickListener listener) {
        this.leagues = leagues;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LeagueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_league, parent, false);
        return new LeagueViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LeagueViewHolder holder, int position) {
        League league = leagues.get(position);
        holder.name.setText(league.name);
        holder.desc.setText(league.description);
        holder.date.setText("Termina: " + league.end_date);
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(league));
    }

    @Override
    public int getItemCount() {
        return leagues != null ? leagues.size() : 0;
    }

    public void setLeagues(List<League> newLeagues) {
        this.leagues = newLeagues;
        notifyDataSetChanged();
    }

    static class LeagueViewHolder extends RecyclerView.ViewHolder {
        TextView name, desc, date;

        public LeagueViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textLeagueName);
            desc = itemView.findViewById(R.id.textLeagueDesc);
            date = itemView.findViewById(R.id.textLeagueEndDate);
        }
    }
}
