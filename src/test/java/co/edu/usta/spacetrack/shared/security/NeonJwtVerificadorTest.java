package co.edu.usta.spacetrack.shared.security;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.Base64URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Simula tokens validos y ataques comunes (firma falsa, alg none, expirado, otro emisor). */
class NeonJwtVerificadorTest {

    private static final String EMISOR = "https://auth.ejemplo.neon.tech/neondb/auth";
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");

    private KeyPair claves;
    private NeonJwtVerificador verificador;

    @BeforeEach
    void preparar() throws Exception {
        claves = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        verificador = verificadorCon(claves, "kid-1");
    }

    @Test
    void aceptaUnTokenValidoYExtraeLaIdentidad() throws Exception {
        IdentidadVerificada identidad = verificador.verificar(token(claves.getPrivate(), "EdDSA", "kid-1", claims(EMISOR, AHORA.plusSeconds(600))));

        assertThat(identidad.authId()).isEqualTo("user-123");
        assertThat(identidad.correo()).isEqualTo("ana@ejemplo.com");
        assertThat(identidad.nombre()).isEqualTo("Ana");
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() throws Exception {
        KeyPair atacante = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String falso = token(atacante.getPrivate(), "EdDSA", "kid-1", claims(EMISOR, AHORA.plusSeconds(600)));

        assertThatThrownBy(() -> verificador.verificar(falso)).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenConElContenidoAlterado() throws Exception {
        String valido = token(claves.getPrivate(), "EdDSA", "kid-1", claims(EMISOR, AHORA.plusSeconds(600)));
        String[] partes = valido.split("\\.");
        String alterado = partes[0] + "." + b64(claims(EMISOR, AHORA.plusSeconds(600)).replace("ana@", "admin@")) + "." + partes[2];

        assertThatThrownBy(() -> verificador.verificar(alterado)).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
    }

    @Test
    void rechazaElAlgoritmoNone() {
        String sinFirma = b64("{\"alg\":\"none\"}") + "." + b64(claims(EMISOR, AHORA.plusSeconds(600))) + ".";

        assertThatThrownBy(() -> verificador.verificar(sinFirma)).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
    }

    @Test
    void rechazaUnTokenExpirado() throws Exception {
        String expirado = token(claves.getPrivate(), "EdDSA", "kid-1", claims(EMISOR, AHORA.minusSeconds(120)));

        assertThatThrownBy(() -> verificador.verificar(expirado))
                .isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class).hasMessageContaining("expirado");
    }

    @Test
    void rechazaUnTokenDeOtroEmisor() throws Exception {
        String otro = token(claves.getPrivate(), "EdDSA", "kid-1", claims("https://otro-proyecto.neon.tech/auth", AHORA.plusSeconds(600)));

        assertThatThrownBy(() -> verificador.verificar(otro))
                .isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class).hasMessageContaining("Emisor");
    }

    @Test
    void rechazaBasuraYTokensVacios() {
        assertThatThrownBy(() -> verificador.verificar(null)).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
        assertThatThrownBy(() -> verificador.verificar("")).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
        assertThatThrownBy(() -> verificador.verificar("abc.def.ghi")).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
        assertThatThrownBy(() -> verificador.verificar("x".repeat(9000))).isInstanceOf(VerificadorIdentidad.TokenInvalidoException.class);
    }

    private NeonJwtVerificador verificadorCon(KeyPair par, String kid) {
        byte[] codificada = par.getPublic().getEncoded();
        byte[] x = Arrays.copyOfRange(codificada, codificada.length - 32, codificada.length);
        OctetKeyPair jwk = new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(x)).keyID(kid).build();
        return new NeonJwtVerificador(new ImmutableJWKSet<>(new JWKSet(jwk)), Set.of(EMISOR), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static String claims(String emisor, Instant expira) {
        return "{\"sub\":\"user-123\",\"email\":\"ana@ejemplo.com\",\"name\":\"Ana\",\"iss\":\"" + emisor
                + "\",\"iat\":" + AHORA.minusSeconds(10).getEpochSecond() + ",\"exp\":" + expira.getEpochSecond() + "}";
    }

    private static String token(PrivateKey privada, String alg, String kid, String claims) throws Exception {
        String contenido = b64("{\"alg\":\"" + alg + "\",\"kid\":\"" + kid + "\",\"typ\":\"JWT\"}") + "." + b64(claims);
        Signature firma = Signature.getInstance("Ed25519");
        firma.initSign(privada);
        firma.update(contenido.getBytes(StandardCharsets.US_ASCII));
        return contenido + "." + Base64URL.encode(firma.sign());
    }

    private static String b64(String texto) {
        return Base64URL.encode(texto.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
