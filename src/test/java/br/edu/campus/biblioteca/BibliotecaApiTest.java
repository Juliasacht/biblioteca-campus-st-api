package br.edu.campus.biblioteca;

import br.edu.campus.biblioteca.model.*;
import br.edu.campus.biblioteca.repository.*;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "biblioteca.carga-inicial=false")
@Import(BibliotecaApiTest.TempoConfig.class)
class BibliotecaApiTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);
    private final LivroRepository livros;
    private final LeitorRepository leitores;
    private final EmprestimoRepository emprestimos;
    private final String base;
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    BibliotecaApiTest(LivroRepository livros, LeitorRepository leitores,
            EmprestimoRepository emprestimos, @Value("${local.server.port}") int porta) {
        this.livros = livros;
        this.leitores = leitores;
        this.emprestimos = emprestimos;
        this.base = "http://localhost:" + porta;
    }

    @TestConfiguration
    static class TempoConfig {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @BeforeEach
    void limpar() {
        emprestimos.findAll().forEach(e -> emprestimos.deleteById(e.id()));
        livros.findAll().forEach(l -> livros.deleteById(l.id()));
        leitores.findAll().forEach(l -> leitores.deleteById(l.id()));
    }

    @Test
    void crudLivroComLocationFiltrosE204() throws Exception {
        var criado = enviar("POST", "/livros", livroJson("9788576082675", 3));
        assertEquals(201, criado.statusCode());
        long id = id(criado);
        assertTrue(criado.headers().firstValue("Location").orElseThrow().endsWith("/livros/" + id));
        assertEquals(3, ((Number) campo(criado, "$.quantidadeDisponivel")).intValue());
        assertEquals(200, enviar("GET", "/livros/" + id, null).statusCode());
        var filtrado = enviar("GET", "/livros?autor=martin&disponivel=true", null);
        assertEquals(1, ((java.util.List<?>) campo(filtrado, "$")).size());
        assertEquals(0, ((java.util.List<?>) campo(enviar("GET", "/livros?autor=inexistente", null), "$")).size());
        assertEquals(200, enviar("PUT", "/livros/" + id, livroJson("9788576082675", 4)).statusCode());
        var excluido = enviar("DELETE", "/livros/" + id, null);
        assertEquals(204, excluido.statusCode());
        assertEquals("", excluido.body());
        assertEquals(404, enviar("GET", "/livros/" + id, null).statusCode());
    }

    @Test
    void crudLeitorComFiltroETipo() throws Exception {
        var r = enviar("POST", "/leitores", leitorJson("ana@campus.edu.br", "ALUNO"));
        assertEquals(201, r.statusCode());
        long id = id(r);
        assertTrue(r.headers().firstValue("Location").orElseThrow().endsWith("/leitores/" + id));
        assertEquals(200, enviar("GET", "/leitores/" + id, null).statusCode());
        assertEquals(1, ((java.util.List<?>) campo(enviar("GET", "/leitores?tipo=ALUNO", null), "$")).size());
        assertEquals(0, ((java.util.List<?>) campo(enviar("GET", "/leitores?tipo=PROFESSOR", null), "$")).size());
        assertEquals(200, enviar("PUT", "/leitores/" + id, leitorJson("novo@campus.edu.br", "PROFESSOR")).statusCode());
        assertEquals(204, enviar("DELETE", "/leitores/" + id, null).statusCode());
    }

    @Test
    void rn01IsbnDuplicadoNoCadastroENaAtualizacao() throws Exception {
        long primeiro = id(enviar("POST", "/livros", livroJson("9788576082675", 3)));
        conflito(enviar("POST", "/livros", livroJson("9788576082675", 3)));
        long segundo = id(enviar("POST", "/livros", livroJson("9788543004792", 3)));
        conflito(enviar("PUT", "/livros/" + segundo, livroJson("9788576082675", 3)));
        assertEquals(200, enviar("PUT", "/livros/" + primeiro, livroJson("9788576082675", 2)).statusCode());
    }

    @Test
    void rn02EmailDuplicadoIgnoraMaiusculas() throws Exception {
        long id = id(enviar("POST", "/leitores", leitorJson("ana@campus.edu.br", "ALUNO")));
        conflito(enviar("POST", "/leitores", leitorJson("ANA@campus.edu.br", "PROFESSOR")));
        long outro = id(enviar("POST", "/leitores", leitorJson("outro@campus.edu.br", "ALUNO")));
        conflito(enviar("PUT", "/leitores/" + outro, leitorJson("ana@campus.edu.br", "ALUNO")));
        assertEquals(200, enviar("PUT", "/leitores/" + id, leitorJson("ana@campus.edu.br", "ALUNO")).statusCode());
    }

    @Test
    void rn03ExemplarIndisponivelEDevolucaoLiberaEstoque() throws Exception {
        Livro l = livro(1);
        Leitor a = leitor(TipoLeitor.ALUNO);
        long emp = id(emprestar(l, a));
        conflito(emprestar(l, a));
        assertEquals(0, ((Number) campo(enviar("GET", "/livros/" + l.id(), null), "$.quantidadeDisponivel")).intValue());
        assertEquals(1, ((java.util.List<?>) campo(enviar("GET", "/livros?disponivel=false", null), "$")).size());
        assertEquals(200, enviar("POST", "/emprestimos/" + emp + "/devolucao", null).statusCode());
        assertEquals(201, emprestar(l, a).statusCode());
    }

    @Test
    void rn04LimiteAlunoTres() throws Exception {
        verificarLimite(TipoLeitor.ALUNO, 3);
    }

    @Test
    void rn04LimiteProfessorCinco() throws Exception {
        verificarLimite(TipoLeitor.PROFESSOR, 5);
    }

    @Test
    void rn05AtrasoBloqueiaNovoEmprestimo() throws Exception {
        Livro l = livro(5);
        Leitor a = leitor(TipoLeitor.ALUNO);
        historico(l, a, HOJE.minusDays(1), null);
        conflito(emprestar(l, a));
    }

    @Test
    void rn06PrazosAlunoEProfessor() throws Exception {
        Livro l = livro(5);
        var aluno = emprestar(l, leitor(TipoLeitor.ALUNO));
        var professor = emprestar(l, leitor(TipoLeitor.PROFESSOR));
        assertEquals(HOJE.toString(), campo(aluno, "$.dataEmprestimo"));
        assertEquals(HOJE.plusDays(7).toString(), campo(aluno, "$.dataPrevistaDevolucao"));
        assertEquals(HOJE.plusDays(14).toString(), campo(professor, "$.dataPrevistaDevolucao"));
        assertNotNull(campo(aluno, "$.titulo"));
        assertNotNull(campo(aluno, "$.leitor"));
        assertTrue(aluno.headers().firstValue("Location").isPresent());
    }

    @Test
    void rn07MultaAlunoProfessorEVencimentoHoje() throws Exception {
        Livro l = livro(10);
        for (TipoLeitor tipo : TipoLeitor.values()) {
            var a = leitor(tipo);
            Emprestimo e = historico(l, a, HOJE.minusDays(3), null);
            var r = enviar("POST", "/emprestimos/" + e.id() + "/devolucao", null);
            assertEquals(200, r.statusCode());
            assertEquals(tipo == TipoLeitor.ALUNO ? 3.0 : 1.5, ((Number) campo(r, "$.multa")).doubleValue());
            assertEquals(HOJE.toString(), campo(r, "$.dataDevolucao"));
        }
        var e = historico(l, leitor(TipoLeitor.ALUNO), HOJE, null);
        var r = enviar("POST", "/emprestimos/" + e.id() + "/devolucao", null);
        assertEquals(0.0, ((Number) campo(r, "$.multa")).doubleValue());
        var futuro = historico(l, leitor(TipoLeitor.PROFESSOR), HOJE.plusDays(2), null);
        assertEquals(0.0, ((Number) campo(enviar("POST", "/emprestimos/" + futuro.id() + "/devolucao", null), "$.multa")).doubleValue());
    }

    @Test
    void rn08NaoDevolveDuasVezes() throws Exception {
        long id = id(emprestar(livro(3), leitor(TipoLeitor.ALUNO)));
        assertEquals(200, enviar("POST", "/emprestimos/" + id + "/devolucao", null).statusCode());
        conflito(enviar("POST", "/emprestimos/" + id + "/devolucao", null));
    }

    @Test
    void rn09ExclusoesBloqueadasEAposDevolucaoHistoricoPreservado() throws Exception {
        Livro l = livro(3);
        Leitor a = leitor(TipoLeitor.ALUNO);
        long id = id(emprestar(l, a));
        conflito(enviar("DELETE", "/livros/" + l.id(), null));
        conflito(enviar("DELETE", "/leitores/" + a.id(), null));
        enviar("POST", "/emprestimos/" + id + "/devolucao", null);
        assertEquals(204, enviar("DELETE", "/livros/" + l.id(), null).statusCode());
        assertEquals(204, enviar("DELETE", "/leitores/" + a.id(), null).statusCode());
        var r = enviar("GET", "/emprestimos/" + id, null);
        assertEquals(200, r.statusCode());
        assertEquals(l.titulo(), campo(r, "$.titulo"));
        assertEquals(a.nome(), campo(r, "$.leitor"));
    }

    @Test
    void rn10RecursosInexistentes() throws Exception {
        for (String caminho : new String[]{"/livros/99999", "/leitores/99999", "/emprestimos/99999"}) {
            var r = enviar("GET", caminho, null);
            assertEquals(404, r.statusCode());
            assertEquals(caminho, campo(r, "$.instance"));
        }
        Livro l = livro(3);
        Leitor a = leitor(TipoLeitor.ALUNO);
        assertEquals(404, enviar("POST", "/emprestimos", "{\"livroId\":99999,\"leitorId\":" + a.id() + "}").statusCode());
        assertEquals(404, enviar("POST", "/emprestimos", "{\"livroId\":" + l.id() + ",\"leitorId\":99999}").statusCode());
        assertEquals(404, enviar("POST", "/emprestimos/99999/devolucao", null).statusCode());
    }

    @Test
    void relatorioAtrasadosEFiltros() throws Exception {
        Livro l = livro(10);
        Leitor a = leitor(TipoLeitor.ALUNO);
        Leitor b = leitor(TipoLeitor.PROFESSOR);
        historico(l, a, HOJE.minusDays(1), null);
        historico(l, b, HOJE, null);
        historico(l, b, HOJE.minusDays(5), HOJE.minusDays(2));
        var atrasados = enviar("GET", "/emprestimos/atrasados", null);
        assertEquals(200, atrasados.statusCode());
        assertEquals(1, ((java.util.List<?>) campo(atrasados, "$")).size());
        assertEquals(1, ((java.util.List<?>) campo(enviar("GET", "/emprestimos?ativos=false", null), "$")).size());
        assertEquals(1, ((java.util.List<?>) campo(enviar("GET", "/emprestimos?ativos=true&leitorId=" + b.id(), null), "$")).size());
    }

    @Test
    void validacao400ComMapaDeErrosEJsonInvalido() throws Exception {
        var r = enviar("POST", "/livros", "{\"titulo\":\"\",\"autor\":\"\",\"isbn\":\"abc\",\"anoPublicacao\":1400,\"quantidadeTotal\":0}");
        assertEquals(400, r.statusCode());
        assertTrue(r.headers().firstValue("Content-Type").orElseThrow().contains("application/problem+json"));
        assertNotNull(campo(r, "$.erros.isbn"));
        assertNotNull(campo(r, "$.erros.quantidadeTotal"));
        assertEquals(400, enviar("POST", "/leitores", leitorJson("invalido", "ALUNO")).statusCode());
        assertEquals(400, enviar("POST", "/leitores", leitorJson("a@b.com", "DIRETOR")).statusCode());
        assertEquals(400, enviar("POST", "/emprestimos", "{\"livroId\":0}").statusCode());
        assertEquals(400, enviar("POST", "/livros", "{}").statusCode());
        assertEquals(400, enviar("POST", "/livros", "{").statusCode());
        assertEquals(400, enviar("GET", "/leitores?tipo=DIRETOR", null).statusCode());
        assertEquals(400, enviar("GET", "/livros?disponivel=xyz", null).statusCode());
    }

    @Test
    void atualizacaoNaoReduzEstoqueAbaixoDosAtivos() throws Exception {
        Livro l = livro(4);
        Leitor a = leitor(TipoLeitor.ALUNO);
        emprestar(l, a);
        emprestar(l, a);
        conflito(enviar("PUT", "/livros/" + l.id(), livroJson(l.isbn(), 1)));
    }


    @Test
    void trocaDeTipoPreservaPrazoEMultaDaContratacao() throws Exception {
        Livro l = livro(10);
        for (TipoLeitor original : TipoLeitor.values()) {
            Leitor a = leitor(original);
            Emprestimo antigo = historico(l, a, HOJE.minusDays(3), null);
            TipoLeitor novoTipo = original == TipoLeitor.ALUNO ? TipoLeitor.PROFESSOR : TipoLeitor.ALUNO;

            var atualizado = enviar("PUT", "/leitores/" + a.id(), leitorJson(a.email(), novoTipo.name()));
            assertEquals(200, atualizado.statusCode());
            assertEquals(novoTipo.name(), campo(atualizado, "$.tipo"));
            var consulta = enviar("GET", "/emprestimos/" + antigo.id(), null);
            assertEquals(antigo.dataPrevistaDevolucao().toString(), campo(consulta, "$.dataPrevistaDevolucao"));

            var devolucao = enviar("POST", "/emprestimos/" + antigo.id() + "/devolucao", null);
            assertEquals(200, devolucao.statusCode());
            double multaEsperada = original == TipoLeitor.ALUNO ? 3.0 : 1.5;
            assertEquals(multaEsperada, ((Number) campo(devolucao, "$.multa")).doubleValue());

            var novo = emprestar(l, a);
            assertEquals(201, novo.statusCode());
            int novoPrazo = novoTipo == TipoLeitor.ALUNO ? 7 : 14;
            assertEquals(HOJE.plusDays(novoPrazo).toString(), campo(novo, "$.dataPrevistaDevolucao"));
        }
    }

    @Test
    void trocaDeTipoRespeitaLimiteDaNovaPolitica() throws Exception {
        Livro l = livro(10);
        Leitor a = leitor(TipoLeitor.PROFESSOR);
        for (int i = 0; i < 4; i++) {
            assertEquals(201, emprestar(l, a).statusCode());
        }
        conflito(enviar("PUT", "/leitores/" + a.id(), leitorJson(a.email(), "ALUNO")));
        assertEquals("PROFESSOR", campo(enviar("GET", "/leitores/" + a.id(), null), "$.tipo"));
    }

    private void verificarLimite(TipoLeitor tipo, int limite) throws Exception {
        Livro l = livro(10);
        Leitor a = leitor(tipo);
        for (int i = 0; i < limite; i++) {
            assertEquals(201, emprestar(l, a).statusCode());
        }
        conflito(emprestar(l, a));
    }

    private Livro livro(int quantidade) {
        return livros.save(new Livro(null, "Código Limpo", "Robert C. Martin", "9788576082675", 2009, quantidade));
    }

    private Leitor leitor(TipoLeitor tipo) {
        return leitores.save(new Leitor(null, "Pessoa " + tipo, tipo + "@campus.edu.br", tipo));
    }

    private Emprestimo historico(Livro l, Leitor a, LocalDate prazo, LocalDate devolucao) {
        return emprestimos.save(new Emprestimo(null, l.id(), a.id(), l.titulo(), a.nome(), a.tipo(),
                prazo.minusDays(14), prazo, devolucao, new BigDecimal("0.00")));
    }

    private HttpResponse<String> emprestar(Livro l, Leitor a) throws Exception {
        return enviar("POST", "/emprestimos", "{\"livroId\":" + l.id() + ",\"leitorId\":" + a.id() + "}");
    }

    private String livroJson(String isbn, int quantidade) {
        return "{\"titulo\":\"Código Limpo\",\"autor\":\"Robert C. Martin\",\"isbn\":\"" + isbn
                + "\",\"anoPublicacao\":2009,\"quantidadeTotal\":" + quantidade + "}";
    }

    private String leitorJson(String email, String tipo) {
        return "{\"nome\":\"Ana Souza\",\"email\":\"" + email + "\",\"tipo\":\"" + tipo + "\"}";
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String json) throws Exception {
        var corpo = json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json);
        var r = HttpRequest.newBuilder(URI.create(base + caminho))
                .header("Content-Type", "application/json").method(metodo, corpo).build();
        return http.send(r, HttpResponse.BodyHandlers.ofString());
    }

    private Object campo(HttpResponse<String> r, String expressao) {
        return JsonPath.read(r.body(), expressao);
    }

    private long id(HttpResponse<String> r) {
        assertEquals(201, r.statusCode(), r.body());
        return ((Number) campo(r, "$.id")).longValue();
    }

    private void conflito(HttpResponse<String> r) {
        assertEquals(409, r.statusCode(), r.body());
        assertEquals("Conflict", campo(r, "$.title"));
    }
}
