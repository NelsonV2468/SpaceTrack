package co.edu.usta.spacetrack.vehiculos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String placa;

    @Column(nullable = false, length = 50)
    private String tipoVehiculo;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 50)
    private String modelo;

    @Column(nullable = false)
    private Integer anioModelo;

    @Column(nullable = false)
    private Double capacidadCargaKg;

    @Column(nullable = false)
    private boolean requiereCadenaFrio;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Mantiene la trazabilidad basica del recurso de flota.
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
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getTipoVehiculo() { return tipoVehiculo; }
    public void setTipoVehiculo(String tipoVehiculo) { this.tipoVehiculo = tipoVehiculo; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getAnioModelo() { return anioModelo; }
    public void setAnioModelo(Integer anioModelo) { this.anioModelo = anioModelo; }
    public Double getCapacidadCargaKg() { return capacidadCargaKg; }
    public void setCapacidadCargaKg(Double capacidadCargaKg) { this.capacidadCargaKg = capacidadCargaKg; }
    public boolean isRequiereCadenaFrio() { return requiereCadenaFrio; }
    public void setRequiereCadenaFrio(boolean requiereCadenaFrio) { this.requiereCadenaFrio = requiereCadenaFrio; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
