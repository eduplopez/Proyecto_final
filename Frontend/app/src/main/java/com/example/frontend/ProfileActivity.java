package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class ProfileActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private EditText editName;
    private Spinner spinnerAvatar;
    private ImageView imgPreview;
    private View layoutViewProfile, layoutEditProfile;
    private ImageView imgViewProfilePreview;
    private TextView textViewProfileName;
    private TextView textCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        apiClient = new ApiClient(this);

        editName = findViewById(R.id.editProfileName);
        spinnerAvatar = findViewById(R.id.spinnerProfileAvatar);
        imgPreview = findViewById(R.id.imgProfilePreview);
        textCount = findViewById(R.id.textLeagueCount);

        layoutViewProfile = findViewById(R.id.layoutViewProfile);
        layoutEditProfile = findViewById(R.id.layoutEditProfile);
        imgViewProfilePreview = findViewById(R.id.imgViewProfilePreview);
        textViewProfileName = findViewById(R.id.textViewProfileName);

        spinnerAvatar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int avatarRes = getResources().getIdentifier("avatar" + (position + 1), "drawable", getPackageName());
                if (avatarRes != 0) {
                    imgPreview.setImageResource(avatarRes);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        Button btnCancelEdit = findViewById(R.id.btnCancelEdit);

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnEditProfile.setOnClickListener(v -> setEditMode(true));
        btnCancelEdit.setOnClickListener(v -> {
            setEditMode(false);
            loadProfile();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_leagues) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            }
            return true;
        });

        loadProfile();
    }

    private void loadProfile() {
        apiClient.get("users/profile/", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String bodyStr = response.body().string();
                        JSONObject json = new JSONObject(bodyStr);
                        JSONObject userObj = json.getJSONObject("user");
                        int count = json.getInt("league_count");

                        String firstName = userObj.optString("first_name", "");
                        String photo = userObj.optString("profile_photo", "avatar1");
                        
                        int selection = 0;
                        if (photo != null && photo.startsWith("avatar")) {
                            try {
                                selection = Integer.parseInt(photo.substring(6)) - 1;
                                if (selection < 0 || selection > 3) selection = 0;
                            } catch(Exception ignored) {}
                        }
                        final int finalSelection = selection;
                        final int avatarRes = getResources().getIdentifier("avatar" + (finalSelection + 1), "drawable", getPackageName());

                        runOnUiThread(() -> {
                            editName.setText(firstName);
                            spinnerAvatar.setSelection(finalSelection);

                            textViewProfileName.setText(firstName.isEmpty() ? "Sin Nombre" : firstName);

                            if (avatarRes != 0) {
                                imgPreview.setImageResource(avatarRes);
                                imgViewProfilePreview.setImageResource(avatarRes);
                            }
                            textCount.setText("Ligas inscritas: " + count);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void saveProfile() {
        Map<String, String> data = new HashMap<>();
        data.put("first_name", editName.getText().toString());
        
        int selectedPos = spinnerAvatar.getSelectedItemPosition();
        String selectedAvatar = "avatar" + (selectedPos + 1);
        data.put("profile_photo", selectedAvatar);

        apiClient.put("users/profile/", data, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                        setEditMode(false);
                        loadProfile();
                    });
                }
            }
        });
    }

    private void logout() {
        apiClient.clearToken();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setEditMode(boolean edit) {
        layoutViewProfile.setVisibility(edit ? View.GONE : View.VISIBLE);
        layoutEditProfile.setVisibility(edit ? View.VISIBLE : View.GONE);
    }
}
