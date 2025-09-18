package util;

import model.Equipment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    /**
     * Экспортирует список equipment в указанный файл .xlsx.
     * Бросает IOException в случае ошибки записи.
     */
    public static void exportToExcel(List<Equipment> items, File file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Equipment");

            // Шрифт и стиль заголовка
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            // Заголовки
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Название", "Серийный номер", "Ответственный", "Расположение"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowIdx = 1;
            for (Equipment e : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getId());
                row.createCell(1).setCellValue(nullToEmpty(e.getName()));
                row.createCell(2).setCellValue(nullToEmpty(e.getSerialNumber()));
                row.createCell(3).setCellValue(nullToEmpty(e.getDescription()));
                row.createCell(4).setCellValue(nullToEmpty(e.getLocation()));
            }

            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Записываем в файл
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
