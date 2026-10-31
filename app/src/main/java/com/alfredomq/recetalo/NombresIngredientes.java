package com.alfredomq.recetalo;

import java.util.HashMap;
import java.util.Map;

public final class NombresIngredientes {

    private static final Map<String, String> TRADUCCION = new HashMap<>();

    static {
        TRADUCCION.put("apple", "Manzana");
        TRADUCCION.put("banana", "Plátano");
        TRADUCCION.put("bell_pepper", "Pimiento");
        TRADUCCION.put("cabbage", "Repollo");
        TRADUCCION.put("carrot", "Zanahoria");
        TRADUCCION.put("chicken", "Pollo");
        TRADUCCION.put("chilli_pepper", "Ají");
        TRADUCCION.put("corn", "Choclo");
        TRADUCCION.put("cucumber", "Pepino");
        TRADUCCION.put("eggplant", "Berenjena");
        TRADUCCION.put("garlic", "Ajo");
        TRADUCCION.put("grape", "Uva");
        TRADUCCION.put("kiwi", "Kiwi");
        TRADUCCION.put("lemon", "Limón");
        TRADUCCION.put("lettuce", "Lechuga");
        TRADUCCION.put("mango", "Mango");
        TRADUCCION.put("meat", "Carne");
        TRADUCCION.put("onion", "Cebolla");
        TRADUCCION.put("orange", "Naranja");
        TRADUCCION.put("pescado", "Pescado");
        TRADUCCION.put("pineapple", "Piña");
        TRADUCCION.put("potato", "Papa");
        TRADUCCION.put("sweetpotato", "Camote");
        TRADUCCION.put("tomato", "Tomate");
        TRADUCCION.put("watermelon", "Sandía");
    }

    private NombresIngredientes() { }

    public static String enEspanol(String etiquetaModelo) {
        if (etiquetaModelo == null) return "";
        String clave = etiquetaModelo.trim().toLowerCase();
        String traducido = TRADUCCION.get(clave);
        if (traducido != null) return traducido;
        return ListaIngredientes.formatear(clave.replace('_', ' '));
    }
}
