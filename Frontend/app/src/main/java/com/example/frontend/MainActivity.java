package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private LeagueAdapter adapter;
    private List<League> leagueList = new ArrayList<>();
    private android.widget.TextView txtStatLeagues, txtStatPlayers, txtStatMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtStatLeagues = findViewById(R.id.txtStatLeagues);
        txtStatPlayers = findViewById(R.id.txtStatPlayers);
        txtStatMatches = findViewById(R.id.txtStatMatches);

        apiClient = new ApiClient(this);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_leagues);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            }
            return true;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerViewLeagues);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new LeagueAdapter(leagueList, league -> {
            Intent intent = new Intent(this, LeagueDetailsActivity.class);
            intent.putExtra("league_id", league.id);
            intent.putExtra("name", league.name);
            intent.putExtra("description", league.description);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabCreateLeague);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, LeagueCreateActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnInvitations).setOnClickListener(v -> {
            startActivity(new Intent(this, InvitationsActivity.class));
        });

        fetchLeagues();
        fetchInvitationsCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchLeagues();
        fetchInvitationsCount();
    }

    private void fetchInvitationsCount() {
        apiClient.get("invitations/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray array = json.getJSONArray("invitations");
                        int count = array.length();
                        runOnUiThread(() -> {
                            android.widget.Button btn = findViewById(R.id.btnInvitations);
                            if (btn != null) {
                                btn.setText("Invitaciones: " + count);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchLeagues() {
        apiClient.get("leagues/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error obteniendo ligas", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String bodyStr = response.body().string();
                        JSONObject json = new JSONObject(bodyStr);
                        JSONArray array = json.getJSONArray("leagues");
                        
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<League>>(){}.getType();
                        List<League> newLeagues = gson.fromJson(array.toString(), listType);
                        
                        runOnUiThread(() -> {
                            leagueList.clear();
                            leagueList.addAll(newLeagues);
                            adapter.notifyDataSetChanged();
                            
                            int totalPlayers = 0;
                            int totalPoints = 0;
                            for (League l : newLeagues) {
                                totalPlayers += l.participant_count;
                                totalPoints += l.user_points;
                            }
                            
                            txtStatLeagues.setText(String.valueOf(newLeagues.size()));
                            txtStatPlayers.setText(String.valueOf(totalPlayers));
                            txtStatMatches.setText(String.valueOf(totalPoints));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error de sesión", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
