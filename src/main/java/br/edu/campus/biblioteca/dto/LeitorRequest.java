package br.edu.campus.biblioteca.dto;

import br.edu.campus.biblioteca.model.TipoLeitor;
import jakarta.validation.constraints.*;

public record LeitorRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter até 100 caracteres") String nome,
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido") String email,
        @NotNull(message = "O tipo é obrigatório: ALUNO ou PROFESSOR") TipoLeitor tipo) {}
