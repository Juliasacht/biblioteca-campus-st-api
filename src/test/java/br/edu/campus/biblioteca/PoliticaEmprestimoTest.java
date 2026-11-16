package br.edu.campus.biblioteca;

import br.edu.campus.biblioteca.service.politica.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaEmprestimoTest {
    @Test
    void respeitaValoresConfiguradosESemMultaNegativa() {
        var aluno = new PoliticaAluno(9, 4, new BigDecimal("1.25"));
        var professor = new PoliticaProfessor(20, 7, new BigDecimal("0.75"));
        assertEquals(9, aluno.prazoEmDias());
        assertEquals(4, aluno.limiteEmprestimos());
        assertEquals(new BigDecimal("3.75"), aluno.calcularMulta(3));
        assertEquals(new BigDecimal("0.00"), aluno.calcularMulta(-2));
        assertEquals(20, professor.prazoEmDias());
        assertEquals(7, professor.limiteEmprestimos());
        assertEquals(new BigDecimal("2.25"), professor.calcularMulta(3));
    }
}
