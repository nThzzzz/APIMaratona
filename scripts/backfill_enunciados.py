#!/usr/bin/env python3
"""Preenche o campo `descricao` dos problemas ja salvos no Mongo.

Existe porque o Codeforces bloqueia no handshake TLS, nao no header: Jsoup e curl
levam 403 mesmo mandando o jogo completo de headers de browser. O Scrapling passa
porque imita o fingerprint TLS do Chrome -- e o fetcher leve basta, nao precisa de
navegador (`scrapling install`) nenhum.

E backfill de proposito, nao trabalho de request: o enunciado de um problema do
Codeforces nao muda depois de publicado, entao se busca uma vez e pronto. A API em
Java nao faz scraping nenhum.

    pip install -r scripts/requirements.txt
    python scripts/backfill_enunciados.py --limite 5 --dry-run   # ve o que faria
    python scripts/backfill_enunciados.py                        # roda pra valer

O MONGO_URI e o mesmo do docker-compose. O banco e "maratona" porque e o que o
MongoConfig fixa no MongoTemplate, independente do que vier na URI.
"""

import argparse
import os
import re
import sys
import time

from pymongo import MongoClient
from scrapling.fetchers import Fetcher

# "4A" -> contest 4, index A. O index pode ter numero ("1234A1"), por isso ele tem
# que comecar por letra em vez de separar so digito de nao-digito.
ID_PROBLEMA = re.compile(r"^(\d+)([A-Za-z]\d*)$")

# Os dois placeholders que o extrairTexto gravava quando o scraping falhava.
FALTA_ENUNCIADO = {
    "$or": [
        {"descricao": {"$exists": False}},
        {"descricao": None},
        {"descricao": ""},
        {"descricao": {"$regex": "Texto indispon", "$options": "i"}},
    ]
}


def url_do_problema(id_problema):
    achou = ID_PROBLEMA.match(id_problema)
    if not achou:
        return None
    contest, index = achou.groups()
    return f"https://codeforces.com/problemset/problem/{contest}/{index}"


def buscar_enunciado(url, timeout):
    """Devolve (titulo, html_do_enunciado). Levanta excecao se nao conseguir."""
    pagina = Fetcher.get(url, stealthy_headers=True, timeout=timeout)

    if pagina.status != 200:
        raise RuntimeError(f"HTTP {pagina.status}")

    enunciado = pagina.css("div.problem-statement")
    if not enunciado:
        raise RuntimeError("pagina sem div.problem-statement (bloqueio ou layout novo)")

    titulo = pagina.css("div.header .title")
    return (titulo[0].text.strip() if titulo else None), enunciado[0].html_content


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--uri", default=os.environ.get("MONGO_URI", "mongodb://localhost:27017"),
                    help="conexao do Mongo (padrao: $MONGO_URI ou localhost)")
    ap.add_argument("--db", default="maratona", help="banco (padrao: maratona)")
    ap.add_argument("--colecao", default="problemas")
    ap.add_argument("--limite", type=int, default=0, help="para depois de N problemas (0 = todos)")
    ap.add_argument("--intervalo", type=float, default=2.0,
                    help="segundos entre requisicoes, para nao apanhar de rate limit")
    ap.add_argument("--timeout", type=int, default=30)
    ap.add_argument("--forcar", action="store_true",
                    help="refaz tambem os que ja tem enunciado")
    ap.add_argument("--dry-run", action="store_true", help="nao grava nada")
    args = ap.parse_args()

    cliente = MongoClient(args.uri, serverSelectionTimeoutMS=5000)
    try:
        cliente.admin.command("ping")
    except Exception as e:
        sys.exit(f"nao conectou no Mongo ({args.uri}): {e}")

    colecao = cliente[args.db][args.colecao]
    filtro = {} if args.forcar else FALTA_ENUNCIADO

    total = colecao.count_documents(filtro)
    if not total:
        print(f"nada a fazer: nenhum problema em {args.db}.{args.colecao} precisa de enunciado")
        return 0

    alvo = min(total, args.limite) if args.limite else total
    motivo = "no total (--forcar)" if args.forcar else "sem enunciado"
    print(f"{total} problema(s) {motivo}; processando {alvo}"
          f"{' (dry-run, nada sera gravado)' if args.dry_run else ''}\n")

    cursor = colecao.find(filtro, {"_id": 1, "nome": 1})
    if args.limite:
        cursor = cursor.limit(args.limite)

    ok = falhou = pulou = 0
    try:
        for i, doc in enumerate(cursor, 1):
            id_problema = doc["_id"]
            url = url_do_problema(id_problema)

            if not url:
                print(f"[{i}/{alvo}] {id_problema}: id fora do formato contestId+index, pulando")
                pulou += 1
                continue

            try:
                titulo, html = buscar_enunciado(url, args.timeout)
            except Exception as e:
                print(f"[{i}/{alvo}] {id_problema}: FALHOU ({type(e).__name__}: {e})")
                falhou += 1
            else:
                campos = {"descricao": html}
                # O nome so e sobrescrito se estiver vazio: o que veio da submissao
                # do Codeforces ja e bom, e o titulo da pagina vem como "A. Watermelon".
                if titulo and not doc.get("nome"):
                    campos["nome"] = titulo

                if not args.dry_run:
                    colecao.update_one({"_id": id_problema}, {"$set": campos})

                print(f"[{i}/{alvo}] {id_problema}: ok ({len(html)} chars)"
                      + (f" nome={titulo!r}" if "nome" in campos else ""))
                ok += 1

            if i < alvo:
                time.sleep(args.intervalo)
    except KeyboardInterrupt:
        # Interromper e seguro: cada problema e gravado no seu proprio update, entao
        # rodar de novo continua de onde parou.
        print("\ninterrompido; o que ja foi gravado continua gravado")

    print(f"\nresumo: {ok} preenchido(s), {falhou} falha(s), {pulou} pulado(s)")
    if falhou and not ok:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
