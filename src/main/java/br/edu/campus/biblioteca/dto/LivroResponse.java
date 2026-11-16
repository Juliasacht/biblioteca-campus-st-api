package br.edu.campus.biblioteca.dto;

public record LivroResponse(Long id, String titulo, String autor, String isbn,
                            Integer anoPublicacao, Integer quantidadeTotal,
                            long quantidadeDisponivel) {}
