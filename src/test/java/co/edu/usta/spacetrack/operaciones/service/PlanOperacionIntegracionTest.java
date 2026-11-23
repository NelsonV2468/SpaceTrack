package co.edu.usta.spacetrack.operaciones.service;

import co.edu.usta.spacetrack.clientes.dto.ClienteRequest;
import co.edu.usta.spacetrack.clientes.service.ClienteService;
import co.edu.usta.spacetrack.necesidades.domain.DiaSemana;
import co.edu.usta.spacetrack.necesidades.service.NecesidadClienteService;
import co.edu.usta.spacetrack.operaciones.domain.DiasOperacion;
import co.edu.usta.spacetrack.operaciones.domain.EstadoOperacion;
import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionRequest;
import co.edu.usta.spacetrack.operaciones.dto.PlanOperacionResponse;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Operacion y necesidad en un solo formulario: cada dato se pide una vez y todo se guarda en un paso. */
@SpringBootTest(properties = "spacetrack.simulacion.intervalo-segundos=3600")
@ActiveProfiles("test")
@Transactional
class PlanOperacionIntegracionTest {

    private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Bogota"));
    private static final Set<DiaSemana> LUNES_A_VIERNES = EnumSet.range(DiaSemana.LUNES, DiaSemana.VIERNES);

    @Autowired PlanOperacionService planService;
    @Autowired NecesidadClienteService necesidadService;
    @Autowired ClienteService clienteService;

    @Test
    void creaElContratoYSuNecesidadEnUnSoloPaso() {
        PlanOperacionResponse plan = planService.crear(solicitud(nuevoCliente(), 3, LUNES_A_VIERNES, EstadoOperacion.ACTIVA));

        assertThat(plan.diasOperacionTexto()).isEqualTo("Lunes a viernes");
        assertThat(plan.numeroVehiculosRequeridos()).isEqualTo(3);
        assertThat(plan.cantidadVehiculos()).isEqualTo(3);
        assertThat(plan.diasOperacion()).containsExactlyElementsOf(LUNES_A_VIERNES);
        assertThat(plan.horaInicio()).isEqualTo(LocalTime.of(6, 0));
        assertThat(plan.necesidadId()).isNotNull();
        assertThat(necesidadService.listar(plan.id())).singleElement()
                .satisfies(n -> assertThat(n.requiereCadenaFrio()).isTrue());
    }

    @Test
    void ampliaYReduceVehiculosYDiasSinChocarConLasReglas() {
        Long cliente = nuevoCliente();
        PlanOperacionResponse plan = planService.crear(solicitud(cliente, 3, LUNES_A_VIERNES, EstadoOperacion.ACTIVA));

        PlanOperacionResponse ampliado = planService.actualizar(plan.id(),
                solicitud(cliente, 6, EnumSet.allOf(DiaSemana.class), EstadoOperacion.ACTIVA));
        assertThat(ampliado.numeroVehiculosRequeridos()).isEqualTo(6);
        assertThat(ampliado.diasOperacionTexto()).isEqualTo("Todos los dias");

        Set<DiaSemana> tresDias = EnumSet.of(DiaSemana.LUNES, DiaSemana.MIERCOLES, DiaSemana.VIERNES);
        PlanOperacionResponse reducido = planService.actualizar(plan.id(), solicitud(cliente, 2, tresDias, EstadoOperacion.PAUSADA));
        assertThat(reducido.numeroVehiculosRequeridos()).isEqualTo(2);
        assertThat(reducido.diasOperacionTexto()).isEqualTo("Lunes, miércoles y viernes");
        assertThat(reducido.estado()).isEqualTo(EstadoOperacion.PAUSADA);
        assertThat(necesidadService.listar(plan.id())).singleElement()
                .satisfies(n -> assertThat(n.cantidadVehiculos()).isEqualTo(2));
    }

    @Test
    void finalizarElContratoConservaSuNecesidad() {
        Long cliente = nuevoCliente();
        PlanOperacionResponse plan = planService.crear(solicitud(cliente, 2, LUNES_A_VIERNES, EstadoOperacion.ACTIVA));

        PlanOperacionResponse finalizado = planService.actualizar(plan.id(), solicitud(cliente, 2, LUNES_A_VIERNES, EstadoOperacion.FINALIZADA));

        assertThat(finalizado.estado()).isEqualTo(EstadoOperacion.FINALIZADA);
        assertThat(finalizado.necesidadId()).isEqualTo(plan.necesidadId());
    }

    @Test
    void eliminaElContratoConSuNecesidad() {
        PlanOperacionResponse plan = planService.crear(solicitud(nuevoCliente(), 1, LUNES_A_VIERNES, EstadoOperacion.ACTIVA));

        planService.eliminar(plan.id());

        assertThat(necesidadService.listar(plan.id())).isEmpty();
        assertThat(planService.listar()).noneMatch(p -> p.id().equals(plan.id()));
    }

    @Test
    void unaReglaRechazadaNoDejaNadaAMedias() {
        Long cliente = nuevoCliente();
        PlanOperacionResponse plan = planService.crear(solicitud(cliente, 1, LUNES_A_VIERNES, EstadoOperacion.ACTIVA));

        assertThatThrownBy(() -> planService.crear(solicitud(cliente, 1, LUNES_A_VIERNES, EstadoOperacion.ACTIVA)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("codigo de contrato");
        assertThat(planService.listar()).filteredOn(p -> p.codigoContrato().equals(plan.codigoContrato())).hasSize(1);
    }

    @Test
    void elTextoDeLosDiasSeVuelveAReconocer() {
        for (Set<DiaSemana> dias : java.util.List.of(EnumSet.of(DiaSemana.SABADO), EnumSet.of(DiaSemana.MARTES, DiaSemana.JUEVES),
                EnumSet.range(DiaSemana.MIERCOLES, DiaSemana.DOMINGO), EnumSet.of(DiaSemana.LUNES, DiaSemana.DOMINGO))) {
            assertThat(DiasOperacion.interpretar(DiasOperacion.describir(dias))).contains(dias);
        }
    }

    private PlanOperacionRequest solicitud(Long cliente, int vehiculos, Set<DiaSemana> dias, EstadoOperacion estado) {
        return new PlanOperacionRequest("CT-PLAN-" + cliente, cliente, "Distribucion de prueba", HOY, HOY.plusMonths(3),
                estado, 100, true, dias, LocalTime.of(6, 0), LocalTime.of(14, 0), "Furgon", vehiculos, 800.0, null);
    }

    private Long nuevoCliente() {
        String nit = String.valueOf(900_000_000L + Math.abs(UUID.randomUUID().getMostSignificantBits() % 99_999_999L));
        return clienteService.crear(new ClienteRequest("Cliente plan " + nit, nit, "Ana", "ana" + nit + "@plan.co",
                "6015550000", "Cl 1 # 2-3", true)).id();
    }
}
