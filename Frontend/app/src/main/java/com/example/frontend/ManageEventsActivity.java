package com.example.frontend;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ManageEventsActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private int leagueId;
    private ListView listEvents;
    private ArrayList<EventItem> events = new ArrayList<>();
    private EventAdapter adapter;
    private ArrayList<LeagueDetailsActivity.Participant> participants = new ArrayList<>();

    static class EventItem {
        int id;
        String name;
        String type;
        String description;
        int rewardPoints;
        String status;
    }

    class EventAdapter extends ArrayAdapter<EventItem> {
        public EventAdapter() {
            super(ManageEventsActivity.this, R.layout.item_event, events);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event, parent, false);
            }
            EventItem e = getItem(position);
            TextView textName = convertView.findViewById(R.id.textEventName);
            TextView textDetails = convertView.findViewById(R.id.textEventDetails);
            Button btnWinner = convertView.findViewById(R.id.btnSetWinner);

            textName.setText(e.name);
            String descText = e.description != null && !e.description.isEmpty() ? "\n" + e.description : "";
            textDetails.setText("Tipo: " + e.type + " • Recompensa: " + e.rewardPoints + " pts" + descText);
            
            if ("FINISHED".equals(e.status)) {
                btnWinner.setVisibility(View.GONE);
                textName.setPaintFlags(textName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                textDetails.setText(textDetails.getText() + " (Finalizado)");
            } else {
                btnWinner.setVisibility(View.VISIBLE);
                textName.setPaintFlags(textName.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                btnWinner.setOnClickListener(v -> showWinnerDialog(e.id));
            }

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        leagueId = getIntent().getIntExtra("league_id", -1);
        apiClient = new ApiClient(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        listEvents = findViewById(R.id.listEvents);
        adapter = new EventAdapter();
        listEvents.setAdapter(adapter);

        loadEvents();
        loadParticipants();
    }

    private void loadEvents() {
        apiClient.get("leagues/" + leagueId + "/events/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ManageEventsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray arr = json.getJSONArray("events");
                        events.clear();
                        for (int i=0; i<arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            EventItem ev = new EventItem();
                            ev.id = obj.getInt("id");
                            ev.name = obj.getString("name");
                            ev.description = obj.optString("description", "");
                            ev.type = obj.getString("event_type");
                            ev.rewardPoints = obj.getInt("reward_points");
                            ev.status = obj.getString("status");
                            events.add(ev);
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void loadParticipants() {
        apiClient.get("leagues/" + leagueId + "/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray parts = json.getJSONArray("participants");
                        participants.clear();
                        for (int i=0; i<parts.length(); i++) {
                            JSONObject obj = parts.getJSONObject(i);
                            LeagueDetailsActivity.Participant p = new LeagueDetailsActivity.Participant();
                            p.id = obj.getInt("user_id");
                            p.username = obj.getString("username");
                            participants.add(p);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showWinnerDialog(int eventId) {
        if (participants.isEmpty()) {
            Toast.makeText(this, "Cargando participantes...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] names = new String[participants.size()];
        for (int i=0; i<participants.size(); i++) {
            names[i] = participants.get(i).username;
        }

        new AlertDialog.Builder(this)
            .setTitle("Seleccionar ganador")
            .setItems(names, (dialog, which) -> {
                setWinner(eventId, participants.get(which).id);
            })
            .show();
    }

    private void setWinner(int eventId, int userId) {
        HashMap<String, Object> body = new HashMap<>();
        body.put("winner_id", userId);
        apiClient.put("leagues/" + leagueId + "/events/" + eventId + "/", body, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ManageEventsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ManageEventsActivity.this, "Ganador establecido. Puntos repartidos.", Toast.LENGTH_SHORT).show();
                        loadEvents();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ManageEventsActivity.this, "Error al actualizar evento", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
