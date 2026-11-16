package br.edu.campus.biblioteca.controller;

import br.edu.campus.biblioteca.dto.*;
import br.edu.campus.biblioteca.model.TipoLeitor;
import br.edu.campus.biblioteca.service.LeitorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/leitores")
public class LeitorController {
    private final LeitorService service;

    public LeitorController(LeitorService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LeitorResponse> criar(@Valid @RequestBody LeitorRequest entrada) {
        var resposta = service.criar(entrada);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(location).body(resposta);
    }

    @GetMapping
    public List<LeitorResponse> listar(@RequestParam(required = false) TipoLeitor tipo) {
        return service.listar(tipo);
    }

    @GetMapping("/{id}")
    public LeitorResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public LeitorResponse atualizar(@PathVariable Long id, @Valid @RequestBody LeitorRequest entrada) {
        return service.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
