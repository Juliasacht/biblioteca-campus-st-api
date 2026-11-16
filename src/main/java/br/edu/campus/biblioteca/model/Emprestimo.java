package br.edu.campus.biblioteca.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Título e nome são preservados para o histórico após exclusões permitidas.
public record Emprestimo(Long id, Long livroId, Long leitorId, String titulo, String leitor,
                         LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao,
                         LocalDate dataDevolucao, BigDecimal multa) {
    public boolean ativo() { return dataDevolucao == null; }
}
