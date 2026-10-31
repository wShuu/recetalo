package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivityHistorialBinding;
import com.google.firebase.auth.FirebaseAuth;

public class HistorialActivity extends AppCompatActivity {

    public static final String EXTRA_FAVORITOS = "favoritos";

    private ActivityHistorialBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistorialBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAtras.setOnClickListener(v -> finish());

        if (getIntent().getBooleanExtra(EXTRA_FAVORITOS, false)) {
            binding.toggleHistorial.check(R.id.btnTabFavoritos);
        }

        boolean esInvitado = FirebaseAuth.getInstance().getCurrentUser() == null;
        binding.layoutInvitado.setVisibility(esInvitado ? View.VISIBLE : View.GONE);
        binding.tvHistorialVacio.setVisibility(esInvitado ? View.GONE : View.VISIBLE);

        binding.btnIniciarSesionHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
