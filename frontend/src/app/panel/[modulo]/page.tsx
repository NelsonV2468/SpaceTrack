import { notFound } from "next/navigation";
import { CLAVES_MODULOS } from "@/modulos/config";
import { ModuloCrud } from "@/modulos/ModuloCrud";

// Una pagina estatica por modulo generico; "rutas" tiene su propia carpeta (incluye el detalle).
const GENERICOS = CLAVES_MODULOS.filter((clave) => clave !== "rutas");

export const dynamicParams = false;
export function generateStaticParams() {
  return GENERICOS.map((modulo) => ({ modulo }));
}

export default async function PaginaModulo({ params }: { params: Promise<{ modulo: string }> }) {
  const { modulo } = await params;
  if (!GENERICOS.includes(modulo)) notFound();
  return <ModuloCrud clave={modulo} />;
}
