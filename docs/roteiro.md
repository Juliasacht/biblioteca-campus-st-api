# Roteiro para compreender e apresentar

## 1. Comece pelos modelos e DTOs

Leia `model/Livro.java` e `dto/LivroRequest.java`.
O modelo é o dado interno; o DTO descreve o que a API aceita.
`LivroResponse` acrescenta disponibilidade calculada.
Um record oferece construtor, acessores e igualdade sem código repetitivo.

## 2. Siga uma requisição de cadastro

Em `LivroController.criar`, `@Valid` verifica o DTO recebido no JSON.
O controller chama `LivroService.criar`.
O serviço verifica ISBN e solicita `LivroRepository.save`.
O Spring injeta `LivroRepositoryMemoria`, que gera o ID e armazena o modelo.
O serviço retorna um DTO; o controller responde 201 com o cabeçalho Location.

O controller não calcula disponibilidade nem aplica regras de domínio.

## 3. Explique a persistência em memória

`CrudRepository` define operações comuns.
Cada recurso tem sua interface e implementação concreta.
`RepositoryMemoria` centraliza mapa e geração de IDs, reaproveitando o comportamento.
`Optional` representa uma busca que pode não encontrar o recurso.
`orElseThrow` transforma ausência em exceção de domínio.

No próximo bloco, outra implementação poderá substituir a memória mantendo os contratos.

## 4. Mostre as regras de empréstimo

Em `EmprestimoService.criar`:

1. Confere livro e leitor.
2. Obtém a data pelo Clock e a política pelo mapa.
3. Lista os empréstimos ativos.
4. Verifica estoque, limite e atrasos.
5. Define vencimento e salva o empréstimo.

`PoliticaAluno` e `PoliticaProfessor` implementam a mesma interface.
O serviço não escolhe uma regra com if/switch por tipo: usa `politicas.get(leitor.tipo())`.
Os valores vêm do YAML pelos construtores das políticas.

## 5. Demonstre a devolução

`devolver` rejeita segunda devolução.
Calcula os dias entre vencimento e hoje, limitando a zero.
A política multiplica os dias pela diária.
O modelo é substituído por outro com data de devolução e multa.
Como disponibilidade é calculada pelos ativos, o exemplar volta a ficar disponível.

## 6. Mostre os erros

`ApiExceptionHandler` transforma:

- `RecursoNaoEncontradoException` em 404.
- `RegraNegocioException` em 409.
- Bean Validation em 400 com mapa `errors`.
- JSON e parâmetros incompatíveis em 400.

Use o Postman para visualizar o Content-Type e o corpo ProblemDetail.

## 7. Demonstração sugerida

1. Inicie a aplicação e liste livros/leitores.
2. Mostre o empréstimo inicial no relatório de atrasados.
3. Tente novo empréstimo para Ana: 409.
4. Devolva o empréstimo 1: multa de R$ 3,00.
5. Devolva novamente: 409.
6. Cadastre livro e leitor; observe 201 e Location.
7. Realize empréstimo e confira prazo, título e nome.
8. Tente excluir livro emprestado: 409.
9. Devolva e então exclua: 204.
10. Envie ISBN inválido: 400 com mapa de erros.
11. Consulte um ID inexistente: 404.
12. Mostre os testes aprovados na aba Actions.

## Perguntas para treinar

- Por que o controller não acessa o Repository diretamente?
- O que muda se a persistência deixar de ser em memória?
- Por que usar DTOs separados dos modelos?
- Como adicionar um terceiro tipo de leitor?
- Qual vantagem o Clock oferece aos testes?
- Por que multa usa BigDecimal?
- Por que empréstimo devolvido não ocupa exemplar?
- O que acontece com os dados após reiniciar?
