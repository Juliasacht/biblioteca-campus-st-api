package br.edu.campus.biblioteca.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Object> naoEncontrado(RecursoNaoEncontradoException ex, WebRequest request) {
        return problema(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> conflito(RegraNegocioException ex, WebRequest request) {
        return problema(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request, null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
                erros.merge(e.getField(), e.getDefaultMessage(), (a, b) -> a + "; " + b));
        return problema(HttpStatus.BAD_REQUEST, "Erro de validação",
                "Um ou mais campos estão inválidos", request, erros);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "JSON inválido ou valor incompatível. Para tipo, utilize ALUNO ou PROFESSOR.",
                request, null);
    }

    // Também cobre parâmetros inválidos, método HTTP incorreto e demais erros MVC.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detalhe = body instanceof ProblemDetail pd
                ? pd : ProblemDetail.forStatusAndDetail(status, "Não foi possível processar a requisição");
        detalhe.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        HttpHeaders resposta = new HttpHeaders();
        resposta.putAll(headers);
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(detalhe, resposta, status);
    }

    private ResponseEntity<Object> problema(HttpStatus status, String titulo, String mensagem,
            WebRequest request, Map<String, String> erros) {
        ProblemDetail detalhe = ProblemDetail.forStatusAndDetail(status, mensagem);
        detalhe.setTitle(titulo);
        detalhe.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        if (erros != null) {
            detalhe.setProperty("erros", erros);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(detalhe);
    }
}
