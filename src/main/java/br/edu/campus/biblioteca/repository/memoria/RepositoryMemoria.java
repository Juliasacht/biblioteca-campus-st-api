package br.edu.campus.biblioteca.repository.memoria;

import br.edu.campus.biblioteca.repository.CrudRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class RepositoryMemoria<T> implements CrudRepository<T> {
    private final ConcurrentHashMap<Long, T> dados = new ConcurrentHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();
    private final Function<T, Long> obterId;
    private final BiFunction<Long, T, T> atribuirId;

    protected RepositoryMemoria(Function<T, Long> obterId, BiFunction<Long, T, T> atribuirId) {
        this.obterId = obterId;
        this.atribuirId = atribuirId;
    }

    @Override
    public List<T> findAll() {
        return dados.values().stream().sorted(Comparator.comparing(obterId)).toList();
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public T save(T entidade) {
        Long id = obterId.apply(entidade);
        if (id == null) {
            id = sequencia.incrementAndGet();
        } else {
            sequencia.accumulateAndGet(id, Math::max);
        }
        T salva = atribuirId.apply(id, entidade);
        dados.put(id, salva);
        return salva;
    }

    @Override
    public void deleteById(Long id) {
        dados.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return dados.containsKey(id);
    }
}
