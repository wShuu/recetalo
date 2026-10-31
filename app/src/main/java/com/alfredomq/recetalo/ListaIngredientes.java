package com.alfredomq.recetalo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ListaIngredientes {

    private static ListaIngredientes instancia;

    private final List<String> items = new ArrayList<>();

    private ListaIngredientes() { }

    public static synchronized ListaIngredientes get() {
        if (instancia == null) instancia = new ListaIngredientes();
        return instancia;
    }

    public List<String> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int tamano() {
        return items.size();
    }

    public boolean estaVacia() {
        return items.isEmpty();
    }

    public boolean agregar(String nombre) {
        String limpio = formatear(nombre);
        if (limpio.isEmpty() || contiene(limpio)) return false;
        items.add(limpio);
        return true;
    }

    public boolean contiene(String nombre) {
        String clave = clave(nombre);
        for (String existente : items) {
            if (sonIguales(clave, clave(existente))) return true;
        }
        return false;
    }

    public String quitar(int posicion) {
        return items.remove(posicion);
    }

    public void insertar(int posicion, String nombre) {
        if (contiene(nombre)) return;
        int pos = Math.max(0, Math.min(posicion, items.size()));
        items.add(pos, nombre);
    }

    public int indiceDe(String nombre) {
        String clave = clave(nombre);
        for (int i = 0; i < items.size(); i++) {
            if (sonIguales(clave, clave(items.get(i)))) return i;
        }
        return -1;
    }

    public void vaciar() {
        items.clear();
    }

    public static String formatear(String texto) {
        if (texto == null) return "";
        String s = texto.trim().replaceAll("\\s+", " ");
        if (s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    static String clave(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñ ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return s;
    }

    private static boolean sonIguales(String a, String b) {
        if (a.equals(b)) return true;
        return a.equals(b + "s") || b.equals(a + "s")
                || a.equals(b + "es") || b.equals(a + "es");
    }
}
