package com.example.frontend;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LeagueCreateActivity extends AppCompatActivity {

    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_league_create);

        apiClient = new ApiClient(this);

        EditText editName = findViewById(R.id.editLeagueName);
        EditText editDesc = findViewById(R.id.editLeagueDesc);
        EditText editDate = findViewById(R.id.editLeagueDate);
        EditText editPoints = findViewById(R.id.editStartingPoints);
        Button btnCreate = findViewById(R.id.btnCreateLeague);

        btnCreate.setOnClickListener(v -> {
            String name = editName.getText().toString();
            String date = editDate.getText().toString();
            String pointsStr = editPoints.getText().toString();
            
            if (name.isEmpty() || date.isEmpty() || pointsStr.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }
            int points = Integer.parseInt(pointsStr);

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("description", editDesc.getText().toString());
            data.put("end_date", date);
            data.put("starting_points", points);

            apiClient.post("leagues/", data, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LeagueCreateActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(LeagueCreateActivity.this, "Liga creada!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(LeagueCreateActivity.this, "Error al crear", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }
}
