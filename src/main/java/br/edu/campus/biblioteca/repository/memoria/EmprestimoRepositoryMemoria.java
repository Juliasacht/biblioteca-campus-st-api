package br.edu.campus.biblioteca.repository.memoria;

import br.edu.campus.biblioteca.model.Emprestimo;
import br.edu.campus.biblioteca.repository.EmprestimoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EmprestimoRepositoryMemoria extends RepositoryMemoria<Emprestimo> implements EmprestimoRepository {
    public EmprestimoRepositoryMemoria() {
        super(Emprestimo::id, (id, e) -> new Emprestimo(id, e.livroId(), e.leitorId(), e.dataEmprestimo(), e.dataPrevistaDevolucao(), e.dataDevolucao(), e.multa()));
    }
}
