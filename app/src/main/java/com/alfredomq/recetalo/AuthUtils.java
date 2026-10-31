package com.alfredomq.recetalo;

import android.content.Context;
import android.util.Patterns;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public final class AuthUtils {

    private AuthUtils() { }

    public static String texto(TextInputEditText campo) {
        CharSequence t = campo.getText();
        return t == null ? "" : t.toString().trim();
    }

    public static boolean correoValido(String correo) {
        return !correo.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(correo).matches();
    }

    public static String mensajeDeError(Context context, Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return context.getString(R.string.error_sin_conexion);
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return context.getString(R.string.error_muchos_intentos);
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return context.getString(R.string.error_cuenta_existe);
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return context.getString(R.string.error_contrasena_debil);
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            String codigo = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(codigo)) {
                return context.getString(R.string.error_cuenta_deshabilitada);
            }
            return context.getString(R.string.error_credenciales);
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String codigo = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(codigo)) {
                return context.getString(R.string.error_correo_invalido);
            }
            return context.getString(R.string.error_credenciales);
        }
        return context.getString(R.string.error_general);
    }
}
