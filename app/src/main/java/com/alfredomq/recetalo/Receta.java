package com.alfredomq.recetalo;

import java.io.Serializable;
import java.util.ArrayList;

public class Receta implements Serializable {

    private static final long serialVersionUID = 1L;

    public String nombre = "";
    public String descripcion = "";
    public String tiempo = "";
    public String dificultad = "";
    public String porciones = "";

    public ArrayList<String> usa = new ArrayList<>();

    public ArrayList<String> falta = new ArrayList<>();

    public ArrayList<String> pasos = new ArrayList<>();
}
