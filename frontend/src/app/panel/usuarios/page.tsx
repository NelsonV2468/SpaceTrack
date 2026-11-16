"use client";

import { useAvisos } from "@/components/Avisos";
import { Cargando, Doble, Encabezado, Etiqueta, Panel, Vacio } from "@/components/ui";
import { api } from "@/lib/api";
import { fechaHora, type Tono } from "@/lib/formato";
import { useRecurso } from "@/lib/hooks";
import { NOMBRE_ROL, useSesion } from "@/lib/sesion";
import type { Rol, Usuario } from "@/lib/tipos";

const ROLES: [Rol, string][] = [["ADMIN", "Administrador"], ["OPERADOR", "Operador"], ["CONSULTA", "Consulta"], ["PENDIENTE", "Pendiente (sin acceso)"]];
const TONO_ROL: Record<Rol, Tono> = { ADMIN: "info", OPERADOR: "ok", CONSULTA: "neutral", PENDIENTE: "warn" };

export default function PaginaUsuarios() {
  const { usuario, esAdmin } = useSesion();
  const avisar = useAvisos();
  const usuarios = useRecurso<Usuario[]>(esAdmin ? "/api/usuarios" : null);

  if (!esAdmin) return <Vacio titulo="Acceso restringido" texto="Solo los administradores gestionan usuarios." />;
  if (!usuarios.datos) return usuarios.error ? <Vacio titulo="No se pudo cargar la información" texto={usuarios.error} /> : <Cargando />;

  async function cambiarRol(u: Usuario, rol: Rol) {
    try {
      await api("PATCH", `/api/usuarios/${u.id}/rol`, { rol });
      avisar("exito", `Rol de ${u.correo} actualizado a ${NOMBRE_ROL[rol]}.`);
    } catch (e) {
      avisar("error", (e as Error).message);
    }
    await usuarios.recargar();
  }

  const pendientes = usuarios.datos.filter((u) => u.rol === "PENDIENTE").length;

  return (
    <>
      <Encabezado titulo="Usuarios y roles"
        subtitulo="Administrador: todo. Operador: registra y actualiza, no elimina. Consulta: solo lectura. Las cuentas nuevas quedan pendientes." />
      {pendientes ? <div className="aviso info">{pendientes} cuenta(s) esperan aprobación: asígneles un rol para que puedan ingresar.</div> : null}
      <Panel titulo="Cuentas registradas" extra={<span className="total">{usuarios.datos.length} usuarios</span>}>
        {usuarios.datos.length ? (
          <div className="tabla">
            <table>
              <thead><tr><th>Usuario</th><th>Rol</th><th>Último ingreso</th><th className="derecha">Cambiar rol</th></tr></thead>
              <tbody>
                {usuarios.datos.map((u) => {
                  const propio = u.correo === usuario.correo;
                  return (
                    <tr key={u.id}>
                      <td data-titulo="Usuario"><Doble principal={u.nombre} secundario={u.correo} /></td>
                      <td data-titulo="Rol"><Etiqueta tono={TONO_ROL[u.rol]}>{NOMBRE_ROL[u.rol]}</Etiqueta></td>
                      <td className="num" data-titulo="Último ingreso">{fechaHora(u.ultimoIngreso)}</td>
                      <td className="derecha" data-titulo="Cambiar rol">
                        {propio ? <span className="sec">Su cuenta</span> : (
                          <select aria-label={`Rol de ${u.correo}`} value={u.rol} onChange={(e) => cambiarRol(u, e.target.value as Rol)}>
                            {ROLES.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
                          </select>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : <Vacio titulo="Sin usuarios" texto="Los usuarios aparecen aquí después de su primer ingreso." />}
      </Panel>
    </>
  );
}
