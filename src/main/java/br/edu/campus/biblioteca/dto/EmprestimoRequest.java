package br.edu.campus.biblioteca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EmprestimoRequest(
        @NotNull(message = "O livro é obrigatório")
        @Positive(message = "O identificador do livro deve ser maior que zero") Long livroId,
        @NotNull(message = "O leitor é obrigatório")
        @Positive(message = "O identificador do leitor deve ser maior que zero") Long leitorId) {}
