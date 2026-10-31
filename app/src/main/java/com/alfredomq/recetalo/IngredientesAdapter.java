package com.alfredomq.recetalo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class IngredientesAdapter extends RecyclerView.Adapter<IngredientesAdapter.ViewHolder> {

    public interface OnEliminarListener {
        void onEliminar(int posicion);
    }

    private final List<String> items;
    private final OnEliminarListener listener;

    public IngredientesAdapter(List<String> items, OnEliminarListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final ImageButton quitar;

        public ViewHolder(View view) {
            super(view);
            nombre = view.findViewById(R.id.tvNombre);
            quitar = view.findViewById(R.id.btnQuitar);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingrediente, parent, false);
        ViewHolder holder = new ViewHolder(vista);

        View.OnClickListener quitar = v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onEliminar(pos);
        };
        holder.itemView.setOnClickListener(quitar);
        holder.quitar.setOnClickListener(quitar);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String nombre = items.get(position);
        holder.nombre.setText(nombre);
        holder.quitar.setContentDescription("Quitar " + nombre);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
