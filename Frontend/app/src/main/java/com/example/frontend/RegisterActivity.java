package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

public class RegisterActivity extends AppCompatActivity {
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiClient = new ApiClient(this);

        EditText editUser = findViewById(R.id.editRegUsername);
        EditText editPass = findViewById(R.id.editRegPassword);
        EditText editFirst = findViewById(R.id.editRegFirstName);
        Spinner spinnerAvatar = findViewById(R.id.spinnerRegAvatar);

        findViewById(R.id.txtGoToLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.btnDoRegister).setOnClickListener(v -> {
            String uname = editUser.getText().toString().trim();
            String pwd = editPass.getText().toString().trim();
            if(uname.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "Completa usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("username", uname);
            data.put("password", pwd);
            data.put("first_name", editFirst.getText().toString().trim());
            
            int selectedPos = spinnerAvatar.getSelectedItemPosition();
            String selectedAvatar = "avatar" + (selectedPos + 1);
            data.put("profile_photo", selectedAvatar);

            apiClient.post("auth/register/", data, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            String token = json.getString("token");
                            apiClient.saveToken(token);
                            runOnUiThread(() -> {
                                Toast.makeText(RegisterActivity.this, "Registrado con éxito", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Usuario ya existe o error al registrar", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });
    }
}
