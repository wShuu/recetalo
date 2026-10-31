package com.alfredomq.recetalo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivityVerificacionBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;

public class VerificacionActivity extends AppCompatActivity {

    private ActivityVerificacionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null && usuario.getEmail() != null) {
            binding.tvCorreoVerificacion.setText(usuario.getEmail());
        }

        binding.btnCancelarVerificacion.setOnClickListener(v -> finish());
        binding.btnVerificar.setOnClickListener(this::mostrarSprint3);
        binding.btnReenviarCodigo.setOnClickListener(this::mostrarSprint3);
    }

    private void mostrarSprint3(View v) {
        Snackbar.make(binding.getRoot(), R.string.disponible_sprint3, Snackbar.LENGTH_SHORT).show();
    }
}
