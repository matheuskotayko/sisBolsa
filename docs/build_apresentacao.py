#!/usr/bin/env python3
"""
Monta a apresentacao do SisBolsa no estilo ABNT da apresentacao de referencia.

Usa o pptx de referencia como base (herda tema, master e fontes), apaga os
slides dele e reconstroi o deck com o conteudo do SisBolsa, reaproveitando a
geometria medida do original.
"""
from pathlib import Path

from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt
from pptx.oxml import parse_xml

AQUI = Path(__file__).resolve().parent
ASSETS = AQUI / "assets"
REFERENCIA = AQUI / "Gestao_Risco_Mobile_Apresentacao_ABNT.pptx"
SAIDA = AQUI / "SisBolsa_Apresentacao_API_ABNT.pptx"

SERIF = "Times New Roman"
PRETO = RGBColor(0x00, 0x00, 0x00)
CINZA_NUM = RGBColor(0x59, 0x59, 0x59)

# geometria medida na apresentacao de referencia (em polegadas)
TITULO = dict(x=1.05, y=0.70, w=17.91, h=1.10, sz=36)
REGUA_Y, REGUA_X, REGUA_W = 2.02, 1.04, 17.93
SUB_Y, SUB_SZ = 2.30, 22.5
CORPO_SZ = 21
CAP_SZ, FONTE_SZ = 18, 15
LOGO = dict(x=0.74, y=10.56, w=2.12)
NUM = dict(x=18.13, y=10.49, w=1.16)

FONTE_AUTOR = "Fonte: Elaborado pelo autor (2026)."


# --------------------------------------------------------------------------- #
# infraestrutura
# --------------------------------------------------------------------------- #
def deck_em_branco():
    """
    Deck novo com a mesma geometria da referencia (20 x 11.25 in).

    Nao reaproveitamos o arquivo de referencia como base porque apagar os
    slides dele deixa as partes orfas no pacote e o zip sai com nomes
    duplicados. Como fonte, tamanho e cor sao definidos run a run, o tema
    do template nao faz falta.
    """
    prs = Presentation()
    prs.slide_width = Inches(20)
    prs.slide_height = Inches(11.25)
    return prs


def _texto(slide, x, y, w, h, paragrafos, *, sz=CORPO_SZ, bold=False, italic=False,
           align=PP_ALIGN.JUSTIFY, cor=PRETO, espaco_entre=10, entrelinha=1.08):
    cx = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = cx.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    if isinstance(paragrafos, str):
        paragrafos = [paragrafos]
    for i, item in enumerate(paragrafos):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = entrelinha
        if i:
            p.space_before = Pt(espaco_entre)
        # cada item pode ser str ou lista de (texto, negrito)
        pedacos = item if isinstance(item, list) else [(item, bold)]
        for txt, neg in pedacos:
            r = p.add_run()
            r.text = txt
            r.font.name = SERIF
            r.font.size = Pt(sz)
            r.font.bold = neg
            r.font.italic = italic
            r.font.color.rgb = cor
    return cx


def novo_slide(prs, numero=None):
    s = prs.slides.add_slide(prs.slide_layouts[6])  # Blank
    if numero is not None:
        s.shapes.add_picture(str(ASSETS / "logo_ufsm.png"),
                             Inches(LOGO["x"]), Inches(LOGO["y"]), width=Inches(LOGO["w"]))
        _texto(s, NUM["x"], NUM["y"], NUM["w"], 0.44, str(numero),
               sz=FONTE_SZ, align=PP_ALIGN.RIGHT, cor=CINZA_NUM)
    return s


def titulo_secao(slide, texto):
    _texto(slide, TITULO["x"], TITULO["y"], TITULO["w"], TITULO["h"], texto,
           sz=TITULO["sz"], bold=True, align=PP_ALIGN.LEFT, entrelinha=1.0)
    linha = slide.shapes.add_shape(1, Inches(REGUA_X), Inches(REGUA_Y), Inches(REGUA_W), 0)
    linha.line.color.rgb = PRETO
    linha.line.width = Pt(1.25)
    linha.fill.background()
    linha.shadow.inherit = False


def subtitulo(slide, x, y, w, texto):
    return _texto(slide, x, y, w, 0.62, texto, sz=SUB_SZ, bold=True, align=PP_ALIGN.LEFT)


def legenda(slide, x, y, w, texto, *, italico=True, sz=CAP_SZ, align=PP_ALIGN.CENTER):
    return _texto(slide, x, y, w, 0.45, texto, sz=sz, italic=italico, align=align)


def fonte(slide, x, y, w, align=PP_ALIGN.CENTER, texto=FONTE_AUTOR):
    return _texto(slide, x, y, w, 0.31, texto, sz=FONTE_SZ, align=align)


def figura(slide, nome, x, y, *, w=None, h=None):
    """Insere imagem preservando proporcao. Informe w OU h."""
    caminho = ASSETS / nome
    iw, ih = Image.open(caminho).size
    prop = ih / iw
    if w is not None:
        largura, altura = w, w * prop
    else:
        altura, largura = h, h / prop
    slide.shapes.add_picture(str(caminho), Inches(x), Inches(y),
                             width=Inches(largura), height=Inches(altura))
    return largura, altura


