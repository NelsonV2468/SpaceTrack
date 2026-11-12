package co.edu.usta.spacetrack.usuarios.domain;

/**
 * Roles basicos de SpaceTrack.
 * <ul>
 *     <li>ADMIN: todo, incluida la eliminacion de registros y la gestion de usuarios.</li>
 *     <li>OPERADOR: consulta, crea y actualiza; no elimina.</li>
 *     <li>CONSULTA: solo lectura.</li>
 *     <li>PENDIENTE: cuenta recien creada sin acceso hasta que un administrador la apruebe.</li>
 * </ul>
 */
public enum Rol {
    ADMIN,
    OPERADOR,
    CONSULTA,
    PENDIENTE;

    public boolean tieneAcceso() {
        return this != PENDIENTE;
    }
}
