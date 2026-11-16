package br.edu.campus.biblioteca.dto;

import br.edu.campus.biblioteca.model.TipoLeitor;

public record LeitorResponse(Long id, String nome, String email, TipoLeitor tipo) {}
