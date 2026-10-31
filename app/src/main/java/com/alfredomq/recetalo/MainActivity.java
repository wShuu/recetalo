package com.alfredomq.recetalo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alfredomq.recetalo.databinding.ActivityMainBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SALUDO = "saludo";

    private static final String PREFS = "recetalo_prefs";
    private static final String KEY_PERMISO_PEDIDO = "permiso_camara_pedido";
    private static final int LADO_MAXIMO_FOTO = 1280;

    private ActivityMainBinding binding;
    private Detector detector;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;

    private boolean camaraIniciada = false;
    private boolean procesando = false;
    private boolean usuarioDijoAhoraNo = false;
    private boolean solicitudEnCurso = false;

    private final ListaIngredientes lista = ListaIngredientes.get();

    private final ActivityResultLauncher<String> lanzadorPermiso =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                solicitudEnCurso = false;
                getPrefs().edit().putBoolean(KEY_PERMISO_PEDIDO, true).apply();
                if (concedido) {
                    mostrarCamara();
                } else {
                    mostrarPantallaPermiso();
                }
            });

    private final ActivityResultLauncher<String[]> lanzadorGaleria =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),
                    this::analizarImagenesDeGaleria);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            detector = new Detector(this);
        } catch (IOException e) {
            e.printStackTrace();
            detector = null;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.btnCapturar.setOnClickListener(v -> capturarYDetectar());
        binding.btnGaleria.setOnClickListener(v -> abrirGaleria());
        binding.btnIrIngredientes.setOnClickListener(v -> abrirMisIngredientes());
        binding.btnCuenta.setOnClickListener(v -> mostrarCuenta());

        String saludo = getIntent().getStringExtra(EXTRA_SALUDO);
        if (saludo != null && !saludo.isEmpty() && savedInstanceState == null) {
            mensaje(getString(R.string.login_bienvenida, saludo));
        }
        binding.ivResultado.setOnClickListener(v -> ocultarResultado());

        binding.btnPermitirAcceso.setOnClickListener(v -> solicitarPermiso());
        binding.btnActivarCamara.setOnClickListener(v -> {
            usuarioDijoAhoraNo = false;
            solicitarPermiso();
        });
        binding.btnAhoraNo.setOnClickListener(v -> {
            usuarioDijoAhoraNo = true;
            mostrarPantallaPermiso();
        });

        if (tienePermiso()) {
            mostrarCamara();
        } else {
            mostrarPantallaPermiso();
            if (!getPrefs().getBoolean(KEY_PERMISO_PEDIDO, false)) {
                solicitarPermiso();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescarChips();
        if (tienePermiso() && !camaraIniciada) {
            mostrarCamara();
        } else if (!tienePermiso() && !solicitudEnCurso) {
            mostrarPantallaPermiso();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private boolean permisoBloqueado() {
        return !solicitudEnCurso
                && getPrefs().getBoolean(KEY_PERMISO_PEDIDO, false)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
    }

    private void solicitarPermiso() {
        if (tienePermiso()) {
            mostrarCamara();
            return;
        }
        if (permisoBloqueado()) {
            abrirAjustesDeLaApp();
            return;
        }
        solicitudEnCurso = true;
        lanzadorPermiso.launch(Manifest.permission.CAMERA);
    }

    private void abrirAjustesDeLaApp() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void mostrarPantallaPermiso() {
        if (usuarioDijoAhoraNo) {
            binding.layoutPermiso.setVisibility(View.GONE);
            binding.layoutSinPermiso.setVisibility(View.VISIBLE);
            binding.previewView.setVisibility(View.INVISIBLE);
            return;
        }
        boolean bloqueado = permisoBloqueado();
        binding.tvPermisoBloqueado.setVisibility(bloqueado ? View.VISIBLE : View.GONE);
        binding.btnPermitirAcceso.setText(bloqueado ? "Abrir ajustes" : "Permitir acceso");
        binding.layoutPermiso.setVisibility(View.VISIBLE);
    }

    private void mostrarCamara() {
        usuarioDijoAhoraNo = false;
        binding.layoutPermiso.setVisibility(View.GONE);
        binding.layoutSinPermiso.setVisibility(View.GONE);
        binding.previewView.setVisibility(View.VISIBLE);
        if (!camaraIniciada) iniciarCamara();
    }

    private void iniciarCamara() {
        camaraIniciada = true;
        ListenableFuture<ProcessCameraProvider> proveedorFuturo = ProcessCameraProvider.getInstance(this);
        proveedorFuturo.addListener(() -> {
            try {
                ProcessCameraProvider proveedor = proveedorFuturo.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                proveedor.unbindAll();
                proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception e) {
                e.printStackTrace();
                camaraIniciada = false;
                mensaje("No se pudo iniciar la cámara. Intenta de nuevo.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturarYDetectar() {
        if (procesando) return;
        if (!tienePermiso()) {
            usuarioDijoAhoraNo = false;
            solicitarPermiso();
            return;
        }
        if (imageCapture == null) {
            mensaje("La cámara se está iniciando, espera un momento.");
            return;
        }
        if (detector == null) {
            mensaje("No se pudo cargar el modelo de reconocimiento.");
            return;
        }

        ocultarResultado();
        setProcesando(true);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap foto;
                try {
                    foto = imageProxyABitmap(image);
                } finally {
                    image.close();
                }
                if (foto == null) {
                    runOnUiThread(() -> {
                        setProcesando(false);
                        mensaje("No se pudo leer la foto. Intenta de nuevo.");
                    });
                    return;
                }

                LinkedHashSet<String> nombres = new LinkedHashSet<>();
                Bitmap fotoConCajas;
                try {
                    fotoConCajas = reconocer(foto, nombres);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        setProcesando(false);
                        mensaje("Ocurrió un error al reconocer la imagen.");
                    });
                    return;
                }
                runOnUiThread(() -> mostrarResultado(fotoConCajas, new ArrayList<>(nombres)));
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
                runOnUiThread(() -> {
                    setProcesando(false);
                    mensaje("No se pudo tomar la foto. Intenta de nuevo.");
                });
            }
        });
    }

    private Bitmap reconocer(Bitmap imagen, LinkedHashSet<String> nombres) {
        List<DeteccionBox> cajas = detector.detectarConCajas(imagen);
        for (DeteccionBox c : cajas) nombres.add(NombresIngredientes.enEspanol(c.clase));
        return dibujarCajas(imagen, cajas);
    }

    private void abrirGaleria() {
        if (procesando) return;
        if (detector == null) {
            mensaje("No se pudo cargar el modelo de reconocimiento.");
            return;
        }
        try {
            lanzadorGaleria.launch(new String[]{"image/png", "image/jpeg"});
        } catch (Exception e) {
            mensaje("No se encontró una app para elegir imágenes.");
        }
    }

    private void analizarImagenesDeGaleria(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        ocultarResultado();
        setProcesando(true);

        cameraExecutor.execute(() -> {
            LinkedHashSet<String> nombres = new LinkedHashSet<>();
            Bitmap ultimaConCajas = null;
            int fallidas = 0;

            for (Uri uri : uris) {
                Bitmap imagen = cargarImagen(uri);
                if (imagen == null) {
                    fallidas++;
                    continue;
                }
                try {
                    ultimaConCajas = reconocer(imagen, nombres);
                } catch (Exception e) {
                    e.printStackTrace();
                    fallidas++;
                }
            }

            final Bitmap resultado = ultimaConCajas;
            final int errores = fallidas;
            runOnUiThread(() -> {
                if (resultado == null) {
                    setProcesando(false);
                    mensaje(uris.size() == 1
                            ? "No se pudo abrir la imagen. Usa un archivo PNG o JPG."
                            : "No se pudo abrir ninguna de las imágenes. Usa archivos PNG o JPG.");
                    return;
                }
                mostrarResultado(resultado, new ArrayList<>(nombres));
                if (errores > 0) {
                    mensaje(errores + " imagen(es) no se pudieron abrir. Las demás sí se analizaron.");
                }
            });
        });
    }

    private Bitmap cargarImagen(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source fuente = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(fuente, (decoder, info, src) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int lado = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    int escala = 1;
                    while (lado / (escala * 2) >= LADO_MAXIMO_FOTO) escala *= 2;
                    decoder.setTargetSampleSize(escala);
                });
            } else {
                BitmapFactory.Options limites = new BitmapFactory.Options();
                limites.inJustDecodeBounds = true;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(in, null, limites);
                }
                int escala = 1;
                int lado = Math.max(limites.outWidth, limites.outHeight);
                while (lado / (escala * 2) >= LADO_MAXIMO_FOTO) escala *= 2;
                BitmapFactory.Options opciones = new BitmapFactory.Options();
                opciones.inSampleSize = escala;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(in, null, opciones);
                }
            }
            if (bitmap == null) return null;

            if (bitmap.hasAlpha()) {
                Bitmap conFondo = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(conFondo);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(bitmap, 0, 0, null);
                bitmap = conFondo;
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void mostrarResultado(Bitmap fotoConCajas, List<String> nombres) {
        if (isFinishing() || isDestroyed()) return;
        setProcesando(false);

        binding.layoutSinPermiso.setVisibility(View.GONE);
        binding.ivResultado.setImageBitmap(fotoConCajas);
        binding.ivResultado.setVisibility(View.VISIBLE);
        binding.tvIndicacion.setText(tienePermiso()
                ? "Toca la imagen para volver a la cámara"
                : "Toca la imagen para cerrarla");

        if (nombres.isEmpty()) {
            Snackbar.make(binding.getRoot(),
                            "No reconocí ningún producto. Prueba con otra imagen o agrégalo a mano.",
                            Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.btnCapturar)
                    .setAction("Agregar", v -> abrirMisIngredientes())
                    .show();
            return;
        }

        List<String> nuevos = new ArrayList<>();
        List<String> repetidos = new ArrayList<>();
        for (String n : nombres) {
            if (lista.agregar(n)) nuevos.add(n); else repetidos.add(n);
        }
        refrescarChips();

        StringBuilder texto = new StringBuilder();
        if (!nuevos.isEmpty()) texto.append("Agregado: ").append(String.join(", ", nuevos));
        if (!repetidos.isEmpty()) {
            if (texto.length() > 0) texto.append(" · ");
            texto.append("Ya estaba en tu lista: ").append(String.join(", ", repetidos));
        }
        mensaje(texto.toString());
    }

    private void ocultarResultado() {
        binding.ivResultado.setVisibility(View.GONE);
        binding.ivResultado.setImageDrawable(null);
        binding.tvIndicacion.setText("Apunta al producto y toca el botón para capturarlo, o sube una imagen");
        if (!tienePermiso() && usuarioDijoAhoraNo) {
            binding.layoutSinPermiso.setVisibility(View.VISIBLE);
        }
    }

    private void setProcesando(boolean valor) {
        procesando = valor;
        binding.btnCapturar.setEnabled(!valor);
        binding.btnGaleria.setEnabled(!valor);
        binding.progresoDeteccion.setVisibility(valor ? View.VISIBLE : View.GONE);
    }

    private void refrescarChips() {
        binding.chipsIngredientes.removeAllViews();
        for (String nombre : lista.getItems()) {
            Chip chip = new Chip(this);
            chip.setText(nombre);
            chip.setChipBackgroundColorResource(R.color.verde_claro);
            chip.setTextColor(ContextCompat.getColor(this, R.color.verde_oscuro));
            chip.setChipStrokeWidth(0f);
            chip.setCloseIconVisible(true);
            chip.setCloseIconResource(R.drawable.ic_cerrar);
            chip.setCloseIconTintResource(R.color.verde_oscuro);
            chip.setCloseIconContentDescription("Quitar " + nombre);
            chip.setOnCloseIconClickListener(v -> quitarDesdeChip(nombre));
            binding.chipsIngredientes.addView(chip);
        }
        int total = lista.tamano();
        binding.tvContador.setText(String.format(Locale.getDefault(), "Mis ingredientes (%d)", total));
        binding.tvSinIngredientes.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        binding.scrollChips.post(() -> binding.scrollChips.fullScroll(View.FOCUS_RIGHT));
    }

    private void quitarDesdeChip(String nombre) {
        int posicion = lista.indiceDe(nombre);
        if (posicion < 0) return;
        lista.quitar(posicion);
        refrescarChips();
        Snackbar.make(binding.getRoot(), "Se quitó " + nombre, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnCapturar)
                .setAction("Deshacer", v -> {
                    lista.insertar(posicion, nombre);
                    refrescarChips();
                })
                .show();
    }

    private void mostrarCuenta() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.invitado_titulo)
                    .setMessage(R.string.invitado_mensaje)
                    .setNegativeButton(R.string.invitado_seguir, null)
                    .setPositiveButton(R.string.invitado_ir_login, (d, w) -> irAlLogin())
                    .show();
            return;
        }

        String nombre = usuario.getDisplayName();
        String correo = usuario.getEmail() != null ? usuario.getEmail() : "";
        String detalle = (nombre != null && !nombre.trim().isEmpty())
                ? nombre.trim() + "\n" + correo
                : correo;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cuenta_titulo)
                .setMessage(detalle)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cuenta_cerrar_sesion, (d, w) -> confirmarCerrarSesion())
                .show();
    }

    private void confirmarCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cuenta_cerrar_sesion)
                .setMessage(R.string.cuenta_cerrar_confirmar)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cuenta_cerrar_sesion, (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    lista.vaciar();
                    irAlLogin();
                })
                .show();
    }

    private void irAlLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void abrirMisIngredientes() {
        startActivity(new Intent(this, IngredientesActivity.class));
    }

    private void mensaje(String texto) {
        Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnCapturar)
                .show();
    }

    private Bitmap imageProxyABitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, limites);

        int escala = 1;
        int ladoMayor = Math.max(limites.outWidth, limites.outHeight);
        while (ladoMayor / (escala * 2) >= LADO_MAXIMO_FOTO) escala *= 2;

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = escala;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones);
        if (bitmap == null) return null;

        int rotacion = image.getImageInfo().getRotationDegrees();
        if (rotacion != 0) {
            Matrix matriz = new Matrix();
            matriz.postRotate(rotacion);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true);
        }
        return bitmap;
    }

    private Bitmap dibujarCajas(Bitmap original, List<DeteccionBox> cajas) {
        Bitmap resultado = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultado);

        float grosor = Math.max(6f, original.getWidth() / 110f);
        float tamTexto = Math.max(28f, original.getWidth() / 24f);

        Paint paintBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorde.setColor(Color.WHITE);
        paintBorde.setStyle(Paint.Style.STROKE);
        paintBorde.setStrokeWidth(grosor * 1.8f);

        Paint paintCaja = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCaja.setColor(ContextCompat.getColor(this, R.color.verde_oscuro));
        paintCaja.setStyle(Paint.Style.STROKE);
        paintCaja.setStrokeWidth(grosor);

        Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTexto.setColor(Color.WHITE);
        paintTexto.setTextSize(tamTexto);
        paintTexto.setFakeBoldText(true);

        Paint paintFondoTexto = new Paint();
        paintFondoTexto.setColor(ContextCompat.getColor(this, R.color.verde_oscuro));

        float escalaX = original.getWidth() / 640f;
        float escalaY = original.getHeight() / 640f;

        for (DeteccionBox caja : cajas) {
            float left = (caja.cx - caja.w / 2) * escalaX;
            float top = (caja.cy - caja.h / 2) * escalaY;
            float right = (caja.cx + caja.w / 2) * escalaX;
            float bottom = (caja.cy + caja.h / 2) * escalaY;

            canvas.drawRect(left, top, right, bottom, paintBorde);
            canvas.drawRect(left, top, right, bottom, paintCaja);

            String etiqueta = NombresIngredientes.enEspanol(caja.clase) + " "
                    + String.format(Locale.getDefault(), "%.0f%%", caja.confianza * 100);
            float anchoTexto = paintTexto.measureText(etiqueta);
            float altoEtiqueta = tamTexto * 1.3f;
            float topEtiqueta = Math.max(0, top - altoEtiqueta);
            canvas.drawRect(left, topEtiqueta, left + anchoTexto + 16, topEtiqueta + altoEtiqueta, paintFondoTexto);
            canvas.drawText(etiqueta, left + 8, topEtiqueta + altoEtiqueta - tamTexto * 0.3f, paintTexto);
        }
        return resultado;
    }
}
