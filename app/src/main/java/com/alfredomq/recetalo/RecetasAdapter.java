package com.alfredomq.recetalo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecetasAdapter extends RecyclerView.Adapter<RecetasAdapter.ViewHolder> {

    public interface OnRecetaListener {
        void onAbrir(Receta receta);
    }

    private final List<Receta> recetas;
    private final OnRecetaListener listener;

    public RecetasAdapter(List<Receta> recetas, OnRecetaListener listener) {
        this.recetas = recetas;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre, descripcion, tiempo, dificultad, porciones, usa, falta;

        ViewHolder(View v) {
            super(v);
            nombre = v.findViewById(R.id.tvNombre);
            descripcion = v.findViewById(R.id.tvDescripcion);
            tiempo = v.findViewById(R.id.tvTiempo);
            dificultad = v.findViewById(R.id.tvDificultad);
            porciones = v.findViewById(R.id.tvPorciones);
            usa = v.findViewById(R.id.tvUsa);
            falta = v.findViewById(R.id.tvFalta);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receta, parent, false);
        ViewHolder holder = new ViewHolder(v);
        v.setOnClickListener(x -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onAbrir(recetas.get(pos));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Receta r = recetas.get(position);

        h.nombre.setText(r.nombre);
        mostrarSiHay(h.descripcion, r.descripcion);
        mostrarSiHay(h.tiempo, r.tiempo);
        mostrarSiHay(h.dificultad, r.dificultad);
        mostrarSiHay(h.porciones, r.porciones);

        int cantidad = r.usa.size();
        h.usa.setText(h.itemView.getResources().getQuantityString(R.plurals.recetas_usa, cantidad, cantidad));

        if (r.falta.isEmpty()) {
            h.falta.setText(R.string.recetas_tienes_todo);
        } else {
            h.falta.setText(h.itemView.getContext().getString(
                    R.string.recetas_te_falta, String.join(", ", r.falta)));
        }
    }

    @Override
    public int getItemCount() {
        return recetas.size();
    }

    static void mostrarSiHay(TextView vista, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            vista.setVisibility(View.GONE);
        } else {
            vista.setText(texto.trim());
            vista.setVisibility(View.VISIBLE);
        }
    }
}
