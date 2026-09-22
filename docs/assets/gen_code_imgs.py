#!/usr/bin/env python3
"""Gera imagens de trechos de codigo no estilo claro da apresentacao de referencia."""
import html
import re
import subprocess
from pathlib import Path

RAIZ = Path("/home/matheus/workspace/trabalho-finalp-poow1")
SAIDA = RAIZ / "docs/assets"

KEYWORDS = {
    "public", "private", "protected", "class", "interface", "extends", "implements",
    "return", "new", "if", "else", "for", "while", "throw", "throws", "try", "catch",
    "final", "static", "void", "abstract", "import", "package", "boolean", "int", "long",
    "double", "true", "false", "null", "this", "super", "enum", "record", "var",
}
TIPOS = re.compile(r"\b([A-Z][A-Za-z0-9_]*)\b")


TOKEN = re.compile(
    r"""(?P<cm>//[^\n]*|/\*.*?\*/|\*/|/\*|^\s*\*[^\n]*)
      | (?P<st>"(?:[^"\\]|\\.)*")
      | (?P<an>@[A-Za-z][A-Za-z0-9_.]*)
      | (?P<nu>\b\d+(?:\.\d+)?\b)
      | (?P<id>\b[A-Za-z_][A-Za-z0-9_]*\b)""",
    re.X | re.S,
)


def realcar(linha: str) -> str:
    """Realce de Java em passada unica: cada caractere e consumido uma vez so."""
    despido = linha.strip()
    if despido.startswith("//") or despido.startswith("*") or despido.startswith("/*"):
        return f'<span class="cm">{html.escape(linha)}</span>'

    saida = []
    pos = 0
    for m in TOKEN.finditer(linha):
        if m.start() > pos:
            saida.append(html.escape(linha[pos:m.start()]))
        texto = html.escape(m.group(0))
        tipo = m.lastgroup
        if tipo == "id":
            bruto = m.group(0)
            if bruto in KEYWORDS:
                saida.append(f'<span class="kw">{texto}</span>')
            elif bruto[0].isupper():
                saida.append(f'<span class="ty">{texto}</span>')
            else:
                saida.append(texto)
        else:
            saida.append(f'<span class="{tipo}">{texto}</span>')
        pos = m.end()
    saida.append(html.escape(linha[pos:]))
    return "".join(saida)


CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: #fff; padding: 0; display: inline-block; font-family: Arial, Helvetica, sans-serif; }
.wrap { border: 1px solid #c4c4c4; display: inline-block; min-width: 980px; }
.hdr {
  background: #f2f2f2; border-bottom: 1px solid #c4c4c4;
  font-family: "DejaVu Sans Mono", "Courier New", monospace; font-size: 15px; color: #333;
  padding: 8px 14px;
}
table { border-collapse: collapse; width: 100%; background: #fff; }
td { font-family: "DejaVu Sans Mono", "Courier New", monospace; font-size: 16px; line-height: 1.55; }
td.ln {
  text-align: right; color: #a0a0a0; padding: 0 14px 0 12px; width: 1%;
  border-right: 1px solid #ebebeb; user-select: none; white-space: nowrap;
}
td.code { padding: 0 18px 0 14px; color: #1f1f1f; white-space: pre; }
tr:first-child td { padding-top: 10px; }
tr:last-child td { padding-bottom: 10px; }
.kw { color: #0033b3; font-weight: 600; }
.an { color: #9e880d; }
.st { color: #067d17; }
.cm { color: #8c8c8c; font-style: italic; }
.ty { color: #1750a5; }
.nu { color: #1750eb; }
"""


def montar_html(caminho_label: str, primeira_linha: int, linhas: list[str]) -> str:
    trs = []
    for i, l in enumerate(linhas):
        n = primeira_linha + i
        trs.append(f'<tr><td class="ln">{n}</td><td class="code">{realcar(l.rstrip())}</td></tr>')
    return f"""<!DOCTYPE html><html lang="pt-BR"><head><meta charset="utf-8"><style>{CSS}</style></head>
<body><div class="wrap"><div class="hdr">{html.escape(caminho_label)}</div>
<table>{''.join(trs)}</table></div></body></html>"""


def fatiar(rel: str, ini: int, fim: int) -> list[str]:
    linhas = (RAIZ / rel).read_text(encoding="utf-8").splitlines()
    return linhas[ini - 1:fim]


# (nome_saida, rotulo_exibido, arquivo, linha_inicial, linha_final)
TRECHOS = [
    ("cod_controller", "src/main/java/.../controller/BolsistaApiController.java",
     "src/main/java/dev/matheus/cadastroBolsistas/controller/BolsistaApiController.java", 231, 252),
    ("cod_service", "src/main/java/.../service/BolsistaService.java",
     "src/main/java/dev/matheus/cadastroBolsistas/service/BolsistaService.java", 86, 112),
    ("cod_exception", "src/main/java/.../exceptions/ApiExceptionHandler.java",
     "src/main/java/dev/matheus/cadastroBolsistas/exceptions/ApiExceptionHandler.java", 20, 46),
    ("cod_entidade", "src/main/java/.../model/Bolsista.java",
     "src/main/java/dev/matheus/cadastroBolsistas/model/Bolsista.java", 18, 45),
    ("cod_seguranca", "src/main/java/.../security/SecurityConfig.java",
     "src/main/java/dev/matheus/cadastroBolsistas/security/SecurityConfig.java", 29, 53),
    ("cod_repositorio", "src/main/java/.../repository/BolsistaRepository.java",
     "src/main/java/dev/matheus/cadastroBolsistas/repository/BolsistaRepository.java", 11, 31),
]

if __name__ == "__main__":
    for nome, rotulo, arq, ini, fim in TRECHOS:
        linhas = fatiar(arq, ini, fim)
        (SAIDA / f"{nome}.html").write_text(montar_html(rotulo, ini, linhas), encoding="utf-8")
        print(f"{nome}.html  ({len(linhas)} linhas, {ini}-{fim})")
