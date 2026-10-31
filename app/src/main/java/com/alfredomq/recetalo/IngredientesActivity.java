package com.alfredomq.recetalo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alfredomq.recetalo.databinding.ActivityIngredientesBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

public class IngredientesActivity extends AppCompatActivity {

    private ActivityIngredientesBinding binding;
    private final ListaIngredientes lista = ListaIngredientes.get();
    private IngredientesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIngredientesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new IngredientesAdapter(lista.getItems(), this::quitar);
        binding.rvIngredientes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvIngredientes.setAdapter(adapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder destino) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direccion) {
                int pos = vh.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) quitar(pos);
            }
        }).attachToRecyclerView(binding.rvIngredientes);

        binding.btnAgregarManual.setOnClickListener(v -> agregarManual());
        binding.etIngredienteManual.setOnEditorActionListener((v, actionId, evento) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                agregarManual();
                return true;
            }
            return false;
        });
        binding.etIngredienteManual.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                binding.tilIngrediente.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnVaciarLista.setOnClickListener(v -> confirmarVaciar());

        binding.btnAtras.setOnClickListener(v -> finish());
        binding.btnSeguirEscaneando.setOnClickListener(v -> finish());
        binding.btnRecetas.setOnClickListener(v -> pedirRecetas());
        binding.btnRequisitoAdicional.setOnClickListener(v ->
                binding.cardPreferencias.setVisibility(
                        binding.cardPreferencias.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        actualizarEstado();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        actualizarEstado();
    }

    private void agregarManual() {
        CharSequence escrito = binding.etIngredienteManual.getText();
        String nombre = ListaIngredientes.formatear(escrito == null ? "" : escrito.toString());

        if (nombre.isEmpty()) {
            binding.tilIngrediente.setError("Escribe el nombre de un ingrediente");
            return;
        }
        if (lista.contiene(nombre)) {
            binding.tilIngrediente.setError("\"" + nombre + "\" ya está en tu lista");
            int existente = lista.indiceDe(nombre);
            if (existente >= 0) binding.rvIngredientes.smoothScrollToPosition(existente);
            return;
        }

        lista.agregar(nombre);
        int pos = lista.tamano() - 1;
        adapter.notifyItemInserted(pos);
        binding.rvIngredientes.scrollToPosition(pos);
        binding.etIngredienteManual.setText("");
        binding.tilIngrediente.setError(null);
        actualizarEstado();
    }

    private void quitar(int posicion) {
        if (posicion < 0 || posicion >= lista.tamano()) return;
        String nombre = lista.quitar(posicion);
        adapter.notifyItemRemoved(posicion);
        actualizarEstado();

        Snackbar.make(binding.getRoot(), "Se quitó " + nombre, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnRecetas)
                .setAction("Deshacer", v -> {
                    lista.insertar(posicion, nombre);
                    int nuevaPos = lista.indiceDe(nombre);
                    if (nuevaPos >= 0) {
                        adapter.notifyItemInserted(nuevaPos);
                        binding.rvIngredientes.scrollToPosition(nuevaPos);
                    }
                    actualizarEstado();
                })
                .show();
    }

    private void confirmarVaciar() {
        int total = lista.tamano();
        if (total == 0) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("¿Vaciar la lista?")
                .setMessage(total == 1
                        ? "Se quitará 1 ingrediente."
                        : "Se quitarán los " + total + " ingredientes.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Vaciar", (d, w) -> {
                    int cantidad = lista.tamano();
                    lista.vaciar();
                    adapter.notifyItemRangeRemoved(0, cantidad);
                    actualizarEstado();
                })
                .show();
    }

    private void pedirRecetas() {
        if (lista.estaVacia()) return;
        Intent intent = new Intent(this, RecetasActivity.class);
        intent.putStringArrayListExtra(RecetasActivity.EXTRA_INGREDIENTES, new ArrayList<>(lista.getItems()));
        startActivity(intent);
    }

    private void actualizarEstado() {
        int total = lista.tamano();
        binding.tvContador.setText(total == 1
                ? "1 ingrediente"
                : String.format(Locale.getDefault(), "%d ingredientes", total));
        binding.tvListaVacia.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        binding.btnVaciarLista.setEnabled(total > 0);
        binding.btnRecetas.setEnabled(total > 0);
    }
}
