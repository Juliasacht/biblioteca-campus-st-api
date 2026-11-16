package br.edu.campus.biblioteca.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Emprestimo(Long id, Long livroId, Long leitorId,
                         LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao,
                         LocalDate dataDevolucao, BigDecimal multa) {
    public boolean ativo() {
        return dataDevolucao == null;
    }
}
