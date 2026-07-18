package com.APImaratona.Maratona.support;

import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ApiHtmlReportBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiHtmlReportBuilder() {
    }

    static String gerar(List<ApiCallRecord> registros) {
        Map<String, List<ApiCallRecord>> porControlador = new LinkedHashMap<>();
        for (ApiCallRecord r : registros) {
            porControlador.computeIfAbsent(r.controlador(), k -> new java.util.ArrayList<>()).add(r);
        }

        long sucesso = registros.stream().filter(r -> r.status() < 400).count();
        long falha = registros.size() - sucesso;

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Relatorio de Requisicoes - API Maratona</title>")
                .append("<style>").append(CSS).append("</style></head><body>");

        html.append("<header><h1>Relatorio de Requisicoes - API Maratona</h1>")
                .append("<div class=\"resumo\">")
                .append("<span class=\"badge total\">Total: ").append(registros.size()).append("</span>")
                .append("<span class=\"badge ok\">2xx/3xx: ").append(sucesso).append("</span>")
                .append("<span class=\"badge erro\">4xx/5xx: ").append(falha).append("</span>")
                .append("<span class=\"acoes\">")
                .append("<button id=\"expandir-todos\">Expandir todos</button>")
                .append("<button id=\"recolher-todos\">Recolher todos</button>")
                .append("</span>")
                .append("</div></header><main>");

        for (Map.Entry<String, List<ApiCallRecord>> grupo : porControlador.entrySet()) {
            html.append("<section class=\"controlador\"><h2>").append(escapar(grupo.getKey())).append("</h2>");
            for (ApiCallRecord r : grupo.getValue()) {
                html.append(cardRequisicao(r));
            }
            html.append("</section>");
        }

        html.append("</main>")
                .append("<script>").append(JS).append("</script>")
                .append("</body></html>");

        return html.toString();
    }

    private static String cardRequisicao(ApiCallRecord r) {
        String statusClasse = r.status() < 300 ? "s2xx" : r.status() < 400 ? "s3xx" : r.status() < 500 ? "s4xx" : "s5xx";

        StringBuilder sb = new StringBuilder();
        sb.append("<details class=\"chamada\"><summary class=\"cabecalho\">");
        sb.append("<span class=\"metodo m-").append(r.metodo().toLowerCase()).append("\">").append(escapar(r.metodo())).append("</span>");
        sb.append("<span class=\"url\">").append(escapar(r.url())).append("</span>");
        sb.append("<span class=\"cenario\">").append(escapar(r.cenario())).append("</span>");
        sb.append("<span class=\"status ").append(statusClasse).append("\">").append(r.status()).append("</span>");
        sb.append("<span class=\"duracao\">").append(r.duracaoMs()).append(" ms</span>");
        sb.append("</summary>");

        sb.append("<div class=\"detalhes\">");
        sb.append("<div class=\"coluna\"><h3>Requisicao</h3>");
        sb.append(headersHtml(r.requestHeaders()));
        sb.append(corpoHtml(r.requestBody()));
        sb.append("</div>");
        sb.append("<div class=\"coluna\"><h3>Resposta</h3>");
        sb.append(headersHtml(r.responseHeaders()));
        sb.append(corpoHtml(r.responseBody()));
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</details>");
        return sb.toString();
    }

    private static String headersHtml(Map<String, String> headers) {
        if (headers.isEmpty()) {
            return "<p class=\"vazio\">Sem headers</p>";
        }
        StringBuilder sb = new StringBuilder("<table class=\"headers\">");
        for (Map.Entry<String, String> h : headers.entrySet()) {
            sb.append("<tr><td>").append(escapar(h.getKey())).append("</td><td>").append(escapar(h.getValue())).append("</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private static String corpoHtml(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return "<p class=\"vazio\">Sem corpo</p>";
        }
        return "<pre class=\"corpo\">" + escapar(formatarSePossivel(corpo)) + "</pre>";
    }

    private static String formatarSePossivel(String corpo) {
        try {
            Object json = MAPPER.readValue(corpo, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return corpo;
        }
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String CSS = """
            :root {
              color-scheme: light dark;
              --bg: #f7f7f9; --bg-card: #ffffff; --border: #e2e2e6;
              --text: #1b1b1f; --text-muted: #6b6b76; --mono-bg: #f0f0f3;
            }
            @media (prefers-color-scheme: dark) {
              :root { --bg: #17171a; --bg-card: #211f26; --border: #34333a; --text: #ececef; --text-muted: #9c9ba3; --mono-bg: #17171a; }
            }
            * { box-sizing: border-box; }
            body { margin:0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: var(--bg); color: var(--text); }
            header { padding: 24px 32px; border-bottom: 1px solid var(--border); position: sticky; top:0; background: var(--bg); z-index: 10; }
            header h1 { margin: 0 0 12px; font-size: 20px; }
            .resumo { display:flex; gap:8px; align-items:center; flex-wrap: wrap; }
            .badge { padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600; }
            .badge.total { background: #e5e7ff; color:#3730a3; }
            .badge.ok { background: #dcfce7; color:#166534; }
            .badge.erro { background: #fee2e2; color:#991b1b; }
            .acoes { margin-left:auto; display:flex; gap:8px; }
            .acoes button { border:1px solid var(--border); background: var(--bg-card); color: var(--text); border-radius:6px; padding:6px 12px; cursor:pointer; font-size:12px; }
            .acoes button:hover { border-color:#8b8b96; }
            main { padding: 24px 32px 64px; max-width: 1100px; margin: 0 auto; }
            .controlador h2 { font-size: 16px; margin: 32px 0 12px; color: var(--text-muted); text-transform: uppercase; letter-spacing: .04em; }
            details.chamada { background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; margin-bottom: 10px; overflow:hidden; }
            summary.cabecalho { list-style:none; cursor:pointer; display:flex; align-items:center; gap:12px; padding: 12px 16px; }
            summary.cabecalho::-webkit-details-marker { display:none; }
            .metodo { font-family: ui-monospace, SFMono-Regular, monospace; font-weight:700; font-size:12px; padding:3px 8px; border-radius:5px; min-width:52px; text-align:center; }
            .m-get{ background:#dbeafe; color:#1e40af; }
            .m-post{ background:#dcfce7; color:#166534; }
            .m-put{ background:#fef3c7; color:#92400e; }
            .m-delete{ background:#fee2e2; color:#991b1b; }
            .url { font-family: ui-monospace, SFMono-Regular, monospace; font-size:13px; flex:1; overflow-wrap: anywhere; }
            .cenario { color: var(--text-muted); font-size: 12px; flex-basis: 260px; }
            .status { font-family: ui-monospace, monospace; font-weight:700; font-size:12px; padding:3px 8px; border-radius:5px; }
            .s2xx,.s3xx { background:#dcfce7; color:#166534; }
            .s4xx { background:#fef3c7; color:#92400e; }
            .s5xx { background:#fee2e2; color:#991b1b; }
            .duracao { color: var(--text-muted); font-size:12px; min-width:60px; text-align:right; }
            .detalhes { display:grid; grid-template-columns: 1fr 1fr; gap:16px; padding: 4px 16px 16px; border-top:1px solid var(--border); }
            .coluna h3 { font-size:12px; text-transform:uppercase; letter-spacing:.04em; color:var(--text-muted); margin: 12px 0 6px; }
            table.headers { width:100%; border-collapse: collapse; font-size:12px; margin-bottom:8px; }
            table.headers td { padding:3px 6px; border-bottom:1px solid var(--border); vertical-align:top; word-break: break-all; }
            table.headers td:first-child { color: var(--text-muted); width:38%; }
            pre.corpo { background: var(--mono-bg); border-radius:6px; padding:10px; font-size:12px; overflow-x:auto; white-space:pre-wrap; word-break: break-word; margin:0; }
            p.vazio { color: var(--text-muted); font-size:12px; font-style: italic; margin:0 0 8px; }
            @media (max-width: 720px){ .detalhes{ grid-template-columns: 1fr; } .cenario{ flex-basis:auto; } }
            """;

    private static final String JS = """
            document.getElementById('expandir-todos').addEventListener('click', () => {
              document.querySelectorAll('details.chamada').forEach(d => d.open = true);
            });
            document.getElementById('recolher-todos').addEventListener('click', () => {
              document.querySelectorAll('details.chamada').forEach(d => d.open = false);
            });
            """;
}
