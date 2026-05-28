package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiClient = new ApiClient(this);

        EditText editUser = findViewById(R.id.editLogUsername);
        EditText editPass = findViewById(R.id.editLogPassword);

        findViewById(R.id.txtGoToRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        findViewById(R.id.btnDoLogin).setOnClickListener(v -> {
            String uname = editUser.getText().toString().trim();
            String pwd = editPass.getText().toString().trim();
            if(uname.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "Completa usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("username", uname);
            data.put("password", pwd);

            apiClient.post("auth/login/", data, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            String token = json.getString("token");
                            apiClient.saveToken(token);
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Bienvenido", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }
}
