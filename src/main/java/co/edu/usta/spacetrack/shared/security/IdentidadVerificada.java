package co.edu.usta.spacetrack.shared.security;

/** Datos confiables extraidos de un token de Neon Auth cuya firma ya fue validada. */
public record IdentidadVerificada(String authId, String correo, String nombre) {
}
