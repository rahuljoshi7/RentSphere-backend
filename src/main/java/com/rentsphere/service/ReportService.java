package com.rentsphere.service;

import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.ReportResponse;
import com.rentsphere.entity.*;
import com.rentsphere.entity.Payment.PaymentStatus;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PaymentRepository    paymentRepository;
    private final PropertyRepository   propertyRepository;
    private final TenantRepository     tenantRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final ReportRepository     reportRepository;
    private final UserRepository       userRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Revenue Report ────────────────────────────────────────────────────────

    public byte[] generateRevenueReport(Long userId, LocalDate from, LocalDate to, Report.ReportFormat fmt) {
        // DB-filtered query — no in-memory scan
        List<Payment> payments = isAdmin(userId)
            ? paymentRepository.findPaidInRange(from, to)
            : paymentRepository.findPaidInRangeForOwner(userId, from, to);

        String[] headers = {"Property", "Tenant", "Month/Year", "Amount Paid", "Paid Date"};
        String[][] rows = payments.stream().map(p -> new String[]{
            p.getProperty().getName(),
            p.getTenant().getFullName(),
            p.getMonth() + "/" + p.getYear(),
            "₹" + p.getAmountPaid(),
            p.getPaidDate().format(FMT)
        }).toArray(String[][]::new);

        BigDecimal total = payments.stream()
            .map(Payment::getAmountPaid)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String title    = "Revenue Report (" + from.format(FMT) + " – " + to.format(FMT) + ")";
        String[] footer = {"", "", "Total Revenue", "₹" + total, ""};

        byte[] data = fmt == Report.ReportFormat.PDF
            ? buildPdf(title, headers, rows, footer)
            : buildExcel(title, headers, rows, footer);

        saveHistory(userId, Report.ReportType.REVENUE, fmt);
        return data;
    }

    // ── Occupancy Report ──────────────────────────────────────────────────────

    public byte[] generateOccupancyReport(Long userId, Report.ReportFormat fmt) {
        // Targeted DB query — no full table scan
        List<Property> properties = isAdmin(userId)
            ? propertyRepository.findAll()                       // admin sees all (still a targeted model, no in-memory filter)
            : propertyRepository.findByOwnerId(userId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        String[] headers = {"Property", "Type", "City", "Status", "Rent Amount"};
        String[][] rows = properties.stream().map(p -> new String[]{
            p.getName(),
            p.getPropertyType().name(),
            p.getCity(),
            p.getAvailabilityStatus().name(),
            "₹" + p.getRentAmount()
        }).toArray(String[][]::new);

        long occupied = properties.stream()
            .filter(p -> p.getAvailabilityStatus() == Property.AvailabilityStatus.OCCUPIED)
            .count();

        String title    = "Occupancy Report – " + LocalDate.now().format(FMT);
        String[] footer = {"Total: " + properties.size(), "", "Occupied: " + occupied,
            "Vacant: " + (properties.size() - occupied), ""};

        byte[] data = fmt == Report.ReportFormat.PDF
            ? buildPdf(title, headers, rows, footer)
            : buildExcel(title, headers, rows, footer);

        saveHistory(userId, Report.ReportType.OCCUPANCY, fmt);
        return data;
    }

    // ── Rent Collection Report ────────────────────────────────────────────────

    public byte[] generateRentCollectionReport(Long userId, int month, int year, Report.ReportFormat fmt) {
        // DB-filtered query — no in-memory month/year/owner scan
        List<Payment> payments = isAdmin(userId)
            ? paymentRepository.findByMonthAndYear(month, year)
            : paymentRepository.findByMonthAndYearForOwner(userId, month, year);

        String[] headers = {"Property", "Tenant", "Amount Due", "Amount Paid", "Status", "Due Date"};
        String[][] rows = payments.stream().map(p -> new String[]{
            p.getProperty().getName(),
            p.getTenant().getFullName(),
            "₹" + p.getAmountDue(),
            "₹" + p.getAmountPaid(),
            p.getStatus().name(),
            p.getDueDate().format(FMT)
        }).toArray(String[][]::new);

        BigDecimal collected = payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.PAID)
            .map(Payment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = payments.stream()
            .filter(p -> p.getStatus() != PaymentStatus.PAID)
            .map(Payment::getAmountDue).reduce(BigDecimal.ZERO, BigDecimal::add);

        String title    = "Rent Collection – " + month + "/" + year;
        String[] footer = {"", "", "Collected: ₹" + collected, "Pending: ₹" + pending, "", ""};

        byte[] data = fmt == Report.ReportFormat.PDF
            ? buildPdf(title, headers, rows, footer)
            : buildExcel(title, headers, rows, footer);

        saveHistory(userId, Report.ReportType.RENT_COLLECTION, fmt);
        return data;
    }

    // ── Tenant Report ─────────────────────────────────────────────────────────

    public byte[] generateTenantReport(Long userId, Report.ReportFormat fmt) {
        List<Tenant> tenants = isAdmin(userId)
            ? tenantRepository.findAll()
            : tenantRepository.findAll().stream()
                .filter(t -> t.getAgreements().stream()
                    .anyMatch(a -> a.getProperty().getOwner().getId().equals(userId)
                        && a.isActive()))
                .toList();

        String[] headers = {"Name", "Email", "Phone", "Emergency Contact", "Active Since"};
        String[][] rows = tenants.stream().map(t -> new String[]{
            t.getFullName(),
            t.getEmail(),
            t.getUser().getPhone() != null ? t.getUser().getPhone() : "—",
            t.getEmergencyContactName() != null ? t.getEmergencyContactName() : "—",
            t.getCreatedAt().toString().substring(0, 10)
        }).toArray(String[][]::new);

        String title    = "Tenant Report – " + LocalDate.now().format(FMT);
        String[] footer = {"Total Tenants: " + tenants.size(), "", "", "", ""};

        byte[] data = fmt == Report.ReportFormat.PDF
            ? buildPdf(title, headers, rows, footer)
            : buildExcel(title, headers, rows, footer);

        saveHistory(userId, Report.ReportType.TENANT, fmt);
        return data;
    }

    // ── Maintenance Report ────────────────────────────────────────────────────

    public byte[] generateMaintenanceReport(Long userId, LocalDate from, LocalDate to, Report.ReportFormat fmt) {
        // DB-filtered query — no in-memory date/owner scan
        List<MaintenanceRequest> requests = isAdmin(userId)
            ? maintenanceRepository.findByCreatedAtBetween(
                from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                to.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            : maintenanceRepository.findByOwnerAndCreatedAtBetween(
                userId,
                from.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                to.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

        String[] headers = {"Property", "Tenant", "Title", "Priority", "Status", "Raised On"};
        String[][] rows = requests.stream().map(r -> new String[]{
            r.getProperty().getName(),
            r.getTenant().getFullName(),
            r.getTitle(),
            r.getPriority().name(),
            r.getStatus().name(),
            r.getCreatedAt().toString().substring(0, 10)
        }).toArray(String[][]::new);

        long completed = requests.stream().filter(MaintenanceRequest::isCompleted).count();
        String title    = "Maintenance Report (" + from.format(FMT) + " – " + to.format(FMT) + ")";
        String[] footer = {"Total: " + requests.size(), "", "", "Completed: " + completed,
            "Open: " + (requests.size() - completed), ""};

        byte[] data = fmt == Report.ReportFormat.PDF
            ? buildPdf(title, headers, rows, footer)
            : buildExcel(title, headers, rows, footer);

        saveHistory(userId, Report.ReportType.MAINTENANCE, fmt);
        return data;
    }

    // ── History ───────────────────────────────────────────────────────────────

    public PagedResponse<ReportResponse> getHistory(Long userId, int page, int size) {
        var result = reportRepository.findByGeneratedByIdOrderByGeneratedAtDesc(
            userId, PageRequest.of(page, size, Sort.by("generatedAt").descending()));
        return PagedResponse.of(result.map(ReportResponse::from));
    }

    // ── PDF Builder ───────────────────────────────────────────────────────────

    private byte[] buildPdf(String title, String[] headers, String[][] rows, String[] footer) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin   = 40f;
                float yStart   = page.getMediaBox().getHeight() - margin;
                float colWidth = (page.getMediaBox().getWidth() - 2 * margin) / headers.length;
                float rowH     = 18f;
                float y        = yStart;

                // Title
                cs.beginText();
                cs.setFont(bold, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText(title);
                cs.endText();
                y -= 28f;

                // Generated date
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generated: " + LocalDate.now().format(FMT));
                cs.endText();
                y -= 20f;

                // Header row
                cs.setNonStrokingColor(0.2f, 0.2f, 0.6f);
                cs.addRect(margin, y - rowH + 4, page.getMediaBox().getWidth() - 2 * margin, rowH);
                cs.fill();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.beginText();
                cs.setFont(bold, 9);
                cs.newLineAtOffset(margin + 4, y - 8);
                for (String h : headers) {
                    cs.showText(truncate(h, 18));
                    cs.newLineAtOffset(colWidth, 0);
                }
                cs.endText();
                y -= rowH;

                // Data rows
                cs.setNonStrokingColor(0f, 0f, 0f);
                boolean shade = false;
                for (String[] row : rows) {
                    if (y < margin + rowH * 3) {
                        cs.close();
                        PDPage nextPage = new PDPage(PDRectangle.A4);
                        doc.addPage(nextPage);
                        y = nextPage.getMediaBox().getHeight() - margin;
                    }
                    if (shade) {
                        cs.setNonStrokingColor(0.95f, 0.95f, 0.95f);
                        cs.addRect(margin, y - rowH + 4, page.getMediaBox().getWidth() - 2 * margin, rowH);
                        cs.fill();
                        cs.setNonStrokingColor(0f, 0f, 0f);
                    }
                    cs.beginText();
                    cs.setFont(regular, 8);
                    cs.newLineAtOffset(margin + 4, y - 8);
                    for (String cell : row) {
                        cs.showText(truncate(cell != null ? cell : "—", 20));
                        cs.newLineAtOffset(colWidth, 0);
                    }
                    cs.endText();
                    y -= rowH;
                    shade = !shade;
                }

                // Footer
                y -= 10f;
                cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
                cs.addRect(margin, y - rowH + 4, page.getMediaBox().getWidth() - 2 * margin, rowH);
                cs.fill();
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.beginText();
                cs.setFont(bold, 9);
                cs.newLineAtOffset(margin + 4, y - 8);
                for (String f : footer) {
                    cs.showText(truncate(f != null ? f : "", 22));
                    cs.newLineAtOffset(colWidth, 0);
                }
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("PDF generation failed: {}", e.getMessage());
            throw new BusinessException("Failed to generate PDF report.");
        }
    }

    // ── Excel Builder ─────────────────────────────────────────────────────────

    private byte[] buildExcel(String title, String[] headers, String[][] rows, String[] footer) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Report");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle footerStyle = wb.createCellStyle();
            Font footerFont = wb.createFont();
            footerFont.setBold(true);
            footerStyle.setFont(footerFont);
            footerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            footerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            sheet.createRow(1).createCell(0).setCellValue("Generated: " + LocalDate.now().format(FMT));

            Row hRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (int r = 0; r < rows.length; r++) {
                Row dataRow = sheet.createRow(4 + r);
                for (int c = 0; c < rows[r].length; c++) {
                    Cell cell = dataRow.createCell(c);
                    cell.setCellValue(rows[r][c] != null ? rows[r][c] : "—");
                    if (r % 2 == 1) cell.setCellStyle(altStyle);
                }
            }

            Row fRow = sheet.createRow(4 + rows.length + 1);
            for (int i = 0; i < footer.length; i++) {
                Cell c = fRow.createCell(i);
                c.setCellValue(footer[i] != null ? footer[i] : "");
                c.setCellStyle(footerStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel generation failed: {}", e.getMessage());
            throw new BusinessException("Failed to generate Excel report.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveHistory(Long userId, Report.ReportType type, Report.ReportFormat fmt) {
        userRepository.findById(userId).ifPresent(user -> {
            reportRepository.save(Report.builder()
                .generatedBy(user)
                .reportType(type)
                .format(fmt)
                .build());
        });
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.hasRole(Role.RoleName.ADMIN))
            .orElse(false);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
