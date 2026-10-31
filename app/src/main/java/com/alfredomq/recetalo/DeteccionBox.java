package com.alfredomq.recetalo;

public class DeteccionBox {
    public final String clase;
    public final float confianza;
    public final float cx, cy, w, h;

    public DeteccionBox(String clase, float confianza, float cx, float cy, float w, float h) {
        this.clase = clase;
        this.confianza = confianza;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
    }
}