package com.alfredomq.recetalo;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.alfredomq.recetalo.databinding.ActivitySplashBinding;

import java.io.InputStream;

public class Splash extends AppCompatActivity {

    private static final long DURACION_MS = 2500;
    private static final String RUTA_LOGO = "multimedia/logo_recetalo.png";
    private static final String RUTA_AUDIO = "multimedia/recetalo_intro.mp3";

    private ActivitySplashBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable irAlInicio = this::mostrarIniciar;

    private boolean yaNavego = false;
    private boolean seDetuvo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cargarLogo();
        reproducirSonido();
        rotarImagen();
        mostrarTextos();

        handler.postDelayed(irAlInicio, DURACION_MS);
    }

    private void cargarLogo() {
        try (InputStream entrada = getAssets().open(RUTA_LOGO)) {
            Bitmap logo = BitmapFactory.decodeStream(entrada);
            if (logo != null) binding.imageView.setImageBitmap(logo);
        } catch (Exception ignorado) {
        }
    }

    private void reproducirSonido() {
        try (AssetFileDescriptor archivo = getAssets().openFd(RUTA_AUDIO)) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(archivo.getFileDescriptor(),
                    archivo.getStartOffset(), archivo.getLength());
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> liberarSonido());
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            liberarSonido();
        }
    }

    private void rotarImagen() {
        AnimationSet animacion = new AnimationSet(false);

        AlphaAnimation aparecer = new AlphaAnimation(0f, 1f);
        aparecer.setDuration(500);

        ScaleAnimation crecer = new ScaleAnimation(
                0.6f, 1f, 0.6f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        crecer.setDuration(700);
        crecer.setInterpolator(new OvershootInterpolator());

        RotateAnimation girar = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        girar.setDuration(DURACION_MS - 500);
        girar.setInterpolator(new LinearInterpolator());

        animacion.addAnimation(aparecer);
        animacion.addAnimation(crecer);
        animacion.addAnimation(girar);
        binding.imageView.startAnimation(animacion);
    }

    private void mostrarTextos() {
        binding.tvNombre.animate().alpha(1f).setStartDelay(400).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
        binding.tvLema.animate().alpha(1f).setStartDelay(800).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void mostrarIniciar() {
        if (yaNavego || isFinishing()) return;
        yaNavego = true;
        liberarSonido();
        startActivity(new Intent(Splash.this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void liberarSonido() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignorado) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!yaNavego) {
            seDetuvo = true;
            handler.removeCallbacks(irAlInicio);
            liberarSonido();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (seDetuvo) mostrarIniciar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        liberarSonido();
    }
}
