package com.coffeeshop.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * One-sheet .xlsx report: a title block, a header row and data rows. Numbers stay numbers (sum-able in Excel),
 * dates are written as dd/MM/yyyy text the way the screens show them.
 */
public final class ExcelSheet {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final Sheet sheet;
    private final CellStyle titleStyle;
    private final CellStyle headerStyle;
    private final CellStyle moneyStyle;
    private final CellStyle boldMoneyStyle;
    private int rowIndex;
    private int columns;

    public ExcelSheet(String sheetName) {
        sheet = workbook.createSheet(sheetName);
        Font bold = workbook.createFont();
        bold.setBold(true);
        Font big = workbook.createFont();
        big.setBold(true);
        big.setFontHeightInPoints((short) 14);
        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(big);
        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bold);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setWrapText(true);
        DataFormat format = workbook.createDataFormat();
        moneyStyle = workbook.createCellStyle();
        moneyStyle.setDataFormat(format.getFormat("#,##0.##"));
        boldMoneyStyle = workbook.createCellStyle();
        boldMoneyStyle.setDataFormat(format.getFormat("#,##0.##"));
        boldMoneyStyle.setFont(bold);
    }

    public ExcelSheet title(String text) {
        Row row = sheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(titleStyle);
        return this;
    }

    /** "Label: value" lines under the title. */
    public ExcelSheet line(String text) {
        sheet.createRow(rowIndex++).createCell(0).setCellValue(text);
        return this;
    }

    public ExcelSheet header(String... names) {
        if (rowIndex > 0) rowIndex++;
        columns = names.length;
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < names.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(names[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, rowIndex);
        return this;
    }

    public ExcelSheet row(Object... values) {
        write(sheet.createRow(rowIndex++), values, false);
        return this;
    }

    /** a bold total row */
    public ExcelSheet total(Object... values) {
        write(sheet.createRow(rowIndex++), values, true);
        return this;
    }

    public ExcelSheet rows(List<Object[]> rows) {
        rows.forEach(this::row);
        return this;
    }

    public ExcelSheet mergeTitle() {
        if (columns > 1) sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns - 1));
        return this;
    }

    public byte[] toBytes() {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i), 2800), 60 * 256));
        }
        try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** {@code fileName} without extension; sent as an attachment. */
    public ResponseEntity<byte[]> download(String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName + ".xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(toBytes());
    }

    private void write(Row row, Object[] values, boolean bold) {
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            if (v == null) continue;
            Cell cell = row.createCell(i);
            if (v instanceof BigDecimal d) {
                cell.setCellValue(d.doubleValue());
                cell.setCellStyle(bold ? boldMoneyStyle : moneyStyle);
            } else if (v instanceof Number n) {
                cell.setCellValue(n.doubleValue());
                cell.setCellStyle(bold ? boldMoneyStyle : moneyStyle);
            } else if (v instanceof LocalDateTime t) {
                cell.setCellValue(DATE_TIME.format(t));
            } else if (v instanceof LocalDate d) {
                cell.setCellValue(DATE.format(d));
            } else if (v instanceof TemporalAccessor t) {
                cell.setCellValue(t.toString());
            } else {
                cell.setCellValue(v.toString());
                if (bold) cell.setCellStyle(headerStyle);
            }
        }
    }
}
