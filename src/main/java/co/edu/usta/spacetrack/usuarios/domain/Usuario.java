package co.edu.usta.spacetrack.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Usuario de la aplicacion. La identidad (correo y contrasena) vive en Neon Auth;
 * aqui solo se guarda el rol que SpaceTrack le concede.
 */
@Entity
@Table(name = "usuarios_app")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador "sub" del token emitido por Neon Auth.
    @Column(nullable = false, unique = true, length = 120)
    private String authId;

    @Column(nullable = false, unique = true, length = 160)
    private String correo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.PENDIENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime ultimoIngreso;

    protected Usuario() {
    }

    public Usuario(String authId, String correo, String nombre, Rol rol) {
        this.authId = authId;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    @PrePersist
    void crearAuditoria() {
        fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getUltimoIngreso() { return ultimoIngreso; }
    public void setUltimoIngreso(LocalDateTime ultimoIngreso) { this.ultimoIngreso = ultimoIngreso; }
}
