package br.edu.campus.biblioteca.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmprestimoResponse(Long id, Long livroId, String titulo,
                                 Long leitorId, String leitor, LocalDate dataEmprestimo,
                                 LocalDate dataPrevistaDevolucao, LocalDate dataDevolucao,
                                 BigDecimal multa) {}
