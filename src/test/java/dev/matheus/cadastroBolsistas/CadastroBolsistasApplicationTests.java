package dev.matheus.cadastroBolsistas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * a suite nao sobe banco. duas coisas sao necessarias para isso:
 *
 * - flyway desligado, senao ele tenta migrar no startup;
 * - dialect declarado na mao, senao o hibernate abre conexao so para descobrir
 *   qual e o dialect a partir da metadata do jdbc.
 *
 * o datasource ainda e criado, mas o hikari so conecta quando alguem pede -
 * e ninguem pede.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@AutoConfigureMockMvc
class CadastroBolsistasApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void exportarOpenApiParaPostman() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Path caminho = Path.of("postman", "SisBolsa-API.postman.json");
        Files.writeString(caminho, json);
    }
}
