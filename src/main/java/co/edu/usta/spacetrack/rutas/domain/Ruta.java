package co.edu.usta.spacetrack.rutas.domain;

import co.edu.usta.spacetrack.operaciones.domain.Operacion;
import co.edu.usta.spacetrack.zonas.domain.Zona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "rutas")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    // Zona geografica que recorre la ruta.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    // Contrato/operacion al que pertenece la ruta.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion;

    @Column(nullable = false, length = 150)
    private String puntoOrigen;

    @Column(nullable = false, length = 150)
    private String puntoDestino;

    @Column(nullable = false)
    private Double distanciaEstimadaKm;

    @Column(nullable = false)
    private Integer numeroEntregasProgramadas;

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
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Zona getZona() { return zona; }
    public void setZona(Zona zona) { this.zona = zona; }
    public Operacion getOperacion() { return operacion; }
    public void setOperacion(Operacion operacion) { this.operacion = operacion; }
    public String getPuntoOrigen() { return puntoOrigen; }
    public void setPuntoOrigen(String puntoOrigen) { this.puntoOrigen = puntoOrigen; }
    public String getPuntoDestino() { return puntoDestino; }
    public void setPuntoDestino(String puntoDestino) { this.puntoDestino = puntoDestino; }
    public Double getDistanciaEstimadaKm() { return distanciaEstimadaKm; }
    public void setDistanciaEstimadaKm(Double distanciaEstimadaKm) { this.distanciaEstimadaKm = distanciaEstimadaKm; }
    public Integer getNumeroEntregasProgramadas() { return numeroEntregasProgramadas; }
    public void setNumeroEntregasProgramadas(Integer numeroEntregasProgramadas) { this.numeroEntregasProgramadas = numeroEntregasProgramadas; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
