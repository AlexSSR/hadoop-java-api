package com.bitnei.main;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @Date 2018/5/16
 */
public class TextTransToExcel {
    private static String separator = "\t";

    public static void main(String[] args) throws IOException {
        String textFilePath = args[0];
        String excelFilePath = args[1];
        if (args.length == 3) {
            separator = args[2];
        }

        FileOutputStream outputStream = new FileOutputStream(excelFilePath);
        try {
            List<String> lines = IOUtils.readLines(new FileInputStream(textFilePath), Charset.forName("UTF-8"));
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet();

            String header = lines.remove(0);
            generatorHeader(sheet, getHeaderNames(header), generatorHeaderStyle(workbook));

            generatorCells(lines, sheet, generatorCellStyle(workbook));

            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    private static List<String> getHeaderNames(String header) {
        String[] names = header.split(separator);
        return Arrays.asList(names);
    }

    private static HSSFCellStyle generatorHeaderStyle(HSSFWorkbook workbook) {
        HSSFCellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor((short) 22);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static HSSFCellStyle generatorCellStyle(HSSFWorkbook workbook) {
        HSSFCellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static void generatorHeader(HSSFSheet sheet, List<String> cellName, HSSFCellStyle headerStyle) {
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < cellName.size(); i++) {
            sheet.setColumnWidth(i, 30 * 256);
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(cellName.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private static void generatorCells(List<String> records, HSSFSheet sheet, HSSFCellStyle cellStyle) {
        for (int i = 0; i < records.size(); i++) {
            String record = records.get(i);
            HSSFRow row = sheet.createRow(i + 1);
            String[] cellValues = record.split(separator);
            for (int j = 0; j < cellValues.length; j++) {
                String cellValue = StringUtils.isBlank(cellValues[j]) ? "" : cellValues[j];
                HSSFCell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(cellValue.trim());
            }
        }
    }
}