def _borda_celula(celula, cor="9A9A9A", espessura=Pt(0.75)):
    """
    Desenha a grade da celula.

    python-pptx nao expoe borda de celula, entao os elementos a:lnL/R/T/B
    entram na mao — e a ordem importa, o schema do OOXML e sequencial.
    """
    tc_pr = celula._tc.get_or_add_tcPr()
    for tag in ("a:lnL", "a:lnR", "a:lnT", "a:lnB"):
        for antigo in tc_pr.findall(qn(tag)):
            tc_pr.remove(antigo)
    for tag in ("a:lnL", "a:lnR", "a:lnT", "a:lnB"):
        ln = parse_xml(
            f'<{tag} xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
            f'w="{int(espessura)}" cap="flat" cmpd="sng" algn="ctr">'
            f'<a:solidFill><a:srgbClr val="{cor}"/></a:solidFill>'
            f'<a:prstDash val="solid"/></{tag}>')
        tc_pr.append(ln)


def quadro(slide, x, y, w, dados, larguras, *, sz=15, altura_linha=0.34):
    """Tabela simples no estilo ABNT: cabecalho em negrito, grade fina."""
    linhas, colunas = len(dados), len(dados[0])
    forma = slide.shapes.add_table(linhas, colunas, Inches(x), Inches(y),
                                   Inches(w), Inches(altura_linha * linhas))
    tab = forma.table
    # o estilo padrao do PowerPoint pinta cabecalho azul e zebra as linhas;
    # aqui a formatacao e toda explicita, entao ele sai do caminho
    tab.first_row = False
    tab.horz_banding = False
    for j, lw in enumerate(larguras):
        tab.columns[j].width = Inches(lw)
    for i, linha in enumerate(dados):
        for j, valor in enumerate(linha):
            cel = tab.cell(i, j)
            cel.text = ""
            cel.vertical_anchor = MSO_ANCHOR.MIDDLE
            cel.margin_left = Inches(0.08)
            cel.margin_right = Inches(0.08)
            cel.margin_top = Inches(0.03)
            cel.margin_bottom = Inches(0.03)
            cel.fill.solid()
            cel.fill.fore_color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
            p = cel.text_frame.paragraphs[0]
            p.alignment = PP_ALIGN.LEFT
            r = p.add_run()
            r.text = valor
            r.font.name = SERIF
            r.font.size = Pt(sz)
            r.font.bold = (i == 0)
            r.font.color.rgb = PRETO
            _borda_celula(cel)
    return forma


# --------------------------------------------------------------------------- #
# conteudo
# --------------------------------------------------------------------------- #
def slide_capa(prs):
    s = novo_slide(prs)
    s.shapes.add_picture(str(ASSETS / "logo_ufsm.png"), Inches(7.72), Inches(0.26), width=Inches(4.55))
    _texto(s, 2.35, 0.91, 15.26, 2.19,
           ["UNIVERSIDADE FEDERAL DE SANTA MARIA",
            "COLÉGIO POLITÉCNICO DA UFSM",
            "CURSO SUPERIOR DE TECNOLOGIA EM SISTEMAS PARA INTERNET"],
           sz=27, align=PP_ALIGN.CENTER, espaco_entre=0, entrelinha=1.35)
    _texto(s, 2.38, 3.43, 15.26, 0.69, "MATHEUS GABRIEL FLECK DE MELLO",
           sz=27, align=PP_ALIGN.CENTER)
    _texto(s, 2.35, 4.70, 15.26, 1.10, "SISBOLSA — API REST DE GESTÃO DE BOLSISTAS E LABORATÓRIOS",
           sz=39, bold=True, align=PP_ALIGN.CENTER, entrelinha=1.1)
    _texto(s, 9.91, 6.45, 8.97, 1.33,
           "Apresentação referente à disciplina de Programação Orientada a Objetos para Web II, "
           "do Curso Superior de Tecnologia em Sistemas para Internet, da Universidade Federal de "
           "Santa Maria, cursada em regime de autodidatismo.",
           sz=18, italic=True, entrelinha=1.3)
    _texto(s, 2.35, 9.79, 15.26, 0.97, ["Santa Maria, RS", "2026"],
           sz=24, align=PP_ALIGN.CENTER, espaco_entre=0, entrelinha=1.2)


def slide_contexto(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "1  CONTEXTO E ORIGINALIDADE")
    _texto(s, 1.05, 2.45, 17.91, 2.2,
           "O SisBolsa é uma API REST autoral para a gestão de bolsistas de pesquisa de uma instituição: "
           "cadastro de professores, laboratórios, projetos e bolsistas, controle de frequência com emissão "
           "de comprovante em PDF, relatórios agregados e trilha de auditoria. O projeto nasceu na disciplina "
           "de Programação Orientada a Objetos para Web I e, nesta disciplina, foi reescrito com foco total no "
           "backend — a camada de apresentação saiu do escopo e o repositório passou a expor somente a API.",
           entrelinha=1.25)
    _texto(s, 1.05, 4.95, 17.91, 2.6,
           [[("a) Superfície da API", True), (": 9 controllers REST e 53 endpoints sob /api/v1, um controller por recurso", False)],
            [("b) Qualidade verificável", True), (": 150 testes automatizados, sem dependência de banco externo", False)],
            [("c) Documentação viva", True), (": OpenAPI 3 / Swagger UI gerados a partir das próprias anotações do código", False)],
            [("d) Implantação reproduzível", True), (": Docker Compose sobe banco e API com migrations e massa de demonstração", False)]],
           entrelinha=1.25, espaco_entre=12)
    _texto(s, 1.05, 8.1, 17.91, 0.9,
           [[("Repositório do sistema: ", True), ("https://github.com/matheuskotayko/sisBolsa", False)]],
           sz=19, align=PP_ALIGN.LEFT)


