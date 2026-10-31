package com.alfredomq.recetalo;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivityRecuperarBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperarActivity extends AppCompatActivity {

    public static final String EXTRA_CORREO = "correo";

    private ActivityRecuperarBinding binding;
    private FirebaseAuth auth;
    private boolean cargando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecuperarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        String correoPrevio = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoPrevio != null) binding.etCorreo.setText(correoPrevio);

        binding.btnCancelar.setOnClickListener(v -> finish());
        binding.btnEnviar.setOnClickListener(v -> enviar());
        binding.etCorreo.setOnEditorActionListener((v, actionId, evento) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enviar();
                return true;
            }
            return false;
        });
    }

    private void enviar() {
        if (cargando) return;

        String correo = AuthUtils.texto(binding.etCorreo);
        if (correo.isEmpty()) {
            binding.tilCorreo.setError(getString(R.string.error_correo_vacio));
            return;
        }
        if (!AuthUtils.correoValido(correo)) {
            binding.tilCorreo.setError(getString(R.string.error_correo_invalido));
            return;
        }
        binding.tilCorreo.setError(null);

        setCargando(true);
        auth.sendPasswordResetEmail(correo).addOnCompleteListener(this, tarea -> {
            setCargando(false);
            if (tarea.isSuccessful()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.recuperar_enviado_titulo)
                        .setMessage(getString(R.string.recuperar_enviado_mensaje, correo))
                        .setCancelable(false)
                        .setPositiveButton(R.string.aceptar, (d, w) -> finish())
                        .show();
            } else {
                Snackbar.make(binding.getRoot(),
                        AuthUtils.mensajeDeError(this, tarea.getException()),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setCargando(boolean valor) {
        cargando = valor;
        binding.btnEnviar.setEnabled(!valor);
        binding.btnEnviar.setText(valor ? R.string.recuperar_boton_cargando : R.string.recuperar_boton);
    }
}
