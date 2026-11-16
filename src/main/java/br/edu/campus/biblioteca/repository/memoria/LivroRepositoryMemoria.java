package br.edu.campus.biblioteca.repository.memoria;

import br.edu.campus.biblioteca.model.Livro;
import br.edu.campus.biblioteca.repository.LivroRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LivroRepositoryMemoria extends RepositoryMemoria<Livro> implements LivroRepository {
    public LivroRepositoryMemoria() {
        super(Livro::id, (id, e) -> new Livro(id, e.titulo(), e.autor(), e.isbn(), e.anoPublicacao(), e.quantidadeTotal()));
    }
}
