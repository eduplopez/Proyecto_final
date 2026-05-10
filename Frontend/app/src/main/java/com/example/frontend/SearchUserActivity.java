package com.example.frontend;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SearchUserActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private int leagueId;
    private ArrayList<JSONObject> foundUsers = new ArrayList<>();
    private ArrayList<String> displayUsers = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        apiClient = new ApiClient(this);
        leagueId = getIntent().getIntExtra("league_id", -1);

        EditText editSearch = findViewById(R.id.editSearchUser);
        Button btnSearch = findViewById(R.id.btnSearch);
        ListView listView = findViewById(R.id.listSearchResults);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayUsers);
        listView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> search(editSearch.getText().toString()));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                int userId = foundUsers.get(position).getInt("id");
                inviteUser(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void search(String query) {
        apiClient.get("users/?search=" + query, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchUserActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray users = json.getJSONArray("users");
                        foundUsers.clear();
                        displayUsers.clear();
                        for (int i=0; i<users.length(); i++) {
                            JSONObject u = users.getJSONObject(i);
                            foundUsers.add(u);
                            displayUsers.add(u.getString("username") + " (" + u.optString("first_name", "") + ")");
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void inviteUser(int userId) {
        Map<String, Integer> data = new HashMap<>();
        data.put("user_id", userId);

        apiClient.post("leagues/" + leagueId + "/invite/", data, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchUserActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(SearchUserActivity.this, "Usuario invitado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Return to previous screen
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(SearchUserActivity.this, "Error al invitar, posiblemente ya existe", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
