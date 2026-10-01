package co.edu.usta.spacetrack.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "co.edu.usta.spacetrack")
public class ManejadorGlobalDeExcepciones {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ResponseEntity<Map<String, Object>> manejarReglaDeNegocio(ReglaDeNegocioException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
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
