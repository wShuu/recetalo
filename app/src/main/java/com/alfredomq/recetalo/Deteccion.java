package com.alfredomq.recetalo;

public class Deteccion {
    public final String clase;
    public final float confianza;

    public Deteccion(String clase, float confianza) {
        this.clase = clase;
        this.confianza = confianza;
    }
}