def slide_motivacao(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "2  MOTIVAÇÃO")

    # o formulario e retrato, entao a figura fica estreita e o texto ocupa o resto
    legenda(s, 1.05, 2.30, 6.60, "Figura 1 – Formulário de registro de frequência do CTISM")
    _, alt = figura(s, "frequencia_papel.png", 1.05, 3.10, w=6.60)
    fonte(s, 1.05, 3.10 + alt + 0.14, 6.60,
          texto="Fonte: Acervo do autor (2026).")

    _texto(s, 8.42, 2.42, 10.54, 2.3,
           "O projeto nasceu da experiência com a minha própria bolsa no setor de realidade virtual e "
           "inteligência artificial do CTISM. O controle de frequência era o formulário ao lado: data, "
           "entrada, saída, horas e atividade preenchidos à mão, com espaço para a rubrica do monitor e "
           "do preceptor — depois digitalizado e enviado por e-mail.",
           entrelinha=1.25)
    _texto(s, 8.42, 4.78, 10.54, 5.6,
           [[("a) Registro manual e sem validação", True), (": nada impede hora sobreposta, data inválida ou total de horas que não fecha com o período da bolsa", False)],
            [("b) Sem rastreabilidade das horas", True), (": o papel não gera comprovante verificável nem trilha auditável do que foi alterado", False)],
            [("c) Controle disperso", True), (": cada professor e laboratório mantém os próprios arquivos, sem consolidação institucional", False)],
            [("d) Ocupação dos laboratórios invisível", True), (": não havia como saber, em tempo real, quantas vagas restam em cada laboratório", False)]],
           entrelinha=1.25, espaco_entre=14)


def slide_objetivos(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "3  OBJETIVOS")
    subtitulo(s, 1.05, SUB_Y, 8.36, "3.1 OBJETIVO GERAL")
    _texto(s, 1.05, 3.05, 8.50, 3.4,
           "Desenvolver uma API REST completa para a gestão de bolsistas de pesquisa, cobrindo o cadastro "
           "de professores, laboratórios e projetos, o vínculo de bolsistas, o controle de frequência com "
           "emissão de comprovante em PDF, a geração de relatórios e exportações em CSV e uma trilha de "
           "auditoria completa, com controle de acesso por perfil.",
           sz=SUB_SZ, entrelinha=1.25)
    subtitulo(s, 10.46, SUB_Y, 8.36, "3.2 OBJETIVOS ESPECÍFICOS")
    _texto(s, 10.46, 3.06, 8.40, 6.4,
           ["a) Seguir as boas práticas de REST na camada de controller: um recurso por controller, "
            "semântica correta de verbos e códigos HTTP, 201 Created com header Location e links HATEOAS",
            "b) Concentrar as regras de negócio e as permissões na camada de service, mantendo o controller "
            "restrito a receber a requisição e montar a resposta",
            "c) Modelar e persistir o domínio com Spring Data JPA/Hibernate sobre PostgreSQL, com o schema "
            "versionado por migrations do Flyway",
            "d) Implementar autenticação stateless com JWT em cookie httpOnly, BCrypt e rate limiting "
            "anti-força-bruta, com autorização por papel de acesso",
            "e) Validar a entrada com Bean Validation e traduzir as exceções de domínio para os status HTTP "
            "correspondentes, com corpo de erro padronizado",
            "f) Documentar a coleção de endpoints com OpenAPI 3 / Swagger, gerada a partir do próprio código"],
           entrelinha=1.2, espaco_entre=11)


def _slide_figura_direita(prs, n, secao, sub, corpo, img, cap, *, img_w=11.41, img_y=2.95, sz=CORPO_SZ):
    """Layout: figura a esquerda, texto a direita (como o slide 7 da referencia)."""
    s = novo_slide(prs, n)
    titulo_secao(s, secao)
    legenda(s, 1.05, 2.41, img_w, cap)
    _, alt = figura(s, img, 1.05, img_y, w=img_w)
    fonte(s, 1.05, min(img_y + alt + 0.14, 10.2), img_w)
    subtitulo(s, 12.84, 2.38, 6.15, sub)
    _texto(s, 12.84, 3.07, 6.15, 6.5, corpo, sz=sz, entrelinha=1.2, espaco_entre=10)
    return s


def slide_classes(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "4  MODELAGEM DE DADOS")
    subtitulo(s, 1.05, SUB_Y, 8.36, "4.1 DIAGRAMA DE CLASSES")
    _texto(s, 1.05, 3.0, 6.0, 5.6,
           ["a) Usuario é uma classe abstrata anotada com @MappedSuperclass: concentra os atributos comuns "
            "(id, nome, e-mail, senha, tipo de usuário) sem virar tabela própria",
            "b) Bolsista e Professor herdam dela e são entidades concretas, cada uma com a sua tabela e a "
            "sua chave primária em UUID",
            "c) Bolsista concentra os vínculos: laboratório de lotação, projetos (N:N) e os apontamentos de "
            "frequência",
            "d) Cargo e ModalidadeBolsa são enums persistidos como texto; Auditoria registra as ações "
            "relevantes de qualquer perfil"],
           sz=18, entrelinha=1.2, espaco_entre=10)
    legenda(s, 7.4, 2.32, 11.55, "Figura 2 – Diagrama de classes do domínio")
    figura(s, "classes_p1.png", 7.7, 2.92, h=6.6)
    figura(s, "classes_p2.png", 13.6, 2.92, h=6.6)
    _texto(s, 7.4, 9.62, 5.5, 0.3, "Parte 1 de 2", sz=13, align=PP_ALIGN.CENTER, italic=True)
    _texto(s, 13.3, 9.62, 5.5, 0.3, "Parte 2 de 2", sz=13, align=PP_ALIGN.CENTER, italic=True)
    fonte(s, 7.4, 9.95, 11.55)


