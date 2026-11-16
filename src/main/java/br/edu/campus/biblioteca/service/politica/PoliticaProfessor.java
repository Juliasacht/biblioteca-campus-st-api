package br.edu.campus.biblioteca.service.politica;

import br.edu.campus.biblioteca.model.TipoLeitor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PoliticaProfessor implements PoliticaEmprestimo {
    private final int prazo;
    private final int limite;
    private final BigDecimal multaDiaria;

    public PoliticaProfessor(@Value("${biblioteca.professor.prazo-dias}") int prazo,
                         @Value("${biblioteca.professor.limite-emprestimos}") int limite,
                         @Value("${biblioteca.professor.multa-diaria}") BigDecimal multaDiaria) {
        if (prazo <= 0 || limite <= 0 || multaDiaria.signum() < 0) {
            throw new IllegalArgumentException("Configuração da política professor inválida");
        }
        this.prazo = prazo;
        this.limite = limite;
        this.multaDiaria = multaDiaria;
    }

    @Override
    public TipoLeitor tipo() { return TipoLeitor.PROFESSOR; }

    @Override
    public int prazoEmDias() { return prazo; }

    @Override
    public int limiteEmprestimos() { return limite; }

    @Override
    public BigDecimal calcularMulta(long diasAtraso) {
        return multaDiaria.multiply(BigDecimal.valueOf(Math.max(0, diasAtraso)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
