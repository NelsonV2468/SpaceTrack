import { ProveedorAvisos } from "@/components/Avisos";

export default function LayoutAcceso({ children }: { children: React.ReactNode }) {
  return (
    <ProveedorAvisos>
      <main className="pagina-acceso">
        <div className="acceso-marca">
          <span className="isotipo" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z" /><circle cx="12" cy="9.5" r="2.5" />
            </svg>
          </span>
          <div>
            <strong>SpaceTrack</strong>
            <span>City Urban Bogotá</span>
          </div>
        </div>
        {children}
      </main>
    </ProveedorAvisos>
  );
}