def slide_mer(prs, n):
    return _slide_figura_direita(
        prs, n, "4  MODELAGEM DE DADOS", "4.2 MODELO CONCEITUAL",
        ["a) recorte do domínio na notação de Chen (1976), com seis entidades principais;",
         "b) um professor coordena laboratórios e orienta projetos;",
         "c) bolsistas são alocados a um laboratório e vinculados a projetos em relação N:N, "
         "materializada pela tabela associativa;",
         "d) cada apontamento de frequência pertence a um bolsista, e toda ação relevante do sistema "
         "gera um registro de auditoria."],
        "mer.png", "Figura 3 – Modelo conceitual (MER)")


def slide_der(prs, n):
    return _slide_figura_direita(
        prs, n, "4  MODELAGEM DE DADOS", "4.3 MODELO LÓGICO",
        ["a) esquema relacional derivado das migrations do Flyway;",
         "b) chave primária em UUID gerada pelo próprio banco em todas as tabelas;",
         "c) tabela associativa bolsista_projeto para a relação N:N entre bolsista e projeto;",
         "d) exclusão lógica por campo ativo: nenhum registro é removido fisicamente, preservando o "
         "histórico de frequência e de vínculos."],
        "der.png", "Figura 4 – Modelo lógico (DER)")


def slide_arquitetura(prs, n):
    return _slide_figura_direita(
        prs, n, "5  ARQUITETURA", "5.1 VISÃO GERAL",
        ["a) a API é o único ponto de entrada: fala JSON sobre HTTPS e não pressupõe um frontend específico;",
         "b) a sessão é stateless — o usuário é reconstruído a cada requisição a partir do JWT guardado "
         "em cookie httpOnly;",
         "c) a requisição atravessa controller, service e repository, nessa ordem, até o PostgreSQL;",
         "d) o ambiente sobe inteiro com Docker Compose: banco e API, com as migrations do Flyway e a "
         "massa de demonstração aplicadas automaticamente."],
        "diag_arquitetura.png", "Figura 5 – Visão geral da arquitetura", img_y=3.6)


def slide_camadas(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "5  ARQUITETURA")
    subtitulo(s, 1.05, SUB_Y, 9.0, "5.2 CAMADAS DA APLICAÇÃO")
    legenda(s, 1.05, 3.05, 17.91, "Figura 6 – Camadas da aplicação e pacotes correspondentes")
    _, alt = figura(s, "diag_camadas.png", 1.05, 3.65, w=17.91)
    fonte(s, 1.05, 3.65 + alt + 0.18, 17.91)
    _texto(s, 1.05, 9.9, 17.91, 0.8,
           "Cada camada só conhece a camada imediatamente abaixo. O controller não contém regra de negócio: "
           "a decisão de quem pode o quê, a busca que resulta em 404 e as validações de domínio vivem no service.",
           sz=17, align=PP_ALIGN.CENTER, italic=True)


def slide_organizacao(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "5  ARQUITETURA")
    subtitulo(s, 1.05, SUB_Y, 9.0, "5.3 ORGANIZAÇÃO DO CÓDIGO")
    legenda(s, 1.05, 2.98, 12.6, "Figura 7 – Estrutura de pacotes do projeto")
    _, alt = figura(s, "diag_estrutura.png", 1.05, 3.55, w=12.6)
    fonte(s, 1.05, 3.55 + alt + 0.16, 12.6)
    subtitulo(s, 14.2, 3.0, 4.8, "LEITURA DA ESTRUTURA")
    _texto(s, 14.2, 3.72, 4.85, 6.0,
           ["a) um pacote por responsabilidade, espelhando as camadas;",
            "b) dto/ é o maior pacote (20 arquivos) porque cada recurso tem contratos próprios de entrada e "
            "saída, isolando a API das entidades;",
            "c) os testes espelham a estrutura de main, concentrados na camada de service — onde estão as regras;",
            "d) as migrations versionam o schema: subir o projeto do zero reproduz o banco inteiro."],
           sz=17, entrelinha=1.2, espaco_entre=10)


