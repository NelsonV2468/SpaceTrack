"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

// En produccion Spring redirige "/" a "/panel/"; esto cubre el servidor de desarrollo de Next.
export default function Inicio() {
  const router = useRouter();
  useEffect(() => { router.replace("/panel/"); }, [router]);
  return null;
}
