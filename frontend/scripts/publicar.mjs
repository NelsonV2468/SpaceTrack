// Copia el sitio exportado (out/) a los recursos de Spring Boot y calcula los hashes CSP
// de los scripts en linea que genera Next, para no tener que permitir 'unsafe-inline'.
import { createHash } from "node:crypto";
import { cpSync, existsSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const origen = resolve(raiz, "out");
const destino = resolve(raiz, "..", "src", "main", "resources", "webapp");

if (!existsSync(origen)) {
  console.error("No existe frontend/out: ejecute primero 'next build'.");
  process.exit(1);
}

function* archivosHtml(directorio) {
  for (const nombre of readdirSync(directorio)) {
    const ruta = join(directorio, nombre);
    if (statSync(ruta).isDirectory()) yield* archivosHtml(ruta);
    else if (nombre.endsWith(".html")) yield ruta;
  }
}

const hashes = new Set();
const scriptEnLinea = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g;
for (const archivo of archivosHtml(origen)) {
  for (const [, contenido] of readFileSync(archivo, "utf8").matchAll(scriptEnLinea)) {
    if (contenido) hashes.add(`'sha256-${createHash("sha256").update(contenido, "utf8").digest("base64")}'`);
  }
}

rmSync(destino, { recursive: true, force: true });
cpSync(origen, destino, { recursive: true });
writeFileSync(join(destino, "csp-hashes.txt"), [...hashes].sort().join(" "));
console.log(`Frontend publicado en ${destino} (${hashes.size} scripts en linea autorizados por hash)`);
