package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivityRegistroBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegistroActivity extends AppCompatActivity {

    private static final int LARGO_MINIMO_CONTRASENA = 6;

    private ActivityRegistroBinding binding;
    private FirebaseAuth auth;
    private boolean cargando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        binding.btnAtras.setOnClickListener(v -> finish());
        binding.btnYaTengoCuenta.setOnClickListener(v -> finish());
        binding.btnRegistrarme.setOnClickListener(v -> registrar());
        binding.etRepetir.setOnEditorActionListener((v, actionId, evento) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registrar();
                return true;
            }
            return false;
        });
    }

    private void registrar() {
        if (cargando) return;

        String nombre = AuthUtils.texto(binding.etNombre);
        String correo = AuthUtils.texto(binding.etCorreo);
        String contrasena = binding.etContrasena.getText() == null ? "" : binding.etContrasena.getText().toString();
        String repetir = binding.etRepetir.getText() == null ? "" : binding.etRepetir.getText().toString();

        boolean valido = true;

        if (nombre.isEmpty()) {
            binding.tilNombre.setError(getString(R.string.error_nombre_vacio));
            valido = false;
        } else {
            binding.tilNombre.setError(null);
        }

        if (correo.isEmpty()) {
            binding.tilCorreo.setError(getString(R.string.error_correo_vacio));
            valido = false;
        } else if (!AuthUtils.correoValido(correo)) {
            binding.tilCorreo.setError(getString(R.string.error_correo_invalido));
            valido = false;
        } else {
            binding.tilCorreo.setError(null);
        }

        if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            binding.tilContrasena.setError(getString(R.string.error_contrasena_corta));
            valido = false;
        } else {
            binding.tilContrasena.setError(null);
        }

        if (!repetir.equals(contrasena)) {
            binding.tilRepetir.setError(getString(R.string.error_contrasenas_distintas));
            valido = false;
        } else {
            binding.tilRepetir.setError(null);
        }

        if (!valido) return;

        setCargando(true);
        auth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, tarea -> {
                    if (!tarea.isSuccessful()) {
                        setCargando(false);
                        Snackbar.make(binding.getRoot(),
                                AuthUtils.mensajeDeError(this, tarea.getException()),
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    FirebaseUser usuario = auth.getCurrentUser();
                    if (usuario == null) {
                        setCargando(false);
                        irALaApp(nombre);
                        return;
                    }
                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();
                    usuario.updateProfile(perfil).addOnCompleteListener(this, t -> {
                        setCargando(false);
                        irALaApp(nombre);
                    });
                });
    }

    private void setCargando(boolean valor) {
        cargando = valor;
        binding.btnRegistrarme.setEnabled(!valor);
        binding.btnRegistrarme.setText(valor ? R.string.registro_boton_cargando : R.string.registro_boton);
    }

    private void irALaApp(String nombre) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SALUDO, nombre);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