def slide_tecnologias(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "5  ARQUITETURA")
    subtitulo(s, 1.05, SUB_Y, 9.0, "5.4 TECNOLOGIAS UTILIZADAS")
    legenda(s, 1.05, 2.98, 11.8, "Quadro 1 – Tecnologias do projeto e sua função", sz=FONTE_SZ,
            align=PP_ALIGN.LEFT)
    dados = [
        ("Tecnologia", "Função no projeto"),
        ("Spring Boot 4.0.6 / Java 21", "Base da aplicação e injeção de dependências"),
        ("Spring Web MVC", "Camada REST: mapeamento de rotas, verbos e status HTTP"),
        ("Spring Data JPA / Hibernate", "Persistência, repositórios e mapeamento objeto-relacional"),
        ("PostgreSQL 15", "Banco de dados relacional"),
        ("Flyway", "Versionamento do schema por migrations numeradas"),
        ("Spring Security", "Autorização por papel e cadeia de filtros"),
        ("JJWT + BCrypt", "Emissão e leitura do token JWT e hash das senhas"),
        ("Bean Validation (Jakarta)", "Validação declarativa dos DTOs de entrada"),
        ("springdoc-openapi", "Geração do OpenAPI 3 e da interface Swagger UI"),
        ("Spring HATEOAS", "Links de navegação nas respostas de recurso individual"),
        ("OpenPDF", "Geração nativa do comprovante de frequência em PDF"),
        ("JUnit 5 + Mockito", "Testes unitários e de controller com MockMvc"),
        ("Docker Compose", "Subida do ambiente completo (banco e API)"),
    ]
    quadro(s, 1.05, 3.38, 11.8, dados, [4.0, 7.8], sz=14.5, altura_linha=0.315)
    fonte(s, 1.05, 3.38 + 0.315 * len(dados) + 0.12, 11.8, align=PP_ALIGN.LEFT)
    subtitulo(s, 13.6, 3.42, 5.4, "STACK EM UMA LINHA")
    _texto(s, 13.6, 4.12, 5.4, 2.4,
           "API em Java 21 com Spring Boot 4, persistência em PostgreSQL versionada por Flyway, "
           "segurança com JWT e Spring Security, documentação em OpenAPI 3 e ambiente em Docker.",
           sz=18, entrelinha=1.25)


def slide_funcionalidades(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "6  FUNCIONALIDADES")
    esquerda = [
        [("a) Bolsistas e vínculos", True), (": cadastro, lotação em laboratório, cargo, modalidade e vigência da bolsa, com alerta de bolsa vencida ou a vencer", False)],
        [("b) Professores", True), (": cadastro dos coordenadores, restrito ao perfil de administrador", False)],
        [("c) Laboratórios", True), (": capacidade, coordenação e percentual de ocupação calculado em tempo real", False)],
        [("d) Projetos", True), (": gestão por laboratório, com vínculo e desvínculo de bolsistas da equipe", False)],
    ]
    direita = [
        [("e) Frequência e horas", True), (": apontamento com descrição e link de entregável, resumo mensal e exportação em CSV", False)],
        [("f) Comprovante em PDF", True), (": documento institucional gerado pela própria API, com tabela de apontamentos e campos de assinatura", False)],
        [("g) Relatórios", True), (": ocupação dos laboratórios, horas no mês, projetos por laboratório e distribuição por cargo", False)],
        [("h) Auditoria", True), (": toda ação relevante vira registro consultável, com filtros e exportação em CSV", False)],
    ]
    _texto(s, 1.05, 2.6, 8.6, 6.5, esquerda, entrelinha=1.22, espaco_entre=16)
    _texto(s, 10.35, 2.6, 8.6, 6.5, direita, entrelinha=1.22, espaco_entre=16)


def slide_endpoints(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "7  A API REST")
    subtitulo(s, 1.05, SUB_Y, 9.0, "7.1 RECURSOS E ENDPOINTS")
    legenda(s, 1.05, 2.98, 17.91, "Quadro 2 – Recursos da API e principais operações", sz=FONTE_SZ,
            align=PP_ALIGN.LEFT)
    dados = [
        ("Recurso", "Base da URL", "Principais operações"),
        ("Bolsistas", "/api/v1/bolsistas",
         "Listagem paginada e busca por nome ou curso, cadastro, atualização parcial (PATCH), desativação, "
         "exportação CSV, cargos e modalidades — cobre também os administradores"),
        ("Professores", "/api/v1/professores",
         "CRUD completo dos coordenadores — restrito a administradores"),
        ("Laboratórios", "/api/v1/laboratorios",
         "CRUD completo; consulta de bolsistas e projetos vinculados"),
        ("Projetos", "/api/v1/projetos",
         "CRUD completo; vincular e desvincular bolsista da equipe"),
        ("Frequências", "/api/v1/frequencias",
         "Registro, atualização, resumo mensal, exportação CSV e comprovante em PDF"),
        ("Relatórios", "/api/v1/relatorios",
         "Indicadores agregados: ocupação, horas no mês, projetos e distribuição por cargo"),
        ("Auditoria", "/api/v1/auditoria",
         "Consulta filtrada e exportação dos registros de operações"),
        ("Autenticação", "/api/v1/auth",
         "Login, logout, perfil próprio, cadastro de administrador e recuperação de senha"),
        ("Cursos", "/api/v1/cursos",
         "Listagem dos cursos disponíveis e cadastro restrito a administradores"),
    ]
    quadro(s, 1.05, 3.38, 17.91, dados, [2.2, 3.5, 12.21], sz=15, altura_linha=0.55)
    fonte(s, 1.05, 3.38 + 0.55 * len(dados) + 0.14, 17.91, align=PP_ALIGN.LEFT)
    _texto(s, 1.05, 9.9, 17.91, 0.7,
           "Os 53 endpoints estão documentados de forma interativa em /swagger-ui.html e disponíveis "
           "como coleção Postman no repositório.",
           sz=17, align=PP_ALIGN.CENTER, italic=True)


