package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private ArrayList<String> participants = new ArrayList<>();
    private ArrayAdapter<String> adapter;

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
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, participants);
        listView.setAdapter(adapter);

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
                        boolean isCreator = false;
                        if (leagueObj.has("is_creator")) {
                            isCreator = leagueObj.getBoolean("is_creator");
                        }
                        
                        final boolean finalIsCreator = isCreator;

                        JSONArray parts = json.getJSONArray("participants");
                        participants.clear();
                        for (int i=0; i<parts.length(); i++) {
                            JSONObject p = parts.getJSONObject(i);
                            participants.add(p.getString("username") + " - Puntos: " + p.getInt("points"));
                        }
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            android.widget.Button btnDelete = findViewById(R.id.btnDeleteLeague);
                            if (finalIsCreator) {
                                btnDelete.setText("Eliminar Liga");
                                btnDelete.setOnClickListener(v -> deleteLeague());
                            } else {
                                btnDelete.setText("Abandonar Liga");
                                btnDelete.setOnClickListener(v -> leaveLeague());
                                findViewById(R.id.fabInvite).setVisibility(android.view.View.GONE);
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
