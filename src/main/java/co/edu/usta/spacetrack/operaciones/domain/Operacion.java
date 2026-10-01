package co.edu.usta.spacetrack.operaciones.domain;

import co.edu.usta.spacetrack.clientes.domain.Cliente;
import jakarta.persistence.Column;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "operaciones")
public class Operacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigoContrato;

    // Cada contrato pertenece a una empresa contratante registrada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 120)
    private String nombreOperacion;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 80)
    private String diasOperacion;

    @Column(nullable = false)
    private Integer numeroVehiculosRequeridos;

    @Column(nullable = false)
    private Integer numeroEntregasEsperadas;

    @Column(nullable = false)
    private boolean requiereCadenaFrio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOperacion estado = EstadoOperacion.ACTIVA;

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
    public String getCodigoContrato() { return codigoContrato; }
    public void setCodigoContrato(String codigoContrato) { this.codigoContrato = codigoContrato; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getNombreOperacion() { return nombreOperacion; }
    public void setNombreOperacion(String nombreOperacion) { this.nombreOperacion = nombreOperacion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getDiasOperacion() { return diasOperacion; }
    public void setDiasOperacion(String diasOperacion) { this.diasOperacion = diasOperacion; }
    public Integer getNumeroVehiculosRequeridos() { return numeroVehiculosRequeridos; }
    public void setNumeroVehiculosRequeridos(Integer numeroVehiculosRequeridos) { this.numeroVehiculosRequeridos = numeroVehiculosRequeridos; }
    public Integer getNumeroEntregasEsperadas() { return numeroEntregasEsperadas; }
    public void setNumeroEntregasEsperadas(Integer numeroEntregasEsperadas) { this.numeroEntregasEsperadas = numeroEntregasEsperadas; }
    public boolean isRequiereCadenaFrio() { return requiereCadenaFrio; }
    public void setRequiereCadenaFrio(boolean requiereCadenaFrio) { this.requiereCadenaFrio = requiereCadenaFrio; }
    public EstadoOperacion getEstado() { return estado; }
    public void setEstado(EstadoOperacion estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