def slide_controller(prs, n):
    return _slide_figura_direita(
        prs, n, "7  A API REST", "7.2 BOAS PRÁTICAS NO CONTROLLER",
        ["a) um controller por recurso, mapeado em /api/v1/<recurso>, com os verbos correspondendo às operações;",
         "b) PATCH para atualização parcial e PUT para substituição total; POST devolve 201 Created com o "
         "header Location do recurso criado;",
         "c) DELETE devolve 204 No Content e faz exclusão lógica, preservando o histórico;",
         "d) a permissão não é decidida aqui: o controller chama o service, que devolve a entidade ou lança "
         "a exceção;",
         "e) respostas de recurso individual carregam links HATEOAS de navegação."],
        "cod_controller.png", "Figura 8 – Trecho de código: verbo, status e links HATEOAS",
        img_y=3.3, sz=18)


def slide_service(prs, n):
    return _slide_figura_direita(
        prs, n, "7  A API REST", "7.3 REGRAS NA CAMADA DE SERVICE",
        ["a) a busca que pode falhar vive no service: buscarOuFalhar lança a exceção de recurso inexistente, "
         "e nenhum controller checa null na mão;",
         "b) as permissões são métodos nomeados do domínio — ver, editar e excluir têm regras distintas;",
         "c) o service recebe o usuário autenticado como parâmetro e decide; o controller apenas repassa;",
         "d) o mesmo método é reaproveitado por vários endpoints, o que elimina a duplicação de regra que "
         "existia antes no controller."],
        "cod_service.png", "Figura 9 – Trecho de código: busca com 404 e permissão no service",
        img_y=3.15, sz=18)


def slide_excecoes(prs, n):
    return _slide_figura_direita(
        prs, n, "7  A API REST", "7.4 TRATAMENTO DE ERROS",
        ["a) um único @RestControllerAdvice concentra a tradução de exceção para status HTTP;",
         "b) cada exceção de domínio tem um significado HTTP próprio: 404 para recurso inexistente, 403 para "
         "permissão negada, 409 para conflito de regra, 401 para credencial inválida e 429 para conta bloqueada;",
         "c) o corpo de erro é sempre o mesmo objeto, com a mensagem do domínio;",
         "d) falhas de Bean Validation respondem em lista, com o campo e a mensagem de cada violação."],
        "cod_exception.png", "Figura 10 – Trecho de código: exceções de domínio mapeadas para HTTP",
        img_y=3.15, sz=18)


def slide_entidade(prs, n):
    return _slide_figura_direita(
        prs, n, "8  PERSISTÊNCIA", "8.1 MAPEAMENTO JPA",
        ["a) @Entity e @Table por tabela, com chave primária em UUID gerada pelo banco;",
         "b) Usuario é @MappedSuperclass: Bolsista e Professor herdam os atributos comuns, mas cada um tem a "
         "sua própria tabela — não há tabela de usuário;",
         "c) o relacionamento @ManyToOne com o laboratório é somente leitura (insertable e updatable falsos): "
         "enriquece a resposta sem abrir uma segunda via de escrita;",
         "d) a tabela associativa bolsista_projeto é mapeada como @ManyToMany: vincular um bolsista a um "
         "projeto é alterar uma coleção, e o JPA emite o INSERT ou o DELETE correspondente;",
         "e) enums persistidos como texto com @Enumerated(EnumType.STRING), legíveis direto no banco."],
        "cod_entidade.png", "Figura 11 – Trecho de código: entidade Bolsista",
        img_y=3.15, sz=18)


def slide_repositorio(prs, n):
    return _slide_figura_direita(
        prs, n, "8  PERSISTÊNCIA", "8.2 REPOSITÓRIOS E MIGRATIONS",
        ["a) uma interface por entidade estendendo JpaRepository: nenhuma implementação é escrita à mão;",
         "b) as consultas são derivadas do nome do método, inclusive as que navegam por associação — "
         "findByProjetos_Id percorre o relacionamento N:N sem uma linha de SQL;",
         "c) filtros opcionais (período, termo de busca, laboratório) usam Specification, a Criteria API do "
         "próprio JPA: cada predicado só entra no WHERE se o parâmetro tiver sido informado;",
         "d) @Query ficou restrito ao repositório de relatórios, onde há agregação com GROUP BY;",
         "e) a exclusão é um soft delete e o schema é versionado em 10 migrations do Flyway."],
        "cod_repositorio.png", "Figura 12 – Trecho de código: repositório Spring Data JPA",
        img_y=3.15, sz=18)


def slide_seguranca_config(prs, n):
    return _slide_figura_direita(
        prs, n, "9  SEGURANÇA", "9.1 AUTENTICAÇÃO E SESSÃO",
        ["a) sessão stateless: o servidor não guarda estado, e o usuário é reconstruído a cada requisição a "
         "partir do token;",
         "b) o JWT viaja em cookie httpOnly — inacessível a JavaScript no navegador;",
         "c) o filtro roda antes da autenticação padrão do Spring Security e popula o contexto;",
         "d) as rotas públicas são explícitas; o restante de /api/** exige token válido;",
         "e) senhas com BCrypt e bloqueio temporário após cinco tentativas malsucedidas."],
        "cod_seguranca.png", "Figura 13 – Trecho de código: cadeia de filtros e sessão stateless",
        img_y=3.15, sz=18)


