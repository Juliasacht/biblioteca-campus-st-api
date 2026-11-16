package br.edu.campus.biblioteca.dto;

import jakarta.validation.constraints.*;

public record LivroRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter até 150 caracteres") String titulo,
        @NotBlank(message = "O autor é obrigatório")
        @Size(max = 100, message = "O autor deve ter até 100 caracteres") String autor,
        @NotBlank(message = "O ISBN é obrigatório")
        @Pattern(regexp = "[0-9]{13}", message = "O ISBN deve ter 13 dígitos") String isbn,
        @NotNull(message = "O ano de publicação é obrigatório")
        @Min(value = 1450, message = "O ano de publicação deve ser a partir de 1450") Integer anoPublicacao,
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero") Integer quantidadeTotal) {}
