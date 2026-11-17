package co.edu.usta.spacetrack.cierres.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ON-20. Cierre de la operacion de un dia: copia fija de las rutas finalizadas ese dia y sus entregas.
 * Solo hay un cierre por fecha; despues del cierre no se inician ni finalizan rutas en ese dia.
 */
@Entity
@Table(name = "cierres_diarios")
public class CierreDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false, length = 120)
    private String registradoPor;

    @Column(length = 255)
    private String observaciones;

    @Column(nullable = false)
    private int rutasCerradas;

    @Column(nullable = false)
    private int entregasProgramadas;

    @Column(nullable = false)
    private int entregasRealizadas;

    @Column(nullable = false)
    private int entregasNoRealizadas;

    @Column(nullable = false)
    private int entregasSinRegistrar;

    @OneToMany(mappedBy = "cierre", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("codigoRuta ASC")
    private List<CierreDiarioRuta> rutas = new ArrayList<>();

    /** Agrega una ruta al cierre y acumula sus entregas en los totales del dia. */
    public void agregarRuta(CierreDiarioRuta ruta) {
        ruta.setCierre(this);
        rutas.add(ruta);
        rutasCerradas = rutas.size();
        entregasProgramadas += ruta.getEntregasProgramadas();
        entregasRealizadas += ruta.getEntregasRealizadas();
        entregasNoRealizadas += ruta.getEntregasNoRealizadas();
        entregasSinRegistrar += ruta.getEntregasSinRegistrar();
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public int getRutasCerradas() { return rutasCerradas; }
    public int getEntregasProgramadas() { return entregasProgramadas; }
    public int getEntregasRealizadas() { return entregasRealizadas; }
    public int getEntregasNoRealizadas() { return entregasNoRealizadas; }
    public int getEntregasSinRegistrar() { return entregasSinRegistrar; }
    public List<CierreDiarioRuta> getRutas() { return rutas; }
}
