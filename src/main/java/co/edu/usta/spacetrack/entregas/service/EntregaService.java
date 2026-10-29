package co.edu.usta.spacetrack.entregas.service;

import co.edu.usta.spacetrack.entregas.domain.Entrega;
import co.edu.usta.spacetrack.entregas.dto.EntregaRequest;
import co.edu.usta.spacetrack.entregas.dto.EntregaResponse;
import co.edu.usta.spacetrack.entregas.repository.EntregaRepository;
import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import co.edu.usta.spacetrack.rutas.domain.Ruta;
import co.edu.usta.spacetrack.rutas.repository.RutaRepository;
import co.edu.usta.spacetrack.shared.exception.RecursoNoEncontradoException;
import co.edu.usta.spacetrack.shared.exception.ReglaDeNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final RutaRepository rutaRepository;

    public EntregaService(EntregaRepository entregaRepository, RutaRepository rutaRepository) {
        this.entregaRepository = entregaRepository;
        this.rutaRepository = rutaRepository;
    }

    public List<EntregaResponse> listar(Long rutaId) {
        List<Entrega> entregas = rutaId == null ? entregaRepository.findAllByOrderByIdDesc()
                : entregaRepository.findByRutaIdOrderByIdAsc(rutaId);
        return entregas.stream().map(this::aResponse).toList();
    }

    @Transactional
    public EntregaResponse registrar(EntregaRequest solicitud) {
        Ruta ruta = rutaRepository.findById(solicitud.rutaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una ruta con id " + solicitud.rutaId()));
        if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaDeNegocioException("Solo se registran entregas en rutas EN_CURSO; la ruta esta " + ruta.getEstado());
        }
        // Lo realizado nunca puede superar lo programado para la ruta.
        if (entregaRepository.countByRutaId(ruta.getId()) >= ruta.getNumeroEntregasProgramadas()) {
            throw new ReglaDeNegocioException("La ruta ya tiene registradas sus " + ruta.getNumeroEntregasProgramadas()
                    + " entregas programadas");
        }
        Entrega entrega = new Entrega();
        entrega.setRuta(ruta);
        entrega.setNumeroGuia(textoOpcional(solicitud.numeroGuia()));
        entrega.setDestinatario(solicitud.destinatario().trim());
        entrega.setDireccion(solicitud.direccion().trim());
        entrega.setEstado(solicitud.estado());
        entrega.setNovedad(textoOpcional(solicitud.novedad()));
        return aResponse(entregaRepository.save(entrega));
    }

    @Transactional
    public void eliminar(Long id) {
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una entrega con id " + id));
        // Las entregas de una ruta cerrada forman parte del historial y no se corrigen.
        if (entrega.getRuta().getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaDeNegocioException("Solo se pueden corregir entregas de rutas EN_CURSO");
        }
        entregaRepository.delete(entrega);
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private EntregaResponse aResponse(Entrega entrega) {
        Ruta ruta = entrega.getRuta();
        return new EntregaResponse(entrega.getId(), ruta.getId(), ruta.getCodigo(), entrega.getNumeroGuia(),
                entrega.getDestinatario(), entrega.getDireccion(), entrega.getEstado(), entrega.getNovedad(),
                entrega.getFechaRegistro());
    }
}
