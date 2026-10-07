package co.edu.usta.spacetrack.necesidades.domain;

import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "necesidades_cliente")
public class NecesidadCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La necesidad se registra sobre el contrato; el cliente se obtiene desde la operacion.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion;

    @Column(nullable = false, length = 50)
    private String tipoVehiculo;

    @Column(nullable = false)
    private Integer cantidadVehiculos;

    @Column(nullable = false)
    private Double capacidadMinimaKg;

    @Column(nullable = false)
    private boolean requiereCadenaFrio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "necesidad_dias", joinColumns = @JoinColumn(name = "necesidad_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false, length = 10)
    private Set<DiaSemana> diasOperacion = EnumSet.noneOf(DiaSemana.class);

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(length = 255)
    private String observaciones;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void crearAuditoria() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void actualizarAuditoria() {
        fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Operacion getOperacion() { return operacion; }
    public void setOperacion(Operacion operacion) { this.operacion = operacion; }
    public String getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(String tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; }
    public Integer getCantidadVehiculos() { return cantidadVehiculos; }
    public void setCantidadVehiculos(Integer cantidadVehiculos) { this.cantidadVehiculos = cantidadVehiculos; }
    public Double getCapacidadMinimaKg() { return capacidadMinimaKg; }
    public void setCapacidadMinimaKg(Double capacidadMinimaKg) { this.capacidadMinimaKg = capacidadMinimaKg; }
    public boolean isRequiereCadenaFrio() { return requiereCadenaFrio; }
    public void setRequiereCadenaFrio(boolean requiereCadenaFrio) { this.requiereCadenaFrio = requiereCadenaFrio; }
    public Set<DiaSemana> getDiasOperacion() { return diasOperacion; }
    public void setDiasOperacion(Set<DiaSemana> diasOperacion) {
        this.diasOperacion.clear();
        this.diasOperacion.addAll(diasOperacion);
    }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
