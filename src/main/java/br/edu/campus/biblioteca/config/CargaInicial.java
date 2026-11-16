package br.edu.campus.biblioteca.config;

import br.edu.campus.biblioteca.model.*;
import br.edu.campus.biblioteca.repository.*;
import br.edu.campus.biblioteca.service.politica.PoliticaEmprestimo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "biblioteca.carga-inicial", havingValue = "true")
public class CargaInicial implements CommandLineRunner {
    private final LivroRepository livros;
    private final LeitorRepository leitores;
    private final EmprestimoRepository emprestimos;
    private final Clock clock;
    private final List<PoliticaEmprestimo> politicas;

    public CargaInicial(LivroRepository livros, LeitorRepository leitores,
            EmprestimoRepository emprestimos, Clock clock, List<PoliticaEmprestimo> politicas) {
        this.livros = livros;
        this.leitores = leitores;
        this.emprestimos = emprestimos;
        this.clock = clock;
        this.politicas = politicas;
    }

    @Override
    public void run(String... args) {
        Livro livro = livros.save(new Livro(null, "Código Limpo", "Robert C. Martin",
                "9788576082675", 2009, 3));
        livros.save(new Livro(null, "Java: Como Programar", "Paul Deitel",
                "9788543004792", 2016, 2));
        livros.save(new Livro(null, "Engenharia de Software", "Ian Sommerville",
                "9788543024974", 2019, 2));
        Leitor aluno = leitores.save(new Leitor(null, "Ana Souza", "ana@campus.edu.br", TipoLeitor.ALUNO));
        leitores.save(new Leitor(null, "Carlos Lima", "carlos@campus.edu.br", TipoLeitor.PROFESSOR));
        int prazo = politicas.stream().filter(p -> p.tipo() == aluno.tipo())
                .findFirst().orElseThrow().prazoEmDias();
        LocalDate vencimento = LocalDate.now(clock).minusDays(3);
        // Inserção direta é intencional: permite demonstrar atraso sem aguardar dias.
        emprestimos.save(new Emprestimo(null, livro.id(), aluno.id(), livro.titulo(), aluno.nome(),
                vencimento.minusDays(prazo), vencimento, null, new BigDecimal("0.00")));
    }
}
