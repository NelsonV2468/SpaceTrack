package co.edu.usta.spacetrack.shared.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Valida los JWT de Neon Auth (EdDSA / Ed25519, expiran a los 15 minutos) contra el JWKS del proyecto.
 * Se usa la implementacion Ed25519 del JDK para no depender de librerias criptograficas adicionales.
 */
public class NeonJwtVerificador implements VerificadorIdentidad {

    // Prefijo DER de SubjectPublicKeyInfo para una clave Ed25519 (OID 1.3.101.112).
    private static final byte[] PREFIJO_ED25519 = HexFormat.of().parseHex("302a300506032b6570032100");
    private static final Duration TOLERANCIA_RELOJ = Duration.ofSeconds(60);

    private final JWKSource<SecurityContext> fuenteClaves;
    private final Set<String> emisoresPermitidos;
    private final Clock reloj;

    public NeonJwtVerificador(JWKSource<SecurityContext> fuenteClaves, Set<String> emisoresPermitidos, Clock reloj) {
        if (emisoresPermitidos == null || emisoresPermitidos.isEmpty()) {
            throw new IllegalArgumentException("Debe configurarse al menos un emisor permitido");
        }
        this.fuenteClaves = fuenteClaves;
        this.emisoresPermitidos = Set.copyOf(emisoresPermitidos);
        this.reloj = reloj;
    }

    /** Crea el verificador con el JWKS remoto de Neon Auth (con cache y limite de consultas). */
    public static NeonJwtVerificador remoto(String jwksUrl, Set<String> emisoresPermitidos) {
        try {
            JWKSource<SecurityContext> fuente = JWKSourceBuilder.create(URI.create(jwksUrl).toURL())
                    .retrying(true)
                    .build();
            return new NeonJwtVerificador(fuente, emisoresPermitidos, Clock.systemUTC());
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("NEON_AUTH_JWKS_URL no es una URL valida", e);
        }
    }

    @Override
    public IdentidadVerificada verificar(String token) {
        if (token == null || token.isBlank() || token.length() > 8192) {
            throw new TokenInvalidoException("Token ausente o con tamano invalido");
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.EdDSA.equals(jwt.getHeader().getAlgorithm())) {
                throw new TokenInvalidoException("Algoritmo de firma no permitido");
            }
            if (!firmaValida(jwt)) {
                throw new TokenInvalidoException("Firma invalida");
            }
            return identidad(jwt.getJWTClaimsSet());
        } catch (TokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenInvalidoException("Token mal formado", e);
        }
    }

    private boolean firmaValida(SignedJWT jwt) throws Exception {
        String kid = jwt.getHeader().getKeyID();
        JWKMatcher.Builder filtro = new JWKMatcher.Builder().keyType(KeyType.OKP);
        if (kid != null) filtro.keyID(kid);
        List<JWK> claves = fuenteClaves.get(new JWKSelector(filtro.build()), null);
        byte[] firma = jwt.getSignature().decode();
        byte[] contenido = jwt.getSigningInput();
        for (JWK clave : claves) {
            if (clave instanceof OctetKeyPair okp && "Ed25519".equals(okp.getCurve().getName())) {
                Signature verificador = Signature.getInstance("Ed25519");
                verificador.initVerify(clavePublica(okp));
                verificador.update(contenido);
                if (verificador.verify(firma)) return true;
            }
        }
        return false;
    }

    private PublicKey clavePublica(OctetKeyPair okp) throws Exception {
        byte[] x = okp.getX().decode();
        byte[] der = new byte[PREFIJO_ED25519.length + x.length];
        System.arraycopy(PREFIJO_ED25519, 0, der, 0, PREFIJO_ED25519.length);
        System.arraycopy(x, 0, der, PREFIJO_ED25519.length, x.length);
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der));
    }

    private IdentidadVerificada identidad(JWTClaimsSet claims) throws Exception {
        Instant ahora = reloj.instant();
        Date expira = claims.getExpirationTime();
        if (expira == null || expira.toInstant().plus(TOLERANCIA_RELOJ).isBefore(ahora)) {
            throw new TokenInvalidoException("Token expirado");
        }
        Date noAntes = claims.getNotBeforeTime();
        if (noAntes != null && noAntes.toInstant().minus(TOLERANCIA_RELOJ).isAfter(ahora)) {
            throw new TokenInvalidoException("Token aun no valido");
        }
        if (claims.getIssuer() == null || !emisoresPermitidos.contains(claims.getIssuer())) {
            throw new TokenInvalidoException("Emisor no permitido: " + claims.getIssuer());
        }
        String sub = claims.getSubject();
        String correo = claims.getStringClaim("email");
        if (sub == null || sub.isBlank() || correo == null || !correo.contains("@")) {
            throw new TokenInvalidoException("El token no identifica al usuario");
        }
        return new IdentidadVerificada(sub, correo, claims.getStringClaim("name"));
    }
}
