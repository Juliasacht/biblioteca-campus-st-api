package br.edu.campus.biblioteca.service;

import br.edu.campus.biblioteca.dto.*;
import br.edu.campus.biblioteca.exception.*;
import br.edu.campus.biblioteca.model.*;
import br.edu.campus.biblioteca.repository.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LeitorService {
    private final LeitorRepository leitores;
    private final EmprestimoRepository emprestimos;
    private final ControleAcervo controle;

    public LeitorService(LeitorRepository leitores, EmprestimoRepository emprestimos, ControleAcervo controle) {
        this.leitores = leitores;
        this.emprestimos = emprestimos;
        this.controle = controle;
    }

    public LeitorResponse criar(LeitorRequest entrada) {
        return controle.executar(() -> {
            validarEmail(entrada.email(), null);
            return resposta(leitores.save(modelo(null, entrada)));
        });
    }

    public List<LeitorResponse> listar(TipoLeitor tipo) {
        return controle.executar(() -> leitores.findAll().stream()
                .filter(l -> tipo == null || l.tipo() == tipo).map(this::resposta).toList());
    }

    public LeitorResponse buscar(Long id) {
        return controle.executar(() -> resposta(encontrar(id)));
    }

    public LeitorResponse atualizar(Long id, LeitorRequest entrada) {
        return controle.executar(() -> {
            Leitor atual = encontrar(id);
            validarEmail(entrada.email(), id);
            if (atual.tipo() != entrada.tipo() && temAtivos(id)) {
                throw new RegraNegocioException("O tipo do leitor não pode mudar enquanto houver empréstimos ativos");
            }
            return resposta(leitores.save(modelo(id, entrada)));
        });
    }

    public void remover(Long id) {
        controle.executar(() -> {
            encontrar(id);
            if (temAtivos(id)) {
                throw new RegraNegocioException("Leitor com empréstimo ativo não pode ser removido");
            }
            leitores.deleteById(id);
            return null;
        });
    }

    private Leitor encontrar(Long id) {
        return leitores.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Leitor " + id + " não encontrado"));
    }

    private boolean temAtivos(Long id) {
        return emprestimos.findAll().stream().anyMatch(e -> e.ativo() && e.leitorId().equals(id));
    }

    private void validarEmail(String email, Long id) {
        if (leitores.findAll().stream().anyMatch(l -> l.email().equalsIgnoreCase(email.strip())
                && !Objects.equals(l.id(), id))) {
            throw new RegraNegocioException("Já existe leitor com o e-mail informado");
        }
    }

    private Leitor modelo(Long id, LeitorRequest e) {
        return new Leitor(id, e.nome().strip(), e.email().strip().toLowerCase(Locale.ROOT), e.tipo());
    }

    private LeitorResponse resposta(Leitor l) {
        return new LeitorResponse(l.id(), l.nome(), l.email(), l.tipo());
    }
}
