package co.edu.usta.spacetrack.shared.exception;

import co.edu.usta.spacetrack.rutas.controller.RutaRestController;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.service.RutaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ManejadorGlobalDeExcepcionesTest {

    @Mock private RutaService rutaService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RutaRestController(rutaService))
                .setControllerAdvice(new ManejadorGlobalDeExcepciones())
                .build();
    }

    @Test
    void rechazaUnEstadoInvalidoEnElFiltro() throws Exception {
        mockMvc.perform(get("/api/rutas").param("estado", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("Estado de ruta invalido: XYZ. Use PENDIENTE, EN_CURSO o FINALIZADA"));

        verifyNoInteractions(rutaService);
    }

    @Test
    void aceptaUnEstadoValidoEnElFiltro() throws Exception {
        when(rutaService.listar(EstadoRuta.EN_CURSO)).thenReturn(List.of());

        mockMvc.perform(get("/api/rutas").param("estado", "EN_CURSO"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(rutaService).listar(EstadoRuta.EN_CURSO);
    }

    @Test
    void otroParametroMalTipadoNoSeReportaComoEstado() throws Exception {
        mockMvc.perform(get("/api/rutas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parametro id tiene un valor invalido"));

        verifyNoInteractions(rutaService);
    }

    @Test
    void rechazaUnCuerpoJsonMalFormado() throws Exception {
        mockMvc.perform(post("/api/rutas").contentType(MediaType.APPLICATION_JSON).content("{ mal formado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la peticion no es valido"));

        verifyNoInteractions(rutaService);
    }

    @Test
    void unFalloInesperadoRespondeSinDetallesInternos() throws Exception {
        when(rutaService.listar(null)).thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/api/rutas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Ocurrio un error interno. Intente de nuevo mas tarde"));
    }

    @Test
    void unErrorDeClienteDeSpringConservaSuCodigo() throws Exception {
        when(rutaService.listar(null)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "detalle interno"));

        mockMvc.perform(get("/api/rutas"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403))
                .andExpect(jsonPath("$.mensaje").value("La solicitud no se pudo procesar"));
    }
}
