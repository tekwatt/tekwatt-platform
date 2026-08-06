package com.tekwatt.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.reporting.entity.ReportType;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class PdfReportGenerator {
  private static final float MARGIN = 36;
  private static final float ROW_HEIGHT = 22;
  private static final int ROWS_PER_PAGE = 18;
  private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
  private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

  public byte[] generate(ReportType type, JsonNode data) {
    List<JsonNode> rows = new ArrayList<>();
    if (type == ReportType.DAILY && data.isArray()) data.forEach(rows::add);
    else rows.add(data);
    LinkedHashSet<String> headers = new LinkedHashSet<>();
    rows.forEach(row -> row.fieldNames().forEachRemaining(headers::add));
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      int totalPages = Math.max(1, (rows.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
      for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
          drawHeader(stream, type, pageIndex + 1, totalPages);
          List<JsonNode> pageRows = rows.subList(
              Math.min(pageIndex * ROWS_PER_PAGE, rows.size()),
              Math.min((pageIndex + 1) * ROWS_PER_PAGE, rows.size()));
          drawTable(stream, page, headers, pageRows);
          drawFooter(stream, page, pageIndex + 1);
        }
      }
      document.save(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to generate PDF report", ex);
    }
  }

  private void drawHeader(PDPageContentStream stream, ReportType type, int page, int total) throws IOException {
    stream.setNonStrokingColor(new Color(9, 107, 92));
    stream.addRect(0, PDRectangle.A4.getWidth() - 78, PDRectangle.A4.getHeight(), 78);
    stream.fill();
    text(stream, bold, 19, Color.WHITE, MARGIN, PDRectangle.A4.getWidth() - 48, "TekWatt " + type.name() + " Report");
    text(stream, regular, 9, Color.WHITE, MARGIN, PDRectangle.A4.getWidth() - 65, "EV Charging Platform  |  Page " + page + " of " + total);
  }

  private void drawTable(PDPageContentStream stream, PDPage page, LinkedHashSet<String> headers, List<JsonNode> rows) throws IOException {
    if (headers.isEmpty()) {
      text(stream, regular, 11, Color.DARK_GRAY, MARGIN, page.getMediaBox().getHeight() - 120, "No data available for this period.");
      return;
    }
    float width = page.getMediaBox().getWidth() - 2 * MARGIN;
    float columnWidth = width / headers.size();
    float y = page.getMediaBox().getHeight() - 110;
    stream.setNonStrokingColor(new Color(234, 247, 244));
    stream.addRect(MARGIN, y - ROW_HEIGHT, width, ROW_HEIGHT);
    stream.fill();
    int column = 0;
    for (String header : headers) cellText(stream, bold, header, MARGIN + column++ * columnWidth, y - 15, columnWidth);
    y -= ROW_HEIGHT;
    for (JsonNode row : rows) {
      if (((int) ((page.getMediaBox().getHeight() - 110 - y) / ROW_HEIGHT)) % 2 == 1) {
        stream.setNonStrokingColor(new Color(248, 250, 252));
        stream.addRect(MARGIN, y - ROW_HEIGHT, width, ROW_HEIGHT);
        stream.fill();
      }
      column = 0;
      for (String header : headers) cellText(stream, regular, row.path(header).isMissingNode() ? "" : row.path(header).asText(), MARGIN + column++ * columnWidth, y - 15, columnWidth);
      y -= ROW_HEIGHT;
    }
    stream.setStrokingColor(new Color(190, 200, 212));
    stream.setLineWidth(.5f);
    int lines = rows.size() + 1;
    for (int i = 0; i <= headers.size(); i++) {
      stream.moveTo(MARGIN + i * columnWidth, page.getMediaBox().getHeight() - 110);
      stream.lineTo(MARGIN + i * columnWidth, page.getMediaBox().getHeight() - 110 - lines * ROW_HEIGHT);
    }
    for (int i = 0; i <= lines; i++) {
      stream.moveTo(MARGIN, page.getMediaBox().getHeight() - 110 - i * ROW_HEIGHT);
      stream.lineTo(MARGIN + width, page.getMediaBox().getHeight() - 110 - i * ROW_HEIGHT);
    }
    stream.stroke();
  }

  private void cellText(PDPageContentStream stream, PDFont font, String value, float x, float y, float width) throws IOException {
    String safe = ascii(value);
    int max = Math.max(3, (int) (width / 4.8f));
    if (safe.length() > max) safe = safe.substring(0, max - 3) + "...";
    text(stream, font, 7, new Color(30, 41, 59), x + 4, y, safe);
  }

  private void drawFooter(PDPageContentStream stream, PDPage page, int number) throws IOException {
    text(stream, regular, 8, Color.GRAY, MARGIN, 20, "Generated by TekWatt Reporting Service  |  Page " + number);
  }

  private void text(PDPageContentStream stream, PDFont font, float size, Color color, float x, float y, String value) throws IOException {
    stream.beginText();
    stream.setFont(font, size);
    stream.setNonStrokingColor(color);
    stream.newLineAtOffset(x, y);
    stream.showText(ascii(value));
    stream.endText();
  }

  private String ascii(String value) {
    return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
  }
}
