package co.edu.usta.spacetrack.shared.security;

import co.edu.usta.spacetrack.usuarios.domain.Rol;
import co.edu.usta.spacetrack.usuarios.domain.Usuario;
import co.edu.usta.spacetrack.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba simulada de la seguridad completa (Spring + H2 en memoria, sin Neon real):
 * accesos anonimos, permisos por rol, CSRF, cambio de rol en caliente y fuga de datos.
 */
@SpringBootTest(properties = {
        "spacetrack.seguridad.habilitada=true",
        "NEON_AUTH_BASE_URL=https://ep-prueba.neonauth.us-east-1.aws.neon.tech/neondb/auth",
        "NEON_AUTH_JWKS_URL=https://ep-prueba.neonauth.us-east-1.aws.neon.tech/neondb/auth/.well-known/jwks.json",
        "spacetrack.seguridad.correos-admin=jefe@spacetrack.co",
        "spacetrack.simulacion.intervalo-segundos=3600"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadIntegracionTest {

    private static final String ZONA = "{\"codigo\":\"Z-%s\",\"nombre\":\"Zona %s\",\"localidad\":\"Suba\",\"activa\":true}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @MockitoBean
    private VerificadorIdentidad verificador;

    @BeforeEach
    void limpiar() {
        usuarios.deleteAll();
    }

    // ---------- Anonimos ----------

    @Test
    void laApiNoEntregaDatosSinSesion() throws Exception {
        mvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(content().string(not(containsString("razonSocial"))));
    }

    @Test
    void lasPaginasProtegidasRedirigenAlLogin() throws Exception {
        mvc.perform(get("/panel.html")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html"));
        mvc.perform(get("/clientes")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login.html"));
        mvc.perform(get("/h2-console")).andExpect(status().is3xxRedirection());
    }

    @Test
    void elLoginYLaSaludSonPublicosSinDetallesInternos() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
        mvc.perform(get("/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguridad").value(true))
                .andExpect(content().string(not(containsString("jwks"))));
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("components"))));
        mvc.perform(get("/actuator/env")).andExpect(status().is(not(200)));
    }

    @Test
    void seEnvianCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/login.html"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("https://ep-prueba.neonauth.us-east-1.aws.neon.tech")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ---------- Intercambio de token por sesion ----------

    @Test
    void sinTokenOTokenInvalidoNoHaySesion() throws Exception {
        mvc.perform(post("/auth/session").with(csrf())).andExpect(status().isUnauthorized());

        when(verificador.verificar(anyString())).thenThrow(new VerificadorIdentidad.TokenInvalidoException("firma"));
        mvc.perform(post("/auth/session").with(csrf()).header("Authorization", "Bearer falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elIntercambioExigeTokenCsrf() throws Exception {
        when(verificador.verificar("ok")).thenReturn(new IdentidadVerificada("u-1", "jefe@spacetrack.co", "Jefe"));
        mvc.perform(post("/auth/session").header("Authorization", "Bearer ok")).andExpect(status().isForbidden());
    }

    @Test
    void unaCuentaNuevaQuedaPendienteYNoObtieneSesion() throws Exception {
        when(verificador.verificar("nuevo")).thenReturn(new IdentidadVerificada("u-2", "intruso@correo.com", "X"));

        MvcResult resultado = mvc.perform(post("/auth/session").with(csrf()).header("Authorization", "Bearer nuevo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("aprobacion")))
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        if (sesion != null) {
            mvc.perform(get("/api/clientes").session(sesion)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void flujoCompletoAdminIniciaSesionYUsaLaApi() throws Exception {
        when(verificador.verificar("jefe")).thenReturn(new IdentidadVerificada("u-3", "jefe@spacetrack.co", "Jefe"));

        MvcResult login = mvc.perform(post("/auth/session").with(csrf()).header("Authorization", "Bearer jefe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn();
        MockHttpSession sesion = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/auth/me").session(sesion)).andExpect(status().isOk()).andExpect(jsonPath("$.correo").value("jefe@spacetrack.co"));
        mvc.perform(get("/api/zonas").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").session(sesion)).andExpect(status().isOk());

        mvc.perform(post("/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/zonas").session(sesion)).andExpect(status().isUnauthorized());
    }

    // ---------- Permisos por rol ----------

    @Test
    void consultaSoloPuedeLeer() throws Exception {
        RequestPostProcessor consulta = como(Rol.CONSULTA);
        mvc.perform(get("/api/zonas").with(consulta)).andExpect(status().isOk());
        mvc.perform(post("/api/zonas").with(consulta).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(zona()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("rol")));
        mvc.perform(get("/api/usuarios").with(consulta)).andExpect(status().isForbidden());
    }

    @Test
    void operadorCreaPeroNoEliminaNiGestionaUsuarios() throws Exception {
        RequestPostProcessor operador = como(Rol.OPERADOR);
        mvc.perform(post("/api/zonas").with(operador).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(zona()))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/zonas/999").with(operador).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/clientes/1/eliminar").with(operador).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/1/rol").with(operador).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMIN\"}")).andExpect(status().isForbidden());
    }

    @Test
    void sinCsrfNoSeModificanDatosAunqueHayaSesion() throws Exception {
        mvc.perform(post("/api/zonas").with(como(Rol.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(zona()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("token de seguridad")));
    }

    @Test
    void adminPuedeEliminarYCambiarRoles() throws Exception {
        RequestPostProcessor admin = como(Rol.ADMIN);
        Usuario pendiente = usuarios.save(new Usuario("p-" + UUID.randomUUID(), "pend@correo.com", "Pend", Rol.PENDIENTE));

        mvc.perform(delete("/api/zonas/999").with(admin).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(patch("/api/usuarios/" + pendiente.getId() + "/rol").with(admin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"CONSULTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CONSULTA"));
    }

    @Test
    void unCambioDeRolSeAplicaDeInmediato() throws Exception {
        Usuario usuario = usuarios.save(new Usuario("c-" + UUID.randomUUID(), "op@correo.com", "Op", Rol.OPERADOR));
        UsuarioSesion sesionVieja = UsuarioSesion.desde(usuario);

        usuario.setRol(Rol.CONSULTA);
        usuarios.save(usuario);
        mvc.perform(post("/api/zonas").with(authentication(sesionVieja.comoAutenticacion())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(zona())).andExpect(status().isForbidden());

        usuario.setRol(Rol.PENDIENTE);
        usuarios.save(usuario);
        mvc.perform(get("/api/zonas").with(authentication(sesionVieja.comoAutenticacion()))).andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor como(Rol rol) {
        Usuario usuario = usuarios.save(new Usuario("t-" + UUID.randomUUID(), rol.name().toLowerCase() + UUID.randomUUID() + "@correo.com", rol.name(), rol));
        return authentication(UsuarioSesion.desde(usuario).comoAutenticacion());
    }

    private static String zona() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        return ZONA.formatted(sufijo, sufijo);
    }
}
