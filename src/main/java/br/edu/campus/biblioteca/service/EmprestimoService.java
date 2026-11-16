package br.edu.campus.biblioteca.service;

import br.edu.campus.biblioteca.dto.*;
import br.edu.campus.biblioteca.exception.*;
import br.edu.campus.biblioteca.model.*;
import br.edu.campus.biblioteca.repository.*;
import br.edu.campus.biblioteca.service.politica.PoliticaEmprestimo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EmprestimoService {
    private final EmprestimoRepository emprestimos;
    private final LivroRepository livros;
    private final LeitorRepository leitores;
    private final Clock clock;
    private final ControleAcervo controle;
    private final Map<TipoLeitor, PoliticaEmprestimo> politicas;

    public EmprestimoService(EmprestimoRepository emprestimos, LivroRepository livros,
            LeitorRepository leitores, Clock clock, ControleAcervo controle,
            List<PoliticaEmprestimo> politicas) {
        this.emprestimos = emprestimos;
        this.livros = livros;
        this.leitores = leitores;
        this.clock = clock;
        this.controle = controle;
        this.politicas = politicas.stream()
                .collect(Collectors.toUnmodifiableMap(PoliticaEmprestimo::tipo, Function.identity()));
        if (this.politicas.size() != TipoLeitor.values().length) {
            throw new IllegalArgumentException("É necessária uma política para cada tipo de leitor");
        }
    }

    public EmprestimoResponse criar(EmprestimoRequest entrada) {
        return controle.executar(() -> {
            Livro livro = livros.findById(entrada.livroId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Livro " + entrada.livroId() + " não encontrado"));
            Leitor leitor = leitores.findById(entrada.leitorId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Leitor " + entrada.leitorId() + " não encontrado"));
            LocalDate hoje = LocalDate.now(clock);
            PoliticaEmprestimo politica = politicas.get(leitor.tipo());
            List<Emprestimo> ativos = emprestimos.findAll().stream().filter(Emprestimo::ativo).toList();

            if (ativos.stream().filter(e -> e.livroId().equals(livro.id())).count()
                    >= livro.quantidadeTotal()) {
                throw new RegraNegocioException("Livro sem exemplar disponível");
            }
            if (ativos.stream().filter(e -> e.leitorId().equals(leitor.id())).count()
                    >= politica.limiteEmprestimos()) {
                throw new RegraNegocioException("O leitor " + leitor.nome() + " atingiu o limite de "
                        + politica.limiteEmprestimos() + " empréstimos ativos");
            }
            if (ativos.stream().anyMatch(e -> e.leitorId().equals(leitor.id())
                    && e.dataPrevistaDevolucao().isBefore(hoje))) {
                throw new RegraNegocioException("Leitor com empréstimo atrasado não pode fazer novo empréstimo");
            }
            Emprestimo novo = new Emprestimo(null, livro.id(), leitor.id(), livro.titulo(), leitor.nome(),
                    hoje, hoje.plusDays(politica.prazoEmDias()), null, new BigDecimal("0.00"));
            return resposta(emprestimos.save(novo));
        });
    }

    public List<EmprestimoResponse> listar(Long leitorId, Boolean ativos) {
        return controle.executar(() -> emprestimos.findAll().stream()
                .filter(e -> leitorId == null || e.leitorId().equals(leitorId))
                .filter(e -> ativos == null || e.ativo() == ativos)
                .map(this::resposta).toList());
    }

    public EmprestimoResponse buscar(Long id) {
        return controle.executar(() -> resposta(encontrar(id)));
    }

    public EmprestimoResponse devolver(Long id) {
        return controle.executar(() -> {
            Emprestimo atual = encontrar(id);
            if (!atual.ativo()) {
                throw new RegraNegocioException("Empréstimo já devolvido");
            }
            Leitor leitor = leitores.findById(atual.leitorId()).orElseThrow(() ->
                    new RecursoNaoEncontradoException("Leitor do empréstimo não encontrado"));
            LocalDate hoje = LocalDate.now(clock);
            long diasAtraso = Math.max(0, ChronoUnit.DAYS.between(atual.dataPrevistaDevolucao(), hoje));
            BigDecimal multa = politicas.get(leitor.tipo()).calcularMulta(diasAtraso);
            Emprestimo devolvido = new Emprestimo(atual.id(), atual.livroId(), atual.leitorId(),
                    atual.titulo(), atual.leitor(), atual.dataEmprestimo(),
                    atual.dataPrevistaDevolucao(), hoje, multa);
            return resposta(emprestimos.save(devolvido));
        });
    }

    public List<EmprestimoResponse> atrasados() {
        return controle.executar(() -> {
            LocalDate hoje = LocalDate.now(clock);
            return emprestimos.findAll().stream().filter(Emprestimo::ativo)
                    .filter(e -> e.dataPrevistaDevolucao().isBefore(hoje))
                    .map(this::resposta).toList();
        });
    }

    private Emprestimo encontrar(Long id) {
        return emprestimos.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Empréstimo " + id + " não encontrado"));
    }

    private EmprestimoResponse resposta(Emprestimo e) {
        return new EmprestimoResponse(e.id(), e.livroId(), e.titulo(), e.leitorId(), e.leitor(),
                e.dataEmprestimo(), e.dataPrevistaDevolucao(), e.dataDevolucao(), e.multa());
    }
}
