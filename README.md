# Biblioteca do Campus — API REST

![Testes Java](https://github.com/Juliasacht/biblioteca-campus-st-api/actions/workflows/testes.yml/badge.svg)

Mini-projeto individual de Tópicos Especiais II — Programação Avançada (Java + Spring Boot).
API para livros, leitores e empréstimos, com armazenamento em memória.

**Integrante:** Julia de Souza Sacht — [@Juliasacht](https://github.com/Juliasacht).

## Tecnologias

- Java 21, Maven Wrapper e Spring Boot 4.0.9.
- Spring Web MVC e Bean Validation.
- JUnit 5, testes HTTP com servidor real e GitHub Actions.
- Coleção Postman com 36 requisições e verificações de resposta.

A base Maven é obtida do [Spring Initializr](https://start.spring.io/) com Java 21 e dependências Web e Validation.
O workflow [Base Spring Initializr](.github/workflows/base-inicial.yml) permite obter e conferir a base original.
Ele tem somente permissão de leitura e não altera o repositório.

## Executar

É necessário ter um JDK 21 instalado e configurado em `JAVA_HOME`.
Na primeira execução, o Wrapper baixa o Maven e as dependências; é necessário acesso à internet.

```powershell
git clone https://github.com/Juliasacht/biblioteca-campus-st-api.git
cd biblioteca-campus-st-api
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

Endereço: `http://localhost:8080`. Para conferir a execução, acesse `http://localhost:8080/livros`.
A API não possui página inicial; use os endpoints ou o Postman. Para encerrar, pressione Ctrl+C.

No IntelliJ IDEA, abra o `pom.xml`, importe como projeto Maven, selecione o JDK 21 e execute
`BibliotecaApplication.main()`. Confira também o JDK do importador e do executor Maven.

## Testar

Windows:

```powershell
.\mvnw.cmd verify
```

Linux/macOS:

```bash
./mvnw verify
```

Os 19 testes automatizados verificam RN01–RN10, CRUD, filtros, validações, status HTTP, Location,
estoque, multas, prazos e preservação do histórico. O relógio é fixo nos testes.
A carga inicial é desabilitada durante os testes HTTP; cada teste limpa seu estado.

A aba **Actions** apresenta a compilação e os testes de cada alteração.

## Postman

1. Inicie a aplicação com os dados iniciais.
2. Importe `postman/biblioteca.postman_collection.json`.
3. Confira a variável de coleção `baseUrl`, inicialmente `http://localhost:8080`.
4. Execute a coleção completa, em ordem, pelo Collection Runner.

A coleção cobre os 15 endpoints, com respostas de sucesso e erros 400, 404 e 409.
Os IDs dos novos cadastros são capturados automaticamente.
Ela usa a carga inicial para demonstrar atraso e devolução, e altera os dados.
**Reinicie a aplicação antes de repetir a coleção completa.**
Requisições isoladas com variáveis dependem das requisições anteriores.

## Endpoints

| Método | URI | Descrição | Sucesso |
|---|---|---|---|
| POST | /livros | Cadastrar livro | 201 + Location |
| GET | /livros | Listar; filtros `autor` e `disponivel` | 200 |
| GET | /livros/{id} | Consultar livro | 200 |
| PUT | /livros/{id} | Atualizar livro | 200 |
| DELETE | /livros/{id} | Excluir livro | 204 |
| POST | /leitores | Cadastrar leitor | 201 + Location |
| GET | /leitores | Listar; filtro `tipo` | 200 |
| GET | /leitores/{id} | Consultar leitor | 200 |
| PUT | /leitores/{id} | Atualizar leitor | 200 |
| DELETE | /leitores/{id} | Excluir leitor | 204 |
| POST | /emprestimos | Realizar empréstimo | 201 + Location |
| GET | /emprestimos | Listar; filtros `leitorId` e `ativos` | 200 |
| GET | /emprestimos/{id} | Consultar empréstimo | 200 |
| POST | /emprestimos/{id}/devolucao | Devolver e calcular multa | 200 |
| GET | /emprestimos/atrasados | Listar ativos vencidos | 200 |

Filtros podem ser combinados. Autor é pesquisado por trecho, sem diferenciar maiúsculas de minúsculas.
`disponivel=true` retorna livros com exemplares disponíveis; `false`, os indisponíveis.
`ativos=false` retorna empréstimos devolvidos.
Listagens sem resultados retornam uma lista vazia com status 200.
Tipo aceita exatamente `ALUNO` ou `PROFESSOR`.

### Cadastrar um livro

`POST /livros`

```json
{
  "titulo": "Livro de Exemplo",
  "autor": "Autor de Exemplo",
  "isbn": "9781234567890",
  "anoPublicacao": 2024,
  "quantidadeTotal": 3
}
```

### Cadastrar um leitor

`POST /leitores`

```json
{
  "nome": "Maria Silva",
  "email": "maria@campus.edu.br",
  "tipo": "ALUNO"
}
```

### Realizar um empréstimo

`POST /emprestimos`

```json
{
  "livroId": 2,
  "leitorId": 2
}
```

A resposta inclui ID, livroId, titulo, leitorId, leitor, dataEmprestimo,
dataPrevistaDevolucao, dataDevolucao e multa.
A devolução é um POST sem corpo para `/emprestimos/{id}/devolucao`.

## Regras de negócio

| Código | Implementação | Violação |
|---|---|---|
| RN01 | ISBN único, inclusive nas atualizações | 409 |
| RN02 | E-mail único, ignorando maiúsculas/minúsculas | 409 |
| RN03 | Exige quantidade total maior que o número de empréstimos ativos do livro | 409 |
| RN04 | Limite de ativos: aluno 3; professor 5 | 409 |
| RN05 | Leitor com empréstimo ativo vencido não pode fazer novo empréstimo | 409 |
| RN06 | Prazo: aluno 7 dias; professor 14 dias | — |
| RN07 | Multa na devolução: dias de atraso × diária; aluno R$ 1,00, professor R$ 0,50 | — |
| RN08 | Não permite devolver duas vezes | 409 |
| RN09 | Não exclui livro/leitor com empréstimo ativo | 409 |
| RN10 | Livro, leitor ou empréstimo inexistente | 404 |

Prazo, limite e diária vêm de `src/main/resources/application.yml`, lidos com `@Value`.
A multa usa `BigDecimal`, com duas casas decimais.
O dia previsto para devolução ainda está dentro do prazo; o atraso começa no dia seguinte.
Enquanto o empréstimo está ativo, multa permanece zero; ela é calculada na devolução.

## Validação e erros

DTOs de entrada usam Bean Validation e os controllers aplicam `@Valid`:

- Título obrigatório, até 150 caracteres; autor obrigatório, até 100.
- ISBN obrigatório, exatamente 13 dígitos numéricos.
- Ano obrigatório, a partir de 1450; quantidade obrigatória e maior que zero.
- Nome obrigatório, até 100 caracteres; e-mail obrigatório em formato válido.
- Tipo obrigatório; IDs de livro e leitor obrigatórios e maiores que zero.

Erros usam `application/problem+json`. Um único `@RestControllerAdvice` estende
`ResponseEntityExceptionHandler` e trata domínio e erros MVC.

Exemplo de validação:

```json
{
  "type": "about:blank",
  "title": "Erro de validação",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos",
  "instance": "/livros",
  "erros": {
    "isbn": "O ISBN deve ter 13 dígitos"
  }
}
```

## Estrutura e decisões

| Pacote | Responsabilidade |
|---|---|
| controller | Rotas, validação de entrada e respostas HTTP |
| service | Regras de negócio e conversão entre modelos e DTOs |
| service.politica | Strategy por tipo de leitor |
| repository | Contratos de persistência |
| repository.memoria | Implementações em memória |
| model | Modelos de domínio e enum de tipos |
| dto | Records específicos de entrada e saída |
| exception | Exceções de domínio e tratamento centralizado |
| config | Clock e carga inicial |

- Injeção por construtor; não há injeção em atributos.
- Interfaces Repository oferecem `findAll`, `findById`, `save`, `deleteById` e `existsById`.
- Buscas individuais retornam `Optional`; filtros e relatórios usam Streams.
- DTOs de resposta são distintos dos modelos, mesmo sendo ambos records.
- Modelos imutáveis evitam mudanças acidentais fora dos repositories.
- IDs são gerados por sequência atômica; as listagens são ordenadas por ID.
- As políticas são injetadas como lista e convertidas em mapa por tipo.
  Não há `if` ou `switch` por tipo de leitor no serviço de empréstimos.
- O bean `Clock` usa o fuso `America/Sao_Paulo`; datas são obtidas com `LocalDate.now(clock)`.
- `ControleAcervo` serializa operações compostas na aplicação em memória, evitando que
  requisições simultâneas ultrapassem estoque ou limites.
- Não é permitido reduzir quantidade total abaixo dos empréstimos ativos (409).
- O tipo do leitor pode ser atualizado, desde que a quantidade de empréstimos ativos
  respeite o limite da nova política (409 se excedido).
- Cada empréstimo preserva o tipo do leitor na contratação. Atualizar o cadastro não
  altera o prazo nem a política de multa dos empréstimos já realizados.
- Título e nome são registrados no empréstimo como fotografia do momento da operação.
  O histórico continua legível após exclusões permitidas de livros/leitores.
- Os dados não são persistidos: reiniciar apaga as alterações e recria a carga inicial.

## Carga inicial e demonstração

| Recurso | ID | Dados |
|---|---:|---|
| Livro | 1 | Código Limpo; 3 exemplares |
| Livro | 2 | Java: Como Programar; 2 exemplares |
| Livro | 3 | Engenharia de Software; 2 exemplares |
| Leitor | 1 | Ana Souza; ALUNO; ana@campus.edu.br |
| Leitor | 2 | Carlos Lima; PROFESSOR; carlos@campus.edu.br |
| Empréstimo | 1 | Livro 1, leitor 1, vencido há 3 dias |

A carga usa `CommandLineRunner` e insere o empréstimo atrasado diretamente no Repository,
como solicitado no enunciado. As datas são relativas ao Clock, não fixadas no calendário.

Para demonstrar: liste `/emprestimos/atrasados`; tente emprestar para o leitor 1 (409);
devolva o empréstimo 1 (multa R$ 3,00); tente devolvê-lo novamente (409).
Para desabilitar a carga inicial, use `biblioteca.carga-inicial: false`.


