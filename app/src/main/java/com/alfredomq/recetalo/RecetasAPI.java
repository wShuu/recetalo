package com.alfredomq.recetalo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecetasAPI {

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private static final String[] MODELOS = {
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite"
    };

    private static final int INTENTOS_POR_MODELO = 2;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 2000;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    public enum TipoError { SIN_CONEXION, SERVICIO_OCUPADO }

    public interface CallbackRecetas {
        void onExito(List<Receta> recetas);
        void onError(TipoError tipo);
    }

    public static void obtenerRecetas(List<String> ingredientes, CallbackRecetas callback) {
        new Thread(() -> {
            String prompt = construirPrompt(ingredientes);

            boolean ultimoFueDeRed = false;
            for (String modelo : MODELOS) {
                for (int intento = 1; intento <= INTENTOS_POR_MODELO; intento++) {
                    Resultado r = llamarGemini(modelo, prompt);
                    if (r.recetas != null) {
                        callback.onExito(r.recetas);
                        return;
                    }
                    ultimoFueDeRed = r.errorDeRed;
                    android.util.Log.w("RecetasAPI", r.error);
                    if (!r.reintentable) break;
                    dormir(ESPERA_ENTRE_INTENTOS_MS * intento);
                }
            }
            callback.onError(ultimoFueDeRed ? TipoError.SIN_CONEXION : TipoError.SERVICIO_OCUPADO);
        }).start();
    }

    private static String construirPrompt(List<String> ingredientes) {
        return "Eres un asistente de cocina para hogares de Perú.\n"
                + "Tengo estos ingredientes: " + String.join(", ", ingredientes) + ".\n"
                + "Sugiere 3 platos que pueda preparar, priorizando los que aprovechen más mis "
                + "ingredientes y, cuando sea posible, platos conocidos en Perú.\n"
                + "Asume que ya tengo sal, pimienta, aceite y agua; no los pongas como faltantes.\n\n"
                + "Responde SOLO con JSON válido, sin texto adicional, con esta estructura exacta:\n"
                + "{\"recetas\":[{"
                + "\"nombre\":\"nombre del plato\","
                + "\"descripcion\":\"una frase corta y apetitosa\","
                + "\"tiempo\":\"por ejemplo 30 min\","
                + "\"dificultad\":\"Fácil, Media o Difícil\","
                + "\"porciones\":\"por ejemplo 2 porciones\","
                + "\"usa\":[\"ingredientes de MI lista que usa, con cantidad aproximada entre paréntesis\"],"
                + "\"falta\":[\"ingredientes que me faltan comprar, con cantidad aproximada entre paréntesis\"],"
                + "\"pasos\":[\"paso breve\",\"paso breve\"]"
                + "}]}\n\n"
                + "Reglas: todo en español; entre 4 y 8 pasos por receta, cada uno de máximo dos oraciones; "
                + "no numeres los pasos ni uses asteriscos ni markdown.";
    }

    private static class Resultado {
        List<Receta> recetas;
        String error = "";
        boolean reintentable;
        boolean errorDeRed;
    }

    private static Resultado llamarGemini(String modelo, String prompt) {
        Resultado r = new Resultado();
        try {
            JSONObject parte = new JSONObject().put("text", prompt);
            JSONObject contenido = new JSONObject().put("parts", new JSONArray().put(parte));
            JSONObject cuerpoJson = new JSONObject()
                    .put("contents", new JSONArray().put(contenido))
                    .put("generationConfig", new JSONObject().put("responseMimeType", "application/json"));

            RequestBody cuerpo = RequestBody.create(
                    cuerpoJson.toString().getBytes(StandardCharsets.UTF_8),
                    MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/" + modelo
                            + ":generateContent?key=" + API_KEY)
                    .addHeader("content-type", "application/json")
                    .post(cuerpo)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String cuerpoRespuesta = response.body() != null ? response.body().string() : "{}";
                int codigo = response.code();

                if (!response.isSuccessful()) {
                    r.error = modelo + " HTTP " + codigo + ": " + extraerMensaje(cuerpoRespuesta);
                    r.reintentable = codigo == 429 || codigo >= 500;
                    return r;
                }

                String texto = new JSONObject(cuerpoRespuesta)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .optString("text", "");

                List<Receta> recetas = convertirRecetas(texto);
                if (recetas.isEmpty()) {
                    r.error = modelo + ": la respuesta no tenía recetas válidas";
                    r.reintentable = true;
                } else {
                    r.recetas = recetas;
                }
            }
        } catch (java.io.IOException e) {
            r.error = "Red: " + e.getMessage();
            r.reintentable = true;
            r.errorDeRed = true;
        } catch (Exception e) {
            r.error = modelo + ": respuesta inesperada (" + e.getMessage() + ")";
            r.reintentable = true;
        }
        return r;
    }

    static List<Receta> convertirRecetas(String texto) {
        List<Receta> lista = new ArrayList<>();
        if (texto == null) return lista;

        String limpio = texto.trim();
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```[a-zA-Z]*", "");
            int fin = limpio.lastIndexOf("```");
            if (fin >= 0) limpio = limpio.substring(0, fin);
            limpio = limpio.trim();
        }

        try {
            JSONArray arreglo;
            if (limpio.startsWith("[")) {
                arreglo = new JSONArray(limpio);
            } else {
                int inicio = limpio.indexOf('{');
                if (inicio < 0) return lista;
                arreglo = new JSONObject(limpio.substring(inicio)).optJSONArray("recetas");
            }
            if (arreglo == null) return lista;

            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject o = arreglo.optJSONObject(i);
                if (o == null) continue;
                Receta receta = new Receta();
                receta.nombre = limpiar(o.optString("nombre"));
                receta.descripcion = limpiar(o.optString("descripcion"));
                receta.tiempo = limpiar(o.optString("tiempo"));
                receta.dificultad = limpiar(o.optString("dificultad"));
                receta.porciones = limpiar(o.optString("porciones"));
                receta.usa = textos(o.optJSONArray("usa"), false);
                receta.falta = textos(o.optJSONArray("falta"), false);
                receta.pasos = textos(o.optJSONArray("pasos"), true);
                if (!receta.nombre.isEmpty() && !receta.pasos.isEmpty()) lista.add(receta);
            }
        } catch (Exception ignored) {
        }
        return lista;
    }

    private static ArrayList<String> textos(JSONArray arreglo, boolean quitarNumeracion) {
        ArrayList<String> resultado = new ArrayList<>();
        if (arreglo == null) return resultado;
        for (int i = 0; i < arreglo.length(); i++) {
            String t = limpiar(arreglo.optString(i));
            if (quitarNumeracion) t = t.replaceFirst("^(?i)(paso\\s*)?\\d+\\s*[.):-]\\s+", "");
            if (!t.isEmpty()) resultado.add(t);
        }
        return resultado;
    }

    private static String limpiar(String s) {
        if (s == null) return "";
        return s.replace("**", "").replace("*", "").trim();
    }

    private static String extraerMensaje(String cuerpo) {
        try {
            return new JSONObject(cuerpo).getJSONObject("error").optString("message", cuerpo);
        } catch (Exception e) {
            return cuerpo;
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