def slide_perfis(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "9  SEGURANÇA")
    subtitulo(s, 1.05, SUB_Y, 9.0, "9.2 PERFIS DE ACESSO")
    legenda(s, 1.05, 2.98, 11.8, "Quadro 3 – Perfis de acesso e permissões", sz=FONTE_SZ, align=PP_ALIGN.LEFT)
    dados = [
        ("Papel", "Permissões"),
        ("ADMIN", "Acesso irrestrito: todos os laboratórios, bolsistas, professores e projetos, "
                  "relatórios globais, auditoria completa e cadastro de cursos"),
        ("PROFESSOR", "Gerencia os laboratórios que coordena, os projetos associados e os bolsistas "
                      "vinculados a eles; consulta a auditoria"),
        ("BOLSISTA", "Apontamento e edição da própria frequência, emissão do comprovante em PDF e "
                     "visualização da equipe e dos projetos do seu laboratório"),
    ]
    quadro(s, 1.05, 3.38, 11.8, dados, [2.3, 9.5], sz=17, altura_linha=0.95)
    fonte(s, 1.05, 3.38 + 0.95 * len(dados) + 0.14, 11.8, align=PP_ALIGN.LEFT)
    subtitulo(s, 13.6, 3.42, 5.4, "CONTROLES APLICADOS")
    _texto(s, 13.6, 4.12, 5.4, 5.5,
           ["a) sem token, a requisição é recusada com 401; autenticado sem o papel exigido, 403;",
            "b) rate limiting: cinco tentativas de login malsucedidas bloqueiam a conta por cinco minutos (429);",
            "c) recuperação de senha por código numérico de seis dígitos, válido por quinze minutos;",
            "d) a permissão é verificada no service — a mesma regra vale para qualquer endpoint que a use."],
           sz=17, entrelinha=1.2, espaco_entre=10)


def slide_testes(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "10  QUALIDADE")
    subtitulo(s, 1.05, SUB_Y, 9.0, "10.1 TESTES AUTOMATIZADOS")
    legenda(s, 1.05, 2.98, 11.0, "Quadro 4 – Testes automatizados por área", sz=FONTE_SZ, align=PP_ALIGN.LEFT)
    dados = [
        ("Área", "Testes"),
        ("Regras de negócio e permissões (services)", "99"),
        ("Controller de autenticação (MockMvc)", "18"),
        ("Utilitários de domínio", "11"),
        ("Modelo e enums do domínio", "10"),
        ("Segurança: rate limiting e reset de senha", "8"),
        ("Tratamento de exceções da API", "3"),
        ("Contexto da aplicação", "1"),
        ("Total", "150"),
    ]
    quadro(s, 1.05, 3.38, 11.0, dados, [8.6, 2.4], sz=17, altura_linha=0.44)
    fonte(s, 1.05, 3.38 + 0.44 * len(dados) + 0.14, 11.0, align=PP_ALIGN.LEFT)
    subtitulo(s, 12.9, 3.42, 6.1, "COMO A SUÍTE É ORGANIZADA")
    _texto(s, 12.9, 4.12, 6.1, 5.5,
           ["a) 150 testes em 17 arquivos, executados sem banco de dados externo;",
            "b) o peso está na camada de service, onde vivem as regras — cada permissão tem teste de "
            "concessão e de negação;",
            "c) o controller é testado com MockMvc, verificando status e corpo da resposta;",
            "d) a suíte roda com um único comando (mvn test) e é o que sustenta cada refatoração do projeto."],
           sz=17, entrelinha=1.2, espaco_entre=10)


def slide_criterios(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "10  QUALIDADE")
    subtitulo(s, 1.05, SUB_Y, 9.0, "10.2 CRITÉRIOS DE AVALIAÇÃO")
    _texto(s, 1.05, 3.05, 17.91, 6.6,
           [[("a) Boas práticas de REST no controller", True), (": um recurso por controller, verbos e status coerentes, 201 com Location, 204 no delete lógico e links HATEOAS", False)],
            [("b) Regras na camada correta", True), (": permissões, busca com 404 e validações de domínio concentradas no service, com o controller reduzido a orquestração", False)],
            [("c) Persistência com JPA e Spring Data", True), (": entidades mapeadas, herança por @MappedSuperclass, repositórios declarativos e schema versionado com Flyway", False)],
            [("d) Documentação gerada do código", True), (": OpenAPI 3 e Swagger UI a partir das anotações, mais coleções Postman de fluxo e de casos negativos", False)],
            [("e) Segurança concreta", True), (": sessão stateless com JWT em cookie httpOnly, BCrypt, rate limiting e três perfis com permissões efetivamente distintas", False)],
            [("f) Erros com significado", True), (": exceções de domínio traduzidas para 400, 401, 403, 404, 409 e 429, com corpo de erro padronizado", False)],
            [("g) Qualidade verificável", True), (": 150 testes automatizados cobrindo a matriz de autorização e as regras de domínio", False)]],
           sz=19, entrelinha=1.2, espaco_entre=13)


def slide_documentacao(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "11  DOCUMENTAÇÃO E DEMONSTRAÇÃO")
    subtitulo(s, 1.05, SUB_Y, 9.0, "11.1 SWAGGER E COLEÇÕES POSTMAN")
    legenda(s, 1.05, 2.95, 17.91, "Figura 14 – Swagger UI: visão geral e os recursos após a divisão por tipo de usuário")
    figura(s, "swagger_visao.png", 1.05, 3.45, w=8.4)
    figura(s, "swagger_recursos.png", 10.55, 3.45, w=8.4)
    fonte(s, 1.05, 9.66, 17.91)
    _texto(s, 1.05, 10.05, 17.91, 0.45,
           "A documentação é gerada do próprio código, então não desatualiza em relação à API real.",
           sz=15, align=PP_ALIGN.CENTER, italic=True, entrelinha=1.1)


