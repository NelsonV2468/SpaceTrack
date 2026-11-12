package co.edu.usta.spacetrack.shared.security;

public interface VerificadorIdentidad {

    /**
     * Valida la firma y vigencia del token y devuelve la identidad que contiene.
     *
     * @throws TokenInvalidoException si el token no es confiable.
     */
    IdentidadVerificada verificar(String token);

    class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String mensaje) {
            super(mensaje);
        }

        public TokenInvalidoException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
