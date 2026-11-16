package br.edu.campus.biblioteca.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

// Serializa consultas e alterações compostas no acervo em memória.
@Component
public class ControleAcervo {
    public synchronized <T> T executar(Supplier<T> operacao) {
        return operacao.get();
    }
}
