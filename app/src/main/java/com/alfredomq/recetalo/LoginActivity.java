package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivityLoginBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private boolean cargando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            irALaApp(null);
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnInvitado.setOnClickListener(v -> irALaApp(null));
        binding.btnIniciarSesion.setOnClickListener(v -> iniciarSesion());
        binding.etContrasena.setOnEditorActionListener((v, actionId, evento) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                iniciarSesion();
                return true;
            }
            return false;
        });

        binding.btnOlvide.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecuperarActivity.class);
            intent.putExtra(RecuperarActivity.EXTRA_CORREO, AuthUtils.texto(binding.etCorreo));
            startActivity(intent);
        });
        binding.btnRegistrarse.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        binding.etCorreo.setOnFocusChangeListener((v, foco) -> { if (foco) binding.tilCorreo.setError(null); });
        binding.etContrasena.setOnFocusChangeListener((v, foco) -> { if (foco) binding.tilContrasena.setError(null); });
    }

    private void iniciarSesion() {
        if (cargando) return;

        String correo = AuthUtils.texto(binding.etCorreo);
        String contrasena = binding.etContrasena.getText() == null ? "" : binding.etContrasena.getText().toString();

        boolean valido = true;
        if (correo.isEmpty()) {
            binding.tilCorreo.setError(getString(R.string.error_correo_vacio));
            valido = false;
        } else if (!AuthUtils.correoValido(correo)) {
            binding.tilCorreo.setError(getString(R.string.error_correo_invalido));
            valido = false;
        } else {
            binding.tilCorreo.setError(null);
        }
        if (contrasena.isEmpty()) {
            binding.tilContrasena.setError(getString(R.string.error_contrasena_vacia));
            valido = false;
        } else {
            binding.tilContrasena.setError(null);
        }
        if (!valido) return;

        setCargando(true);
        auth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, tarea -> {
                    setCargando(false);
                    if (tarea.isSuccessful()) {
                        FirebaseUser usuario = auth.getCurrentUser();
                        String nombre = usuario != null ? usuario.getDisplayName() : null;
                        irALaApp(nombre);
                    } else {
                        Snackbar.make(binding.getRoot(),
                                AuthUtils.mensajeDeError(this, tarea.getException()),
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void setCargando(boolean valor) {
        cargando = valor;
        binding.btnIniciarSesion.setEnabled(!valor);
        binding.btnInvitado.setEnabled(!valor);
        binding.btnRegistrarse.setEnabled(!valor);
        binding.btnIniciarSesion.setText(valor ? R.string.login_boton_cargando : R.string.login_boton);
    }

    private void irALaApp(String nombreParaSaludar) {
        Intent intent = new Intent(this, MainActivity.class);
        if (nombreParaSaludar != null && !nombreParaSaludar.trim().isEmpty()) {
            intent.putExtra(MainActivity.EXTRA_SALUDO, nombreParaSaludar.trim());
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
