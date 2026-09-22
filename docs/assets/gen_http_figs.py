#!/usr/bin/env python3
"""Gera figuras das trocas HTTP reais da API, no estilo claro da apresentacao."""
import html
import json
import re
from pathlib import Path

FIGS = Path("/tmp/figs")
SAIDA = Path(__file__).resolve().parent


def ler(nome):
    """Arquivo tem o corpo e, na ultima linha, o status."""
    bruto = FIGS.joinpath(nome).read_text(encoding="utf-8")
    corpo, _, status = bruto.rpartition("\n")
    return corpo.strip(), status.strip()


def json_colorido(texto, limite=None):
    try:
        obj = json.loads(texto)
        bonito = json.dumps(obj, ensure_ascii=False, indent=2)
    except Exception:
        bonito = texto
    linhas = bonito.split("\n")
    cortou = False
    if limite and len(linhas) > limite:
        linhas = linhas[:limite]
        cortou = True
    saida = []
    for l in linhas:
        e = html.escape(l)
        e = re.sub(r'(&quot;[^&]*?&quot;)(\s*:)', r'<span class="k">\1</span>\2', e)
        e = re.sub(r'(:\s*)(&quot;.*?&quot;)', r'\1<span class="s">\2</span>', e)
        e = re.sub(r'(:\s*)(\d+\.?\d*|true|false|null)', r'\1<span class="n">\2</span>', e)
        saida.append(e)
    if cortou:
        saida.append('<span class="c">  … resposta completa no Swagger</span>')
    return "\n".join(saida)


CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: Arial, Helvetica, sans-serif; background: #fff; padding: 0; display: inline-block; }
.card { border: 1px solid #b9b9b9; background: #fff; margin-bottom: 14px; }
.card:last-child { margin-bottom: 0; }
.req {
  display: flex; align-items: center; gap: 10px; padding: 9px 13px;
  background: #f4f4f4; border-bottom: 1px solid #ddd;
}
.verbo {
  font-size: 12.5px; font-weight: 700; color: #fff; background: #555;
  padding: 2px 9px; border-radius: 2px; letter-spacing: .3px;
}
.verbo.post { background: #3c6e3c; }
.verbo.get  { background: #2f5b8c; }
.url { font-family: "DejaVu Sans Mono", monospace; font-size: 14px; color: #222; flex: 1; min-width: 0; }
.status { font-family: "DejaVu Sans Mono", monospace; font-size: 13.5px; font-weight: 700; white-space: nowrap; }
.ok  { color: #2f7a2f; }
.err { color: #b3261e; }
.rot { padding: 6px 13px; font-size: 12px; color: #555; border-bottom: 1px solid #eee; background: #fbfbfb; }
pre {
  font-family: "DejaVu Sans Mono", monospace; font-size: 13.5px; line-height: 1.5;
  padding: 10px 13px; color: #1f1f1f; white-space: pre-wrap; word-break: break-word; overflow: hidden;
}
.k { color: #8a5b00; }
.s { color: #067d17; }
.n { color: #1750eb; }
.c { color: #999; font-style: italic; }
.grade { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 16px; }
"""


def card(verbo, url, status, corpo, rotulo, limite=None):
    classe_v = verbo.lower()
    classe_s = "ok" if status.startswith("2") else "err"
    return f"""<div class="card">
      <div class="req"><span class="verbo {classe_v}">{verbo}</span>
        <span class="url">{html.escape(url)}</span>
        <span class="status {classe_s}">{html.escape(status)}</span></div>
      <div class="rot">{html.escape(rotulo)}</div>
      <pre>{json_colorido(corpo, limite)}</pre></div>"""


def pagina(conteudo, largura):
    return (f'<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8">'
            f"<style>{CSS}</style></head><body>"
            f'<div style="width:{largura}px">{conteudo}</div></body></html>')


if __name__ == "__main__":
    c201, s201 = ler("r201.txt")
    SAIDA.joinpath("http_201.html").write_text(
        pagina(card("POST", "/api/v1/bolsistas", f"{s201} Created", c201,
                    "Cadastro de bolsista — resposta com os links HATEOAS de navegação", limite=26), 900),
        encoding="utf-8")

    erros = [
        ("r400.txt", "POST", "/api/v1/auth/cadastro-admin", "Bean Validation: e-mail fora do formato — lista por campo"),
        ("r401.txt", "POST", "/api/v1/auth/login", "CredenciaisInvalidasException: senha incorreta"),
        ("r403.txt", "POST", "/api/v1/bolsistas", "PermissaoNegadaException: bolsista não cadastra usuário"),
        ("r404.txt", "GET", "/api/v1/projetos/{id}", "RecursoNaoEncontradoException: UUID válido, inexistente"),
        ("r409.txt", "POST", "/api/v1/bolsistas", "Violação de integridade: e-mail já cadastrado"),
        ("r429.txt", "POST", "/api/v1/auth/login", "ContaBloqueadaException: rate limiting após 5 falhas"),
    ]
    blocos = []
    for arq, verbo, url, rot in erros:
        corpo, status = ler(arq)
        nome = {"400": "400 Bad Request", "401": "401 Unauthorized", "403": "403 Forbidden",
                "404": "404 Not Found", "409": "409 Conflict",
                "429": "429 Too Many Requests"}[status]
        blocos.append(card(verbo, url, nome, corpo, rot, limite=8))
    SAIDA.joinpath("http_erros.html").write_text(
        pagina(f'<div class="grade">{"".join(blocos)}</div>', 1500), encoding="utf-8")
    print("html gerado: http_201.html, http_erros.html")
