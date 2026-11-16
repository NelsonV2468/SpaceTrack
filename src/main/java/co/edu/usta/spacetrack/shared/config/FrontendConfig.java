package co.edu.usta.spacetrack.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el frontend Next.js exportado (classpath:/webapp, generado por "npm run build" en frontend/)
 * y los recursos estaticos heredados (classpath:/static, usados por las vistas Thymeleaf).
 * Next exporta cada pagina como carpeta/index.html, por eso /panel/clientes/ resuelve a
 * panel/clientes/index.html. Los controladores REST tienen prioridad sobre estos recursos.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/webapp/", "classpath:/static/")
                .resourceChain(true)
                .addResolver(new IndiceDeCarpetaResolver());
    }

    static class IndiceDeCarpetaResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String rutaRecurso, Resource ubicacion) throws IOException {
            // Spring entrega la ruta sin la "/" final: "panel/clientes" para /panel/clientes/.
            Resource archivo = rutaRecurso.isEmpty() || rutaRecurso.endsWith("/") ? null : super.getResource(rutaRecurso, ubicacion);
            if (archivo != null) return archivo;
            String ultimoSegmento = rutaRecurso.substring(rutaRecurso.lastIndexOf('/') + 1);
            if (ultimoSegmento.startsWith("__next.") && ultimoSegmento.endsWith(".txt")) {
                return super.getResource(rutaSegmentoNext(rutaRecurso, ultimoSegmento), ubicacion);
            }
            if (ultimoSegmento.contains(".")) return null;
            String carpeta = rutaRecurso.isEmpty() || rutaRecurso.endsWith("/") ? rutaRecurso : rutaRecurso + "/";
            return super.getResource(carpeta + "index.html", ubicacion);
        }

        /**
         * El cliente de Next pide los fragmentos de prefetch como "__next.panel.rutas.__PAGE__.txt",
         * pero la exportacion estatica los guarda como "__next.panel/rutas/__PAGE__.txt".
         */
        static String rutaSegmentoNext(String rutaRecurso, String nombre) {
            String centro = nombre.substring("__next.".length(), nombre.length() - ".txt".length());
            String carpeta = rutaRecurso.substring(0, rutaRecurso.length() - nombre.length());
            return carpeta + "__next." + centro.replace('.', '/') + ".txt";
        }
    }
}
