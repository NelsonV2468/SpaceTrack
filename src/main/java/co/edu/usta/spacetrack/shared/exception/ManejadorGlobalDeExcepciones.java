package co.edu.usta.spacetrack.shared.exception;

import co.edu.usta.spacetrack.rutas.domain.EstadoRuta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "co.edu.usta.spacetrack")
public class ManejadorGlobalDeExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalDeExcepciones.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ResponseEntity<Map<String, Object>> manejarReglaDeNegocio(ReglaDeNegocioException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Por ejemplo, eliminar un vehiculo que sigue asignado a una ruta.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT, "El registro esta relacionado con otros datos y no se puede modificar o eliminar");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> manejarValidaciones(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            errores.put(campo, error.getDefaultMessage());
        });
        Map<String, Object> cuerpo = cuerpoBase(HttpStatus.BAD_REQUEST, "Hay datos invalidos en la solicitud");
        cuerpo.put("errores", errores);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // Tambien llega aqui cualquier otro parametro mal tipado, por ejemplo /api/rutas/abc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        if (ex.getRequiredType() == EstadoRuta.class) {
            return respuesta(HttpStatus.BAD_REQUEST, "Estado de ruta invalido: " + ex.getValue() + ". Use " + estadosPermitidos());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "El parametro " + ex.getName() + " tiene un valor invalido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido");
    }

    // Los errores 4xx propios de Spring conservan su codigo para no presentarlos como fallos internos.
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> manejarInesperado(Exception ex) {
        if (ex instanceof ErrorResponse errorSpring && errorSpring.getStatusCode().is4xxClientError()) {
            return respuesta(HttpStatus.valueOf(errorSpring.getStatusCode().value()), "La solicitud no se pudo procesar");
        }
        log.error("Error inesperado al atender la solicitud", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno. Intente de nuevo mas tarde");
    }

    private String estadosPermitidos() {
        List<String> estados = Arrays.stream(EstadoRuta.values()).map(Enum::name).toList();
        int ultimo = estados.size() - 1;
        return String.join(", ", estados.subList(0, ultimo)) + " o " + estados.get(ultimo);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(cuerpoBase(estado, mensaje));
    }

    private Map<String, Object> cuerpoBase(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("fecha", LocalDateTime.now());
        cuerpo.put("estado", estado.value());
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
    }
}
