package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.alfredomq.recetalo.databinding.ActivityRecetaDetalleBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class RecetaDetalleActivity extends AppCompatActivity {

    public static final String EXTRA_RECETA = "receta";

    private ActivityRecetaDetalleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecetaDetalleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnAtras.setOnClickListener(v -> finish());
        binding.btnFavoritoDetalle.setOnClickListener(this::mostrarSprint2);
        binding.btnCompartir.setOnClickListener(this::mostrarSprint2);
        binding.btnCopiar.setOnClickListener(this::mostrarSprint2);
        binding.btnVerHistorial.setOnClickListener(v ->
                startActivity(new Intent(this, HistorialActivity.class)));

        Receta receta = (Receta) getIntent().getSerializableExtra(EXTRA_RECETA);
        if (receta == null) {
            finish();
            return;
        }
        mostrar(receta);
    }

    private void mostrarSprint2(View v) {
        Snackbar.make(binding.getRoot(), R.string.disponible_sprint2, Snackbar.LENGTH_SHORT).show();
    }

    private void mostrar(Receta r) {
        binding.tvNombre.setText(r.nombre);
        RecetasAdapter.mostrarSiHay(binding.tvDescripcion, r.descripcion);
        RecetasAdapter.mostrarSiHay(binding.tvTiempo, r.tiempo);
        RecetasAdapter.mostrarSiHay(binding.tvDificultad, r.dificultad);
        RecetasAdapter.mostrarSiHay(binding.tvPorciones, r.porciones);

        binding.tvTituloUsa.setText(titulo(R.string.detalle_ya_tienes, r.usa.size()));
        llenarVinetas(binding.contenedorUsa, r.usa, R.color.verde_oliva);

        binding.tvTituloFalta.setText(titulo(R.string.detalle_te_falta, r.falta.size()));
        if (r.falta.isEmpty()) {
            TextView nada = new TextView(this);
            nada.setText(R.string.detalle_nada_falta);
            nada.setTextColor(ContextCompat.getColor(this, R.color.verde_oliva));
            nada.setTextSize(15);
            binding.contenedorFalta.addView(nada);
        } else {
            llenarVinetas(binding.contenedorFalta, r.falta, R.color.gris_camara);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < r.pasos.size(); i++) {
            View fila = inflater.inflate(R.layout.item_paso, binding.contenedorPasos, false);
            ((TextView) fila.findViewById(R.id.tvNumero)).setText(String.valueOf(i + 1));
            ((TextView) fila.findViewById(R.id.tvTexto)).setText(r.pasos.get(i));
            binding.contenedorPasos.addView(fila);
        }
    }

    private String titulo(int textoId, int cantidad) {
        return cantidad > 0 ? getString(textoId) + " (" + cantidad + ")" : getString(textoId);
    }

    private void llenarVinetas(LinearLayout contenedor, List<String> textos, int colorPunto) {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String t : textos) {
            View fila = inflater.inflate(R.layout.item_vineta, contenedor, false);
            fila.findViewById(R.id.punto).getBackground().mutate()
                    .setTint(ContextCompat.getColor(this, colorPunto));
            ((TextView) fila.findViewById(R.id.tvTexto)).setText(t);
            contenedor.addView(fila);
        }
    }
}
