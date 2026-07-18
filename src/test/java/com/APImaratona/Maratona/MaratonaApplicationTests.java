package com.APImaratona.Maratona;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sobe o contexto completo (Postgres, Mongo, Neo4j, Redis, JWT_SECRET reais via env vars).
// Marcado como "integration" e excluido do `./mvnw test` padrao (ver excludedGroups no
// maven-surefire-plugin) para nao quebrar quando essa infra nao estiver disponivel;
// roda sob demanda com `./mvnw test -DexcludedGroups=` quando a infra existir.
@Tag("integration")
@SpringBootTest
class MaratonaApplicationTests {

	@Test
	void contextLoads() {
	}

}
