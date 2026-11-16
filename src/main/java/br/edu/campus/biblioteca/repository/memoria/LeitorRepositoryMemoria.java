package br.edu.campus.biblioteca.repository.memoria;

import br.edu.campus.biblioteca.model.Leitor;
import br.edu.campus.biblioteca.repository.LeitorRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LeitorRepositoryMemoria extends RepositoryMemoria<Leitor> implements LeitorRepository {
    public LeitorRepositoryMemoria() {
        super(Leitor::id, (id, e) -> new Leitor(id, e.nome(), e.email(), e.tipo()));
    }
}
