package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LeagueDetailsActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private int leagueId;
    private ListView listView;
    private ArrayList<Participant> participants = new ArrayList<>();
    private ParticipantAdapter adapter;
    private boolean isCreator = false;

    public static class Participant {
        public int id;
        public String username;
        public int points;
        public boolean isAdmin;
        public boolean isCreator;
    }

    class ParticipantAdapter extends ArrayAdapter<Participant> {
        public ParticipantAdapter() {
            super(LeagueDetailsActivity.this, R.layout.item_participant, participants);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_participant, parent, false);
            }
            Participant p = getItem(position);
            TextView textName = convertView.findViewById(R.id.textName);
            TextView textAdmin = convertView.findViewById(R.id.textAdmin);
            ImageButton btnOptions = convertView.findViewById(R.id.btnOptions);

            textName.setText(p.username + " - Puntos: " + p.points);
            
            if (p.isCreator) {
                textAdmin.setText("CREADOR");
                textAdmin.setVisibility(View.VISIBLE);
            } else if (p.isAdmin) {
                textAdmin.setText("ADMIN");
                textAdmin.setVisibility(View.VISIBLE);
            } else {
                textAdmin.setVisibility(View.GONE);
            }
            
            if (isCreator && p.id != getSharedPreferences("app_prefs", MODE_PRIVATE).getInt("user_id", -1)) {
                btnOptions.setVisibility(View.VISIBLE);
                btnOptions.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(getContext(), v);
                    popup.getMenu().add(p.isAdmin ? "Quitar Administrador" : "Hacer Administrador");
                    popup.setOnMenuItemClickListener(item -> {
                        toggleAdmin(p.id, !p.isAdmin);
                        return true;
                    });
                    popup.show();
                });
            } else {
                btnOptions.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_league_details);

        leagueId = getIntent().getIntExtra("league_id", -1);
        apiClient = new ApiClient(this);

        TextView name = findViewById(R.id.textDetailLeagueName);
        TextView desc = findViewById(R.id.textDetailLeagueDesc);
        name.setText(getIntent().getStringExtra("name"));
        desc.setText(getIntent().getStringExtra("description"));

        listView = findViewById(R.id.listParticipants);
        adapter = new ParticipantAdapter();
        listView.setAdapter(adapter);

        FloatingActionButton fabAddEvent = findViewById(R.id.fabAddEvent);
        fabAddEvent.setOnClickListener(v -> {
            showAddEventDialog();
        });

        FloatingActionButton fab = findViewById(R.id.fabInvite);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchUserActivity.class);
            intent.putExtra("league_id", leagueId);
            startActivity(intent);
        });

        // Button listener is set in loadDetails()

        loadDetails();
    }

    private void deleteLeague() {
        apiClient.delete("leagues/" + leagueId + "/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(LeagueDetailsActivity.this, "Liga eliminada", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Solo el creador puede eliminar la liga", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void toggleAdmin(int userId, boolean makeAdmin) {
        try {
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("is_admin", makeAdmin);
            apiClient.put("leagues/" + leagueId + "/make_admin/" + userId + "/", body, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(LeagueDetailsActivity.this, makeAdmin ? "Usuario es ahora administrador" : "Administrador eliminado", Toast.LENGTH_SHORT).show();
                            loadDetails();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error al cambiar rol", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddEventDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_event, null);
        android.widget.Spinner spinner = view.findViewById(R.id.spinnerEventType);
        com.google.android.material.textfield.TextInputEditText editName = view.findViewById(R.id.editEventName);
        com.google.android.material.textfield.TextInputEditText editDesc = view.findViewById(R.id.editEventDesc);
        com.google.android.material.textfield.TextInputEditText editPoints = view.findViewById(R.id.editEventPoints);

        String[] types = {"Partido", "Reto", "Apuesta"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Crear", (dialog, which) -> {
                String type = types[spinner.getSelectedItemPosition()];
                String name = editName.getText().toString();
                String desc = editDesc.getText().toString();
                String pointsStr = editPoints.getText().toString();
                int points = pointsStr.isEmpty() ? 0 : Integer.parseInt(pointsStr);
                
                if (!name.isEmpty()) {
                    createEvent(type, name, desc, points);
                } else {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void createEvent(String eventType, String name, String description, int rewardPoints) {
        try {
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("event_type", eventType);
            body.put("name", name);
            body.put("description", description);
            body.put("reward_points", rewardPoints);
            apiClient.post("leagues/" + leagueId + "/events/", body, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(LeagueDetailsActivity.this, "Evento creado", Toast.LENGTH_SHORT).show();
                            loadDetails();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error al crear evento", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDetails() {
        if (leagueId == -1) return;
        apiClient.get("leagues/" + leagueId + "/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject leagueObj = json.getJSONObject("league");
                        boolean localIsCreator = false;
                        if (leagueObj.has("is_creator")) {
                            localIsCreator = leagueObj.getBoolean("is_creator");
                        }
                        LeagueDetailsActivity.this.isCreator = localIsCreator;
                        
                        boolean isAdmin = false;
                        if (leagueObj.has("is_admin")) {
                            isAdmin = leagueObj.getBoolean("is_admin");
                        }

                        JSONArray parts = json.getJSONArray("participants");
                        participants.clear();
                        for (int i=0; i<parts.length(); i++) {
                            JSONObject jsonP = parts.getJSONObject(i);
                            Participant p = new Participant();
                            p.id = jsonP.getInt("user_id");
                            p.username = jsonP.getString("username");
                            p.points = jsonP.getInt("points");
                            if (jsonP.has("is_admin")) {
                                p.isAdmin = jsonP.getBoolean("is_admin");
                            }
                            if (jsonP.has("is_creator")) {
                                p.isCreator = jsonP.getBoolean("is_creator");
                            }
                            participants.add(p);
                        }
                        
                        final boolean finalIsAdmin = isAdmin;
                        
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            android.widget.Button btnDelete = findViewById(R.id.btnDeleteLeague);
                            if (LeagueDetailsActivity.this.isCreator) {
                                btnDelete.setText("Eliminar Liga");
                                btnDelete.setOnClickListener(v -> deleteLeague());
                            } else {
                                btnDelete.setText("Abandonar Liga");
                                btnDelete.setOnClickListener(v -> leaveLeague());
                                findViewById(R.id.fabInvite).setVisibility(android.view.View.GONE);
                            }
                            
                            FloatingActionButton fabAddEvent = findViewById(R.id.fabAddEvent);
                            if (LeagueDetailsActivity.this.isCreator || finalIsAdmin) {
                                fabAddEvent.setVisibility(View.VISIBLE);
                                android.widget.Button btnManageEvents = findViewById(R.id.btnManageEvents);
                                if(btnManageEvents != null) {
                                    btnManageEvents.setVisibility(View.VISIBLE);
                                    btnManageEvents.setOnClickListener(v -> {
                                        Intent intent = new Intent(LeagueDetailsActivity.this, ManageEventsActivity.class);
                                        intent.putExtra("league_id", leagueId);
                                        startActivity(intent);
                                    });
                                }
                            } else {
                                fabAddEvent.setVisibility(View.GONE);
                                android.widget.Button btnManageEvents = findViewById(R.id.btnManageEvents);
                                if(btnManageEvents != null) btnManageEvents.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void leaveLeague() {
        apiClient.post("leagues/" + leagueId + "/leave/", new java.util.HashMap<>(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(LeagueDetailsActivity.this, "Has abandonado la liga", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(LeagueDetailsActivity.this, "Error al abandonar", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
