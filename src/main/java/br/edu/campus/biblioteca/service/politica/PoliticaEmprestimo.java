package br.edu.campus.biblioteca.service.politica;

import br.edu.campus.biblioteca.model.TipoLeitor;
import java.math.BigDecimal;

public interface PoliticaEmprestimo {
    TipoLeitor tipo();
    int prazoEmDias();
    int limiteEmprestimos();
    BigDecimal calcularMulta(long diasAtraso);
}
