package br.com.aula.gerador.infrastructure.extractor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class XlsxTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String extension) {
        return "xlsx".equalsIgnoreCase(extension);
    }

    @Override
    public String extract(byte[] content) throws IOException {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter(new Locale("pt", "BR"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            for (Sheet sheet : workbook) {
                sb.append("# Planilha: ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    boolean firstCell = true;
                    for (Cell cell : row) {
                        if (!firstCell) {
                            sb.append('\t');
                        }
                        sb.append(formatter.formatCellValue(cell));
                        firstCell = false;
                    }
                    sb.append('\n');
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
