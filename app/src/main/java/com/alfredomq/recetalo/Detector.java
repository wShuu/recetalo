package com.alfredomq.recetalo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Detector {

    private final Interpreter interpreter;
    private final List<String> etiquetas;
    private final int inputSize = 640;

    public Detector(Context context) throws IOException {
        interpreter = new Interpreter(cargarModelo(context, "best.tflite"));
        etiquetas = cargarEtiquetas(context);
    }

    private MappedByteBuffer cargarModelo(Context context, String nombreArchivo) throws IOException {
        AssetFileDescriptor descriptor = context.getAssets().openFd(nombreArchivo);
        FileInputStream entrada = new FileInputStream(descriptor.getFileDescriptor());
        FileChannel canal = entrada.getChannel();
        long inicio = descriptor.getStartOffset();
        long longitud = descriptor.getDeclaredLength();
        return canal.map(FileChannel.MapMode.READ_ONLY, inicio, longitud);
    }

    private List<String> cargarEtiquetas(Context context) throws IOException {
        List<String> lista = new ArrayList<>();
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(context.getAssets().open("labels.txt")));
        String linea;
        while ((linea = lector.readLine()) != null) {
            lista.add(linea);
        }
        lector.close();
        return lista;
    }

    private final float umbralConfianza = 0.35f;
    private final float umbralIoU = 0.45f;

    public List<DeteccionBox> detectarConCajas(Bitmap bitmap) {
        Bitmap bitmapRedimensionado = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);
        ByteBuffer entrada = bitmapAByteBuffer(bitmapRedimensionado);

        int numClases = etiquetas.size();
        int numCajas = interpreter.getOutputTensor(0).shape()[2];

        float[][][] salida = new float[1][4 + numClases][numCajas];
        interpreter.run(entrada, salida);

        List<DeteccionBox> candidatas = extraerCandidatas(salida[0], numCajas, numClases);
        return aplicarNMS(candidatas);
    }

    private List<DeteccionBox> extraerCandidatas(float[][] salida, int numCajas, int numClases) {
        List<DeteccionBox> candidatas = new ArrayList<>();

        for (int i = 0; i < numCajas; i++) {
            int mejorClase = -1;
            float mejorConfianza = 0f;

            for (int c = 0; c < numClases; c++) {
                float confianza = salida[4 + c][i];
                if (confianza > mejorConfianza) {
                    mejorConfianza = confianza;
                    mejorClase = c;
                }
            }

            if (mejorConfianza > umbralConfianza && mejorClase != -1) {
                float cx = salida[0][i];
                float cy = salida[1][i];
                float w = salida[2][i];
                float h = salida[3][i];
                candidatas.add(new DeteccionBox(etiquetas.get(mejorClase), mejorConfianza, cx, cy, w, h));
            }
        }
        return candidatas;
    }

    private List<DeteccionBox> aplicarNMS(List<DeteccionBox> candidatas) {
        List<DeteccionBox> ordenadas = new ArrayList<>(candidatas);
        ordenadas.sort((a, b) -> Float.compare(b.confianza, a.confianza));

        List<DeteccionBox> resultado = new ArrayList<>();
        boolean[] eliminadas = new boolean[ordenadas.size()];

        for (int i = 0; i < ordenadas.size(); i++) {
            if (eliminadas[i]) continue;
            DeteccionBox actual = ordenadas.get(i);
            resultado.add(actual);

            for (int j = i + 1; j < ordenadas.size(); j++) {
                if (eliminadas[j]) continue;
                DeteccionBox otra = ordenadas.get(j);
                if (calcularIoU(actual, otra) > umbralIoU) {
                    eliminadas[j] = true;
                }
            }
        }
        return resultado;
    }

    private float calcularIoU(DeteccionBox a, DeteccionBox b) {
        float ax1 = a.cx - a.w / 2, ay1 = a.cy - a.h / 2, ax2 = a.cx + a.w / 2, ay2 = a.cy + a.h / 2;
        float bx1 = b.cx - b.w / 2, by1 = b.cy - b.h / 2, bx2 = b.cx + b.w / 2, by2 = b.cy + b.h / 2;

        float ix1 = Math.max(ax1, bx1), iy1 = Math.max(ay1, by1);
        float ix2 = Math.min(ax2, bx2), iy2 = Math.min(ay2, by2);

        float areaInterseccion = Math.max(0, ix2 - ix1) * Math.max(0, iy2 - iy1);
        float areaA = a.w * a.h;
        float areaB = b.w * b.h;
        float areaUnion = areaA + areaB - areaInterseccion;

        return areaUnion <= 0 ? 0 : areaInterseccion / areaUnion;
    }

    private ByteBuffer bitmapAByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixeles = new int[inputSize * inputSize];
        bitmap.getPixels(pixeles, 0, inputSize, 0, 0, inputSize, inputSize);

        for (int pixel : pixeles) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
        }
        for (int pixel : pixeles) {
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
        }
        for (int pixel : pixeles) {
            buffer.putFloat((pixel & 0xFF) / 255.0f);
        }
        buffer.rewind();
        return buffer;
    }

}