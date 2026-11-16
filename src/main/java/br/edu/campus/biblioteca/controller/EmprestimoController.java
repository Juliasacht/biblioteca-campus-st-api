package br.edu.campus.biblioteca.controller;

import br.edu.campus.biblioteca.dto.*;
import br.edu.campus.biblioteca.service.EmprestimoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {
    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EmprestimoResponse> criar(@Valid @RequestBody EmprestimoRequest entrada) {
        var resposta = service.criar(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(location).body(resposta);
    }

    @GetMapping
    public List<EmprestimoResponse> listar(@RequestParam(required = false) Long leitorId,
            @RequestParam(required = false) Boolean ativos) {
        return service.listar(leitorId, ativos);
    }

    @GetMapping("/atrasados")
    public List<EmprestimoResponse> atrasados() {
        return service.atrasados();
    }

    @GetMapping("/{id}")
    public EmprestimoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/devolucao")
    public EmprestimoResponse devolver(@PathVariable Long id) {
        return service.devolver(id);
    }
}