def slide_casos_negativos(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "11  DOCUMENTAÇÃO E DEMONSTRAÇÃO")
    subtitulo(s, 1.05, SUB_Y, 12.0, "11.2 SUCESSO E CASOS NEGATIVOS")
    legenda(s, 1.05, 2.92, 17.91, "Figura 15 – Respostas reais da API: criação bem-sucedida e exceções de domínio")
    figura(s, "http_201.png", 1.05, 3.45, w=6.3)
    figura(s, "http_erros.png", 7.85, 3.45, w=11.1)
    _texto(s, 1.05, 8.5, 6.3, 1.5,
           "Cada exceção de domínio tem um status HTTP próprio e um corpo de erro padronizado; "
           "a Bean Validation responde em lista, com o campo que falhou.",
           sz=16, entrelinha=1.2)
    fonte(s, 1.05, 10.1, 17.91)


def slide_consideracoes(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "12  CONSIDERAÇÕES FINAIS")
    _texto(s, 1.05, 2.5, 17.91, 2.4,
           "O SisBolsa entrega uma API REST completa para a gestão de bolsistas de pesquisa: as boas práticas "
           "de REST aparecem na camada de controller, as regras de negócio e as permissões ficam no service, a "
           "persistência usa JPA com Spring Data sobre um schema versionado, e a documentação é gerada do próprio "
           "código. São características pensadas para uso real — e não para atender a um requisito acadêmico isolado.",
           entrelinha=1.25)
    subtitulo(s, 1.05, 5.3, 8.6, "O QUE PODE SER IMPLEMENTADO FUTURAMENTE")
    _texto(s, 1.05, 6.1, 8.6, 3.2,
           ["a) notificações por e-mail para prazos de entregáveis dos projetos;",
            "b) aplicativo mobile para apontamento de frequência em campo, consumindo esta mesma API;",
            "c) assinatura digital dos comprovantes de frequência em PDF;",
            "d) relatórios comparativos por período e por laboratório."],
           sz=19, entrelinha=1.2, espaco_entre=10)
    subtitulo(s, 10.35, 5.3, 8.6, "REPOSITÓRIO")
    _texto(s, 10.35, 6.1, 8.6, 1.4,
           ["https://github.com/matheuskotayko/sisBolsa",
            "A branch main contém somente o backend; o frontend construído na disciplina anterior "
            "permanece arquivado na branch fullstack."],
           sz=19, entrelinha=1.2, espaco_entre=10, align=PP_ALIGN.LEFT)


def slide_referencias(prs, n):
    s = novo_slide(prs, n)
    titulo_secao(s, "REFERÊNCIAS")
    _texto(s, 1.05, 2.6, 17.91, 7.0,
           [[("ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. ", False), ("NBR 6023: informação e documentação — referências — elaboração.", True), (" Rio de Janeiro: ABNT, 2018.", False)],
            [("CHEN, P. P. ", False), ("The entity-relationship model: toward a unified view of data.", True), (" ACM Transactions on Database Systems, New York, v. 1, n. 1, p. 9-36, mar. 1976.", False)],
            [("FIELDING, R. T. ", False), ("Architectural styles and the design of network-based software architectures.", True), (" 2000. Tese (Doutorado em Ciência da Computação) — University of California, Irvine, 2000.", False)],
            [("RICHARDSON, L.; AMUNDSEN, M. ", False), ("RESTful Web APIs.", True), (" Sebastopol: O'Reilly Media, 2013.", False)],
            [("VMWARE. ", False), ("Spring Boot reference documentation.", True), (" [S. l.], 2026. Disponível em: https://docs.spring.io/spring-boot/index.html. Acesso em: 21 set. 2026.", False)],
            [("VMWARE. ", False), ("Spring Data JPA reference documentation.", True), (" [S. l.], 2026. Disponível em: https://docs.spring.io/spring-data/jpa/reference/index.html. Acesso em: 21 set. 2026.", False)],
            [("RED HAT. ", False), ("Flyway documentation.", True), (" [S. l.], 2026. Disponível em: https://documentation.red-gate.com/flyway. Acesso em: 21 set. 2026.", False)]],
           sz=19, entrelinha=1.25, espaco_entre=14)


# --------------------------------------------------------------------------- #
def main():
    prs = deck_em_branco()

    slide_capa(prs)
    construtores = [
        slide_contexto, slide_motivacao, slide_objetivos,
        slide_classes, slide_mer, slide_der,
        slide_arquitetura, slide_camadas, slide_organizacao, slide_tecnologias,
        slide_funcionalidades,
        slide_endpoints, slide_controller, slide_service, slide_excecoes,
        slide_entidade, slide_repositorio,
        slide_seguranca_config, slide_perfis,
        slide_testes, slide_criterios,
        slide_documentacao, slide_casos_negativos,
        slide_consideracoes, slide_referencias,
    ]
    for i, fn in enumerate(construtores, start=2):
        fn(prs, i)

    prs.save(str(SAIDA))
    print(f"gerado: {SAIDA.name}  ({len(prs.slides._sldIdLst)} slides)")


if __name__ == "__main__":
    main()
