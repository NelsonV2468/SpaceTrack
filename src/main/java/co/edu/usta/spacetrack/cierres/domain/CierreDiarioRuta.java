package co.edu.usta.spacetrack.cierres.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Detalle de una ruta dentro de un cierre diario. Es una copia de los datos al momento del cierre,
 * por eso guarda textos (codigo, zona, placa, conductor) y no cambia si despues se editan esos registros.
 */
@Entity
@Table(name = "cierres_diarios_rutas")
public class CierreDiarioRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cierre_id", nullable = false)
    private CierreDiario cierre;

    @Column(nullable = false)
    private Long rutaId;

    @Column(nullable = false, length = 30)
    private String codigoRuta;

    @Column(length = 120)
    private String nombreRuta;

    @Column(length = 80)
    private String nombreZona;

    @Column(length = 10)
    private String placaVehiculo;

    @Column(length = 145)
    private String nombreConductor;

    private LocalDateTime fechaInicioReal;

    private LocalDateTime fechaFinReal;

    @Column(nullable = false)
    private int entregasProgramadas;

    @Column(nullable = false)
    private int entregasRealizadas;

    @Column(nullable = false)
    private int entregasNoRealizadas;

    @Column(nullable = false)
    private int entregasSinRegistrar;

    protected CierreDiarioRuta() {
    }

    public CierreDiarioRuta(Long rutaId, String codigoRuta, String nombreRuta, String nombreZona, String placaVehiculo,
                            String nombreConductor, LocalDateTime fechaInicioReal, LocalDateTime fechaFinReal,
                            int entregasProgramadas, int entregasRealizadas, int entregasNoRealizadas) {
        this.rutaId = rutaId;
        this.codigoRuta = codigoRuta;
        this.nombreRuta = nombreRuta;
        this.nombreZona = nombreZona;
        this.placaVehiculo = placaVehiculo;
        this.nombreConductor = nombreConductor;
        this.fechaInicioReal = fechaInicioReal;
        this.fechaFinReal = fechaFinReal;
        this.entregasProgramadas = entregasProgramadas;
        this.entregasRealizadas = entregasRealizadas;
        this.entregasNoRealizadas = entregasNoRealizadas;
        this.entregasSinRegistrar = Math.max(0, entregasProgramadas - entregasRealizadas - entregasNoRealizadas);
    }

    public Long getId() { return id; }
    public CierreDiario getCierre() { return cierre; }
    void setCierre(CierreDiario cierre) { this.cierre = cierre; }
    public Long getRutaId() { return rutaId; }
    public String getCodigoRuta() { return codigoRuta; }
    public String getNombreRuta() { return nombreRuta; }
    public String getNombreZona() { return nombreZona; }
    public String getPlacaVehiculo() { return placaVehiculo; }
    public String getNombreConductor() { return nombreConductor; }
    public LocalDateTime getFechaInicioReal() { return fechaInicioReal; }
    public LocalDateTime getFechaFinReal() { return fechaFinReal; }
    public int getEntregasProgramadas() { return entregasProgramadas; }
    public int getEntregasRealizadas() { return entregasRealizadas; }
    public int getEntregasNoRealizadas() { return entregasNoRealizadas; }
    public int getEntregasSinRegistrar() { return entregasSinRegistrar; }
}
