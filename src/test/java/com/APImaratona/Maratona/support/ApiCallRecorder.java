package com.APImaratona.Maratona.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acumula as requisicoes/respostas capturadas pelos testes de controller e, quando a JVM
 * dos testes termina (fim do `mvn test`), grava um relatorio HTML consolidado em
 * target/api-test-report/relatorio.html.
 */
public final class ApiCallRecorder {

    private static final List<ApiCallRecord> REGISTROS = Collections.synchronizedList(new ArrayList<>());
    private static final Path RELATORIO = Path.of("target", "api-test-report", "relatorio.html");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ApiCallRecorder::gravarRelatorio));
    }

    private ApiCallRecorder() {
    }

    public static void registrar(ApiCallRecord registro) {
        REGISTROS.add(registro);
    }

    private static void gravarRelatorio() {
        if (REGISTROS.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(RELATORIO.getParent());
            Files.writeString(RELATORIO, ApiHtmlReportBuilder.gerar(REGISTROS));
            System.out.println("[api-test-report] Relatorio das requisicoes gerado em: " + RELATORIO.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[api-test-report] Falha ao gerar relatorio: " + e.getMessage());
        }
    }
}
