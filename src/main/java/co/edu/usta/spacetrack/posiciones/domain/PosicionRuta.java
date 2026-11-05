package co.edu.usta.spacetrack.posiciones.domain;

import co.edu.usta.spacetrack.rutas.domain.Ruta;
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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "posiciones_ruta")
public class PosicionRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    // Opcional en las posiciones manuales.
    private Integer velocidadKmh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenPosicion origen;

    // La asigna quien registra con el Clock inyectado, para poder fijar la hora en las pruebas.
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    public Long getId() {
        return id;
    }

    public Ruta getRuta() {
        return ruta;
    }

    public void setRuta(Ruta ruta) {
        this.ruta = ruta;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public Integer getVelocidadKmh() {
        return velocidadKmh;
    }

    public void setVelocidadKmh(Integer velocidadKmh) {
        this.velocidadKmh = velocidadKmh;
    }

    public OrigenPosicion getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenPosicion origen) {
        this.origen = origen;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
