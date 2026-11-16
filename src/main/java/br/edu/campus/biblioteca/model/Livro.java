package br.edu.campus.biblioteca.model;

// Modelo imutável interno; a API utiliza LivroResponse.
public record Livro(Long id, String titulo, String autor, String isbn,
                    Integer anoPublicacao, Integer quantidadeTotal) {}
