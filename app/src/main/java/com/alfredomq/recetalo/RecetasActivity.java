package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alfredomq.recetalo.databinding.ActivityRecetasBinding;

import java.util.ArrayList;
import java.util.List;

public class RecetasActivity extends AppCompatActivity {

    public static final String EXTRA_INGREDIENTES = "ingredientes";

    private ActivityRecetasBinding binding;
    private ArrayList<String> ingredientes;
    private final List<Receta> recetas = new ArrayList<>();
    private RecetasAdapter adapter;
    private boolean cargando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecetasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ingredientes = getIntent().getStringArrayListExtra(EXTRA_INGREDIENTES);
        if (ingredientes == null) ingredientes = new ArrayList<>();
        binding.tvIngredientes.setText(getString(R.string.recetas_con, String.join(", ", ingredientes)));

        adapter = new RecetasAdapter(recetas, this::abrirDetalle);
        binding.rvRecetas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecetas.setAdapter(adapter);

        binding.btnAtras.setOnClickListener(v -> finish());
        binding.btnReintentar.setOnClickListener(v -> buscarRecetas());
        binding.btnOtrasRecetas.setOnClickListener(v -> buscarRecetas());

        buscarRecetas();
    }

    private void buscarRecetas() {
        if (cargando || ingredientes.isEmpty()) return;
        cargando = true;
        mostrarEstado(Estado.CARGANDO);

        RecetasAPI.obtenerRecetas(ingredientes, new RecetasAPI.CallbackRecetas() {
            @Override
            public void onExito(List<Receta> nuevas) {
                runOnUiThread(() -> {
                    cargando = false;
                    if (isFinishing() || isDestroyed()) return;
                    recetas.clear();
                    recetas.addAll(nuevas);
                    adapter.notifyDataSetChanged();
                    binding.rvRecetas.scrollToPosition(0);
                    mostrarEstado(Estado.LISTA);
                });
            }

            @Override
            public void onError(RecetasAPI.TipoError tipo) {
                runOnUiThread(() -> {
                    cargando = false;
                    if (isFinishing() || isDestroyed()) return;
                    binding.tvError.setText(tipo == RecetasAPI.TipoError.SIN_CONEXION
                            ? R.string.recetas_error_red
                            : R.string.recetas_error_ocupado);
                    mostrarEstado(Estado.ERROR);
                });
            }
        });
    }

    private enum Estado { CARGANDO, LISTA, ERROR }

    private void mostrarEstado(Estado estado) {
        binding.layoutCargando.setVisibility(estado == Estado.CARGANDO ? View.VISIBLE : View.GONE);
        binding.rvRecetas.setVisibility(estado == Estado.LISTA ? View.VISIBLE : View.GONE);
        binding.btnOtrasRecetas.setVisibility(estado == Estado.LISTA ? View.VISIBLE : View.GONE);
        binding.layoutError.setVisibility(estado == Estado.ERROR ? View.VISIBLE : View.GONE);
    }

    private void abrirDetalle(Receta receta) {
        Intent intent = new Intent(this, RecetaDetalleActivity.class);
        intent.putExtra(RecetaDetalleActivity.EXTRA_RECETA, receta);
        startActivity(intent);
    }
}
