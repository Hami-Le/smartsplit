package com.smartsplit.report.service;

import com.smartsplit.balance.dto.MemberBalanceResponse;
import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.report.dto.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReportExportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReportService reportService;

    public ReportExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public GeneratedReport export(Long groupId, java.time.LocalDate from, java.time.LocalDate to, String format) {
        ReportSnapshot snapshot = reportService.getSnapshot(groupId, from, to);
        return switch (normalizeFormat(format)) {
            case "xlsx" -> new GeneratedReport(
                    createWorkbook(snapshot),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "xlsx"
            );
            case "pdf" -> new GeneratedReport(createPdf(snapshot), "application/pdf", "pdf");
            default -> throw new BusinessException(
                    "UNSUPPORTED_REPORT_FORMAT",
                    "Định dạng báo cáo chỉ hỗ trợ xlsx hoặc pdf"
            );
        };
    }

    private String normalizeFormat(String format) {
        return format == null ? "xlsx" : format.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] createWorkbook(ReportSnapshot snapshot) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            WorkbookStyles styles = new WorkbookStyles(workbook);
            writeSummarySheet(workbook, styles, snapshot.dashboard());
            writeExpenseSheet(workbook, styles, snapshot.expenses());
            writeBalanceSheet(workbook, styles, snapshot.balances());
            writeSettlementSheet(workbook, styles, snapshot.settlements());
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("REPORT_EXPORT_FAILED", "Không thể tạo báo cáo Excel");
        }
    }

    private void writeSummarySheet(
            XSSFWorkbook workbook,
            WorkbookStyles styles,
            DashboardResponse dashboard
    ) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        int rowIndex = 0;
        Row title = sheet.createRow(rowIndex++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("SMARTSPLIT - BÁO CÁO CHI TIÊU NHÓM");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        rowIndex = writeLabelValue(sheet, rowIndex, styles, "Nhóm", dashboard.groupName());
        rowIndex = writeLabelValue(
                sheet,
                rowIndex,
                styles,
                "Thời gian",
                DATE_FORMAT.format(dashboard.from()) + " - " + DATE_FORMAT.format(dashboard.to())
        );
        rowIndex = writeMoneyValue(sheet, rowIndex, styles, "Tổng chi", dashboard.totalExpense());
        rowIndex = writeLabelValue(sheet, rowIndex, styles, "Số khoản chi", dashboard.expenseCount());
        rowIndex = writeMoneyValue(sheet, rowIndex, styles, "Chi trung bình", dashboard.averageExpense());
        rowIndex = writeMoneyValue(sheet, rowIndex, styles, "Khoản chi lớn nhất", dashboard.highestExpense());
        rowIndex = writeMoneyValue(sheet, rowIndex, styles, "Đã thanh toán trong kỳ", dashboard.totalSettled());
        rowIndex = writeMoneyValue(sheet, rowIndex, styles, "Công nợ hiện tại", dashboard.outstandingAmount());
        rowIndex += 2;

        Row categoryHeader = sheet.createRow(rowIndex++);
        String[] categoryColumns = {"Danh mục", "Số khoản", "Số tiền", "Tỷ trọng"};
        writeHeader(categoryHeader, styles, categoryColumns);
        for (CategorySpendingResponse category : dashboard.categoryBreakdown()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(category.categoryName());
            row.createCell(1).setCellValue(category.expenseCount());
            Cell amount = row.createCell(2);
            amount.setCellValue(category.amount());
            amount.setCellStyle(styles.money);
            row.createCell(3).setCellValue(category.percentage().doubleValue() / 100D);
            row.getCell(3).setCellStyle(styles.percentage);
        }

        for (int column = 0; column < 4; column++) sheet.autoSizeColumn(column);
        sheet.setColumnWidth(0, Math.max(sheet.getColumnWidth(0), 7000));
    }

    private void writeExpenseSheet(
            XSSFWorkbook workbook,
            WorkbookStyles styles,
            List<ReportExpenseRow> expenses
    ) {
        Sheet sheet = workbook.createSheet("Khoản chi");
        String[] columns = {
                "Mã", "Ngày", "Khoản chi", "Danh mục", "Số tiền",
                "Người trả", "Người tham gia", "Người tạo", "Ghi chú"
        };
        writeHeader(sheet.createRow(0), styles, columns);
        int rowIndex = 1;
        for (ReportExpenseRow expense : expenses) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(expense.id());
            row.createCell(1).setCellValue(DATE_FORMAT.format(expense.expenseDate()));
            row.createCell(2).setCellValue(expense.title());
            row.createCell(3).setCellValue(expense.categoryName());
            Cell amount = row.createCell(4);
            amount.setCellValue(expense.totalAmount());
            amount.setCellStyle(styles.money);
            row.createCell(5).setCellValue(expense.payerSummary());
            row.createCell(6).setCellValue(expense.participantSummary());
            row.createCell(7).setCellValue(expense.createdByName());
            row.createCell(8).setCellValue(expense.description() == null ? "" : expense.description());
        }
        setReportColumnWidths(sheet, new int[]{2500, 3500, 6500, 4500, 4500, 9000, 9000, 5000, 9000});
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, columns.length - 1));
    }

    private void writeBalanceSheet(
            XSSFWorkbook workbook,
            WorkbookStyles styles,
            List<MemberBalanceResponse> balances
    ) {
        Sheet sheet = workbook.createSheet("Công nợ");
        String[] columns = {
                "Thành viên", "Email", "Trạng thái", "Đã trả", "Phải chịu",
                "Đã gửi", "Đã nhận", "Số dư"
        };
        writeHeader(sheet.createRow(0), styles, columns);
        int rowIndex = 1;
        for (MemberBalanceResponse balance : balances) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(balance.fullName());
            row.createCell(1).setCellValue(balance.email());
            row.createCell(2).setCellValue(balance.membershipStatus());
            setMoney(row, 3, balance.paidAmount(), styles);
            setMoney(row, 4, balance.shareAmount(), styles);
            setMoney(row, 5, balance.sentAmount(), styles);
            setMoney(row, 6, balance.receivedAmount(), styles);
            setMoney(row, 7, balance.balance(), styles);
        }
        setReportColumnWidths(sheet, new int[]{6000, 7500, 3500, 4500, 4500, 4500, 4500, 4500});
        sheet.createFreezePane(0, 1);
    }

    private void writeSettlementSheet(
            XSSFWorkbook workbook,
            WorkbookStyles styles,
            List<ReportSettlementRow> settlements
    ) {
        Sheet sheet = workbook.createSheet("Thanh toán");
        String[] columns = {
                "Mã", "Thời gian", "Người trả", "Người nhận", "Số tiền",
                "Trạng thái", "Ghi chú", "Người ghi nhận"
        };
        writeHeader(sheet.createRow(0), styles, columns);
        int rowIndex = 1;
        for (ReportSettlementRow settlement : settlements) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(settlement.id());
            row.createCell(1).setCellValue(DATE_TIME_FORMAT.format(settlement.settledAt()));
            row.createCell(2).setCellValue(settlement.payerName());
            row.createCell(3).setCellValue(settlement.receiverName());
            setMoney(row, 4, settlement.amount(), styles);
            row.createCell(5).setCellValue(settlement.status());
            row.createCell(6).setCellValue(settlement.note() == null ? "" : settlement.note());
            row.createCell(7).setCellValue(settlement.createdByName());
        }
        setReportColumnWidths(sheet, new int[]{2500, 5000, 5500, 5500, 4500, 3500, 9000, 5500});
        sheet.createFreezePane(0, 1);
    }

    private byte[] createPdf(ReportSnapshot snapshot) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DashboardResponse dashboard = snapshot.dashboard();
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();

            Font normal = createVietnameseFont(9, Font.NORMAL);
            Font bold = createVietnameseFont(9, Font.BOLD);
            Font title = createVietnameseFont(18, Font.BOLD);
            Font subtitle = createVietnameseFont(11, Font.NORMAL);

            Paragraph heading = new Paragraph("SMARTSPLIT - BÁO CÁO CHI TIÊU NHÓM", title);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);
            Paragraph groupLine = new Paragraph(
                    dashboard.groupName() + " · " + DATE_FORMAT.format(dashboard.from())
                            + " - " + DATE_FORMAT.format(dashboard.to()),
                    subtitle
            );
            groupLine.setAlignment(Element.ALIGN_CENTER);
            groupLine.setSpacingAfter(16);
            document.add(groupLine);

            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setWidths(new float[]{1, 1, 1, 1});
            addSummaryCell(summary, "Tổng chi", money(dashboard.totalExpense()), bold, normal);
            addSummaryCell(summary, "Số khoản", String.valueOf(dashboard.expenseCount()), bold, normal);
            addSummaryCell(summary, "Chi trung bình", money(dashboard.averageExpense()), bold, normal);
            addSummaryCell(summary, "Công nợ hiện tại", money(dashboard.outstandingAmount()), bold, normal);
            summary.setSpacingAfter(14);
            document.add(summary);

            document.add(sectionTitle("Chi tiêu theo danh mục", bold));
            PdfPTable categories = new PdfPTable(new float[]{4, 1, 2, 1.4F});
            categories.setWidthPercentage(100);
            addPdfHeader(categories, bold, "Danh mục", "Số khoản", "Số tiền", "Tỷ trọng");
            for (CategorySpendingResponse category : dashboard.categoryBreakdown()) {
                addPdfRow(
                        categories,
                        normal,
                        category.categoryName(),
                        String.valueOf(category.expenseCount()),
                        money(category.amount()),
                        category.percentage() + "%"
                );
            }
            categories.setSpacingAfter(14);
            document.add(categories);

            document.add(sectionTitle("Chi tiêu theo thành viên", bold));
            PdfPTable members = new PdfPTable(new float[]{3, 2, 2, 1.4F});
            members.setWidthPercentage(100);
            addPdfHeader(members, bold, "Thành viên", "Đã trả", "Phải chịu", "Tỷ trọng");
            for (MemberSpendingResponse member : dashboard.memberSpending()) {
                addPdfRow(
                        members,
                        normal,
                        member.fullName(),
                        money(member.paidAmount()),
                        money(member.shareAmount()),
                        member.sharePercentage() + "%"
                );
            }
            members.setSpacingAfter(14);
            document.add(members);

            document.add(sectionTitle("Danh sách khoản chi", bold));
            PdfPTable expenses = new PdfPTable(new float[]{1.4F, 3.8F, 2.2F, 2F, 2F});
            expenses.setWidthPercentage(100);
            expenses.setHeaderRows(1);
            addPdfHeader(expenses, bold, "Ngày", "Khoản chi", "Danh mục", "Người tạo", "Số tiền");
            for (ReportExpenseRow expense : snapshot.expenses()) {
                addPdfRow(
                        expenses,
                        normal,
                        DATE_FORMAT.format(expense.expenseDate()),
                        expense.title(),
                        expense.categoryName(),
                        expense.createdByName(),
                        money(expense.totalAmount())
                );
            }
            document.add(expenses);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("REPORT_EXPORT_FAILED", "Không thể tạo báo cáo PDF");
        }
    }

    private Font createVietnameseFont(float size, int style) {
        List<Path> candidates = List.of(
                Path.of("C:/Windows/Fonts/arial.ttf"),
                Path.of("C:/Windows/Fonts/tahoma.ttf"),
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/TTF/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf")
        );
        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) continue;
            try {
                BaseFont baseFont = BaseFont.createFont(
                        candidate.toString(),
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED
                );
                return new Font(baseFont, size, style);
            } catch (Exception ignored) {
                // Thử font tiếp theo.
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    private Paragraph sectionTitle(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(6);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(246, 248, 252));
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addPdfHeader(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setPadding(6);
            cell.setBackgroundColor(new Color(229, 235, 245));
            table.addCell(cell);
        }
    }

    private void addPdfRow(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private String money(long amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " đ";
    }

    private int writeLabelValue(
            Sheet sheet,
            int rowIndex,
            WorkbookStyles styles,
            String label,
            Object value
    ) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label);
        row.createCell(1).setCellValue(String.valueOf(value));
        return rowIndex + 1;
    }

    private int writeMoneyValue(
            Sheet sheet,
            int rowIndex,
            WorkbookStyles styles,
            String label,
            long value
    ) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(styles.money);
        return rowIndex + 1;
    }

    private void writeHeader(Row row, WorkbookStyles styles, String... columns) {
        for (int index = 0; index < columns.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(columns[index]);
            cell.setCellStyle(styles.header);
        }
    }

    private void setMoney(Row row, int column, long value, WorkbookStyles styles) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(styles.money);
    }

    private void setReportColumnWidths(Sheet sheet, int[] widths) {
        for (int index = 0; index < widths.length; index++) {
            sheet.setColumnWidth(index, widths[index]);
        }
    }

    private static final class WorkbookStyles {
        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle label;
        private final CellStyle money;
        private final CellStyle percentage;

        private WorkbookStyles(Workbook workbook) {
            title = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            title.setFont(titleFont);

            header = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);

            label = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            label.setFont(labelFont);

            money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"đ\""));

            percentage = workbook.createCellStyle();
            percentage.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        }
    }
}
