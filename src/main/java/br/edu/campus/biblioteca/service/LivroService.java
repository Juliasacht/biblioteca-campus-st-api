package br.edu.campus.biblioteca.service;

import br.edu.campus.biblioteca.dto.*;
import br.edu.campus.biblioteca.exception.*;
import br.edu.campus.biblioteca.model.Livro;
import br.edu.campus.biblioteca.repository.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LivroService {
    private final LivroRepository livros;
    private final EmprestimoRepository emprestimos;
    private final ControleAcervo controle;

    public LivroService(LivroRepository livros, EmprestimoRepository emprestimos, ControleAcervo controle) {
        this.livros = livros;
        this.emprestimos = emprestimos;
        this.controle = controle;
    }

    public LivroResponse criar(LivroRequest entrada) {
        return controle.executar(() -> {
            validarIsbn(entrada.isbn(), null);
            return resposta(livros.save(modelo(null, entrada)));
        });
    }

    public List<LivroResponse> listar(String autor, Boolean disponivel) {
        return controle.executar(() -> livros.findAll().stream()
                .filter(l -> autor == null || l.autor().toLowerCase(Locale.ROOT)
                        .contains(autor.strip().toLowerCase(Locale.ROOT)))
                .map(this::resposta)
                .filter(l -> disponivel == null || (l.quantidadeDisponivel() > 0) == disponivel)
                .toList());
    }

    public LivroResponse buscar(Long id) {
        return controle.executar(() -> resposta(encontrar(id)));
    }

    public LivroResponse atualizar(Long id, LivroRequest entrada) {
        return controle.executar(() -> {
            encontrar(id);
            validarIsbn(entrada.isbn(), id);
            if (entrada.quantidadeTotal() < ativos(id)) {
                throw new RegraNegocioException("A quantidade total não pode ser menor que os empréstimos ativos");
            }
            return resposta(livros.save(modelo(id, entrada)));
        });
    }

    public void remover(Long id) {
        controle.executar(() -> {
            encontrar(id);
            if (ativos(id) > 0) {
                throw new RegraNegocioException("Livro com empréstimo ativo não pode ser removido");
            }
            livros.deleteById(id);
            return null;
        });
    }

    private Livro encontrar(Long id) {
        return livros.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Livro " + id + " não encontrado"));
    }

    private long ativos(Long id) {
        return emprestimos.findAll().stream().filter(e -> e.ativo() && e.livroId().equals(id)).count();
    }

    private void validarIsbn(String isbn, Long id) {
        if (livros.findAll().stream().anyMatch(l -> l.isbn().equals(isbn.strip())
                && !Objects.equals(l.id(), id))) {
            throw new RegraNegocioException("Já existe livro com o ISBN informado");
        }
    }

    private Livro modelo(Long id, LivroRequest e) {
        return new Livro(id, e.titulo().strip(), e.autor().strip(), e.isbn().strip(),
                e.anoPublicacao(), e.quantidadeTotal());
    }

    private LivroResponse resposta(Livro l) {
        return new LivroResponse(l.id(), l.titulo(), l.autor(), l.isbn(),
                l.anoPublicacao(), l.quantidadeTotal(), l.quantidadeTotal() - ativos(l.id()));
    }
}
