package dev.matheus.cadastroBolsistas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
class CadastroBolsistasApplicationTests {

	@Test
	void contextLoads() {
	}

}
