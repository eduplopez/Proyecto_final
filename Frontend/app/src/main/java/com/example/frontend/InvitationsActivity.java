package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class InvitationsActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private ListView listView;
    private ArrayList<String> displayList = new ArrayList<>();
    private ArrayList<Integer> leagueIds = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        apiClient = new ApiClient(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        listView = findViewById(R.id.listInvitations);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int leagueId = leagueIds.get(position);
            String text = displayList.get(position);

            new AlertDialog.Builder(this)
                .setTitle("Invitación")
                .setMessage("¿Qué quieres hacer con esta invitación a:\n" + text + "?")
                .setPositiveButton("Aceptar", (dialog, which) -> respondInvite(leagueId, "accept"))
                .setNegativeButton("Rechazar", (dialog, which) -> respondInvite(leagueId, "reject"))
                .setNeutralButton("Cancelar", null)
                .show();
        });

        loadInvitations();
    }

    private void loadInvitations() {
        apiClient.get("invitations/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray array = json.getJSONArray("invitations");
                        displayList.clear();
                        leagueIds.clear();
                        for(int i=0; i<array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            displayList.add(obj.getString("league_name") + " (de " + obj.getString("creator_name") + ")");
                            leagueIds.add(obj.getInt("league_id"));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void respondInvite(int leagueId, String action) {
        try {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("action", action);
            apiClient.post("leagues/" + leagueId + "/respond/", body, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(InvitationsActivity.this, action.equals("accept") ? "Aceptada" : "Rechazada", Toast.LENGTH_SHORT).show();
                            loadInvitations();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(InvitationsActivity.this, "Error al procesar la invitación", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
