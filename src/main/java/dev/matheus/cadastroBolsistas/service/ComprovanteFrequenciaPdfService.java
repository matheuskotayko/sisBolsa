package dev.matheus.cadastroBolsistas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dev.matheus.cadastroBolsistas.model.Bolsista;
import dev.matheus.cadastroBolsistas.model.Frequencia;
import dev.matheus.cadastroBolsistas.model.Laboratorio;
import dev.matheus.cadastroBolsistas.model.Professor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/*
 * geracao de relatorios e comprovantes mensais de frequencia em formato pdf.
 */
@Service
public class ComprovanteFrequenciaPdfService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Color COR_PRIMARIA = new Color(30, 41, 59); // #1E293B
    private static final Color COR_AZUL = new Color(37, 99, 235);     // #2563EB
    private static final Color COR_FUNDO_CABECALHO = new Color(241, 245, 249); // #F1F5F9
    private static final Color COR_ZEBRADA = new Color(248, 250, 252); // #F8FAFC
    private static final Color COR_BORDA = new Color(226, 232, 240);  // #E2E8F0

    public byte[] gerarComprovante(Bolsista bolsista,
                                   Laboratorio laboratorio,
                                   Professor coordenador,
                                   List<Frequencia> frequencias,
                                   LocalDate dataInicio,
                                   LocalDate dataFim) throws DocumentException {

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);

        doc.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COR_PRIMARIA);
        Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COR_AZUL);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COR_PRIMARIA);
        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fontTextoPequeno = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font fontCabecalhoTabela = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        // Header institucional
        Paragraph pTitulo = new Paragraph("SisBolsa - Gestão de Bolsistas e Laboratórios", fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitulo);

        Paragraph pSub = new Paragraph("COMPROVANTE DE FREQUÊNCIA E RELATÓRIO DE ATIVIDADES", fontSubtitulo);
        pSub.setAlignment(Element.ALIGN_CENTER);
        pSub.setSpacingAfter(15);
        doc.add(pSub);

        // Informações do Bolsista e Laboratório
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        String periodoStr = (dataInicio != null ? dataInicio.format(FORMATO_DATA) : "Início")
                + " até " + (dataFim != null ? dataFim.format(FORMATO_DATA) : "Atual");

        adicionarCelulaInfo(infoTable, "Bolsista:", bolsista != null ? bolsista.getNome() : "-", fontLabel, fontTexto);
        adicionarCelulaInfo(infoTable, "Período de Referência:", periodoStr, fontLabel, fontTexto);

        adicionarCelulaInfo(infoTable, "Matrícula / CPF:", (bolsista != null ? bolsista.getMatricula() : "-") + " / " + (bolsista != null ? bolsista.getCpf() : "-"), fontLabel, fontTexto);
        adicionarCelulaInfo(infoTable, "Modalidade / Cargo:", (bolsista != null && bolsista.getModalidadeBolsa() != null ? bolsista.getModalidadeBolsa().getDescricao() : "Bolsa") + " (" + (bolsista != null && bolsista.getCargo() != null ? bolsista.getCargo().getDescricao() : "Bolsista") + ")", fontLabel, fontTexto);

        adicionarCelulaInfo(infoTable, "Curso:", bolsista != null ? bolsista.getCurso() : "-", fontLabel, fontTexto);
        adicionarCelulaInfo(infoTable, "Valor Mensal da Bolsa:", bolsista != null && bolsista.getValorBolsa() != null ? String.format("R$ %.2f", bolsista.getValorBolsa()) : "R$ 0,00", fontLabel, fontTexto);

        adicionarCelulaInfo(infoTable, "Laboratório:", laboratorio != null ? laboratorio.getNome() : "-", fontLabel, fontTexto);
        adicionarCelulaInfo(infoTable, "Coordenador Responsável:", coordenador != null ? coordenador.getNome() : "-", fontLabel, fontTexto);

        doc.add(infoTable);

        // Tabela de Apontamentos de Frequência
        PdfPTable freqTable = new PdfPTable(new float[]{16, 12, 47, 25});
        freqTable.setWidthPercentage(100);
        freqTable.setSpacingAfter(15);

        // Cabeçalho da tabela
        String[] cabecalhos = {"Data", "Horas", "Descrição da Atividade", "Comprovante / Link"};
        for (String c : cabecalhos) {
            PdfPCell cell = new PdfPCell(new Phrase(c, fontCabecalhoTabela));
            cell.setBackgroundColor(COR_PRIMARIA);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(COR_PRIMARIA);
            freqTable.addCell(cell);
        }

        double totalHoras = 0;
        boolean zebrada = false;

        if (frequencias == null || frequencias.isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum registro de frequência encontrado para o período informado.", fontTexto));
            vazio.setColspan(4);
            vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
            vazio.setPadding(12);
            freqTable.addCell(vazio);
        } else {
            for (Frequencia f : frequencias) {
                totalHoras += f.getHorasTrabalhadas();
                Color bg = zebrada ? COR_ZEBRADA : Color.WHITE;
                zebrada = !zebrada;

                PdfPCell cData = new PdfPCell(new Phrase(f.getData() != null ? f.getData().format(FORMATO_DATA) : "-", fontTexto));
                cData.setHorizontalAlignment(Element.ALIGN_CENTER);
                cData.setBackgroundColor(bg);
                cData.setBorderColor(COR_BORDA);
                cData.setPadding(5);
                freqTable.addCell(cData);

                PdfPCell cHoras = new PdfPCell(new Phrase(String.format("%.1fh", f.getHorasTrabalhadas()), fontLabel));
                cHoras.setHorizontalAlignment(Element.ALIGN_CENTER);
                cHoras.setBackgroundColor(bg);
                cHoras.setBorderColor(COR_BORDA);
                cHoras.setPadding(5);
                freqTable.addCell(cHoras);

                PdfPCell cDesc = new PdfPCell(new Phrase(f.getDescricao() != null ? f.getDescricao() : "-", fontTexto));
                cDesc.setBackgroundColor(bg);
                cDesc.setBorderColor(COR_BORDA);
                cDesc.setPadding(5);
                freqTable.addCell(cDesc);

                PdfPCell cLink = new PdfPCell(new Phrase(f.getLinkComprovante() != null ? f.getLinkComprovante() : "-", fontTextoPequeno));
                cLink.setBackgroundColor(bg);
                cLink.setBorderColor(COR_BORDA);
                cLink.setPadding(5);
                freqTable.addCell(cLink);
            }
        }

        doc.add(freqTable);

        // Resumo de Horas
        Paragraph pTotal = new Paragraph(String.format("Total de Horas Registradas no Período: %.1f horas", totalHoras), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COR_PRIMARIA));
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        pTotal.setSpacingAfter(35);
        doc.add(pTotal);

        // Bloco de Assinaturas
        PdfPTable assinaturas = new PdfPTable(2);
        assinaturas.setWidthPercentage(100);
        assinaturas.setSpacingAfter(25);

        PdfPCell assBolsista = new PdfPCell();
        assBolsista.setBorder(PdfPCell.NO_BORDER);
        assBolsista.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pLinha1 = new Paragraph("_____________________________________________\nAssinatura do Bolsista", fontTexto);
        pLinha1.setAlignment(Element.ALIGN_CENTER);
        assBolsista.addElement(pLinha1);
        assinaturas.addCell(assBolsista);

        PdfPCell assCoord = new PdfPCell();
        assCoord.setBorder(PdfPCell.NO_BORDER);
        assCoord.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pLinha2 = new Paragraph("_____________________________________________\nAssinatura do Coordenador do Laboratório", fontTexto);
        pLinha2.setAlignment(Element.ALIGN_CENTER);
        assCoord.addElement(pLinha2);
        assinaturas.addCell(assCoord);

        doc.add(assinaturas);

        // Rodapé de Validação
        Paragraph pRodape = new Paragraph(
                "Documento emitido eletronicamente pelo Sistema SisBolsa em " + LocalDateTime.now().format(FORMATO_DATA_HORA) + ".\n"
                        + "Código de autenticidade: " + UUID.randomUUID().toString().toUpperCase(),
                fontTextoPequeno);
        pRodape.setAlignment(Element.ALIGN_CENTER);
        doc.add(pRodape);

        doc.close();
        return out.toByteArray();
    }

    private void adicionarCelulaInfo(PdfPTable table, String label, String valor, Font fLabel, Font fValor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COR_FUNDO_CABECALHO);
        cell.setBorderColor(COR_BORDA);
        cell.setPadding(6);

        Paragraph p = new Paragraph();
        p.add(new Phrase(label + " ", fLabel));
        p.add(new Phrase(valor != null ? valor : "-", fValor));
        cell.addElement(p);

        table.addCell(cell);
    }
}
