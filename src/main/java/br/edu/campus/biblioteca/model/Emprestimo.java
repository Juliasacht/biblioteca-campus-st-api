package br.edu.campus.biblioteca.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Preserva identificação e política da contratação, mesmo após alterações no cadastro.
public record Emprestimo(Long id, Long livroId, Long leitorId, String titulo, String leitor,
                         TipoLeitor tipoLeitorNaContratacao, LocalDate dataEmprestimo, LocalDate dataPrevistaDevolucao,
                         LocalDate dataDevolucao, BigDecimal multa) {
    public boolean ativo() { return dataDevolucao == null; }
}
