package com.backend.water_management_system.billing.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.backend.water_management_system.billing.entity.Bill;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class BillDocumentService {

    public byte[] generateBillPdf(Bill bill) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new java.awt.Color(43, 59, 111));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(43, 59, 111));
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        // Header Table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2f, 1f});

        // Left Header: Mock Logo & Company Name
        PdfPCell leftHeader = new PdfPCell();
        leftHeader.setBorder(Rectangle.NO_BORDER);
        
        try {
            java.net.URL logoUrl = getClass().getResource("/static/logo.png");
            if (logoUrl != null) {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(logoUrl);
                logo.scaleAbsolute(40f, 40f);
                leftHeader.addElement(logo);
            }
        } catch (Exception e) {
            // ignore missing logo
        }

        Paragraph companyTitle = new Paragraph("NATIONAL WATER SUPPLY BOARD", titleFont);
        leftHeader.addElement(companyTitle);
        leftHeader.addElement(new Paragraph("HEAD OFFICE,colombo 05, colombo", smallFont));
        leftHeader.addElement(new Paragraph("EMAIL: info@watercompany.com", smallFont));
        headerTable.addCell(leftHeader);

        // Right Header
        PdfPCell rightHeader = new PdfPCell();
        rightHeader.setBorder(Rectangle.NO_BORDER);
        rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        String accNo = bill.getCustomer() != null ? bill.getCustomer().getSubscriptionNumber() : "N/A";
        Paragraph accNoP = new Paragraph("ACCOUNT NO.: " + accNo, boldFont);
        accNoP.setAlignment(Element.ALIGN_RIGHT);
        rightHeader.addElement(accNoP);
        headerTable.addCell(rightHeader);

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // Customer Details Row
        PdfPTable customerTable = new PdfPTable(2);
        customerTable.setWidthPercentage(100);
        
        PdfPCell c1 = new PdfPCell(new Phrase("Name: " + (bill.getCustomer() != null ? bill.getCustomer().getAccountHolderName() : "N/A"), regularFont));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase("Email: " + (bill.getCustomer() != null ? bill.getCustomer().getEmail() : "N/A"), regularFont));
        c2.setBorder(Rectangle.NO_BORDER);
        customerTable.addCell(c1);
        customerTable.addCell(c2);

        String category = bill.getCustomer() != null ? bill.getCustomer().getConnectionType() : "N/A";
        PdfPCell c3 = new PdfPCell(new Phrase("Category: " + category.toUpperCase(), boldFont));
        c3.setBorder(Rectangle.BOTTOM);
        c3.setPaddingBottom(5f);
        c3.setColspan(2);
        customerTable.addCell(c3);

        document.add(customerTable);
        document.add(new Paragraph("\n"));

        // Bill Meta Table
        PdfPTable metaTable = new PdfPTable(2); // Reduced from 3
        metaTable.setWidthPercentage(100);
        
        metaTable.addCell(createCell("Bill No.: " + bill.getBillId(), boldFont, Rectangle.BOX));
        metaTable.addCell(createCell("Billing Period: " + bill.getBillingPeriod(), regularFont, Rectangle.BOX));
        document.add(metaTable);
        document.add(new Paragraph("\n"));

        // Bill Items Table Grid
        PdfPTable itemsTable = new PdfPTable(3); // Reduced from 6
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{4f, 2f, 2f});

        // Headers
        String[] headers = {"Bill Items", "Consumption", "CHARGE (LKR)"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Phrase(h, boldFont));
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            hc.setPadding(5f);
            itemsTable.addCell(hc);
        }

        // Data Rows
        String usage = bill.getUsageUnits() != null ? bill.getUsageUnits().toString() : "0";

        // WATER
        itemsTable.addCell(createCell("WATER", boldFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellCenter(usage, regularFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellRight(formatAmt(bill.getUsageCharge()), regularFont, Rectangle.LEFT | Rectangle.RIGHT));

        // METER RENT (Base Charge)
        itemsTable.addCell(createCell("METER RENT", boldFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellCenter("", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellRight(formatAmt(bill.getBaseCharge()), regularFont, Rectangle.LEFT | Rectangle.RIGHT));

        // TAX / SEWER
        itemsTable.addCell(createCell("TAX", boldFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellCenter("", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellRight(formatAmt(bill.getTaxAmount()), regularFont, Rectangle.LEFT | Rectangle.RIGHT));

        // PENDING BALANCE
        itemsTable.addCell(createCell("BAL. B/FWD", boldFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellCenter("", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
        itemsTable.addCell(createCellRight(formatAmt(bill.getOutstandingAtIssue()), regularFont, Rectangle.LEFT | Rectangle.RIGHT));

        // Empty padding rows to make table taller
        for(int i=0; i<5; i++) {
            itemsTable.addCell(createCell(" ", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
            itemsTable.addCell(createCell(" ", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
            itemsTable.addCell(createCell(" ", regularFont, Rectangle.LEFT | Rectangle.RIGHT));
        }

        // DEBT ANALYSIS HEADER
        PdfPCell debtH = new PdfPCell(new Phrase("DEBT ANALYSIS", boldFont));
        debtH.setColspan(2);
        debtH.setHorizontalAlignment(Element.ALIGN_CENTER);
        itemsTable.addCell(debtH);
        itemsTable.addCell(createCellCenter("TOTAL DUE", boldFont, Rectangle.BOX));

        // DEBT ANALYSIS ROW
        PdfPTable nestedDebtTable = new PdfPTable(4);
        nestedDebtTable.addCell(createCellCenter("Debt > 60 days:\n0.00", regularFont, Rectangle.BOX));
        nestedDebtTable.addCell(createCellCenter("Debt < 60 days:\n0.00", regularFont, Rectangle.BOX));
        nestedDebtTable.addCell(createCellCenter("Debt < 30 days:\n" + formatAmt(bill.getOutstandingAtIssue()), regularFont, Rectangle.BOX));
        nestedDebtTable.addCell(createCellCenter("Current:\n" + formatAmt(bill.getTotalAmount()), regularFont, Rectangle.BOX));

        PdfPCell debtCell = new PdfPCell(nestedDebtTable);
        debtCell.setColspan(2);
        debtCell.setPadding(0);
        itemsTable.addCell(debtCell);
        
        BigDecimal totalDue = bill.getTotalAmount();
        if(bill.getOutstandingAtIssue() != null) {
            totalDue = totalDue.add(bill.getOutstandingAtIssue());
        }
        itemsTable.addCell(createCellRight(formatAmt(totalDue), boldFont, Rectangle.BOX));

        document.add(itemsTable);
        document.add(new Paragraph("\n"));

        // Footer details
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        
        footerTable.addCell(createCell("Billing Date: " + (bill.getBillDate() != null ? bill.getBillDate() : "N/A"), regularFont, Rectangle.NO_BORDER));
        footerTable.addCell(createCell("Due Date: " + (bill.getDueDate() != null ? bill.getDueDate() : "N/A"), boldFont, Rectangle.NO_BORDER));
        
        PdfPCell warning = new PdfPCell(new Phrase("NOTICE IS HEREBY GIVEN THAT IF THIS BILL IS NOT PAID BY THE DUE DATE, YOUR SUPPLY WILL BE DISCONNECTED WITHOUT FURTHER NOTICE.", smallFont));
        warning.setColspan(2);
        warning.setBorder(Rectangle.NO_BORDER);
        warning.setPaddingTop(10f);
        footerTable.addCell(warning);

        document.add(footerTable);

        document.close();
        return baos.toByteArray();
    }

    // PDF Helper methods
    private PdfPCell createCell(String text, Font font, int border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(border);
        cell.setPadding(5f);
        return cell;
    }
    private PdfPCell createCellCenter(String text, Font font, int border) {
        PdfPCell cell = createCell(text, font, border);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private PdfPCell createCellRight(String text, Font font, int border) {
        PdfPCell cell = createCell(text, font, border);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
    private String formatAmt(BigDecimal amt) {
        return amt != null ? String.format("%.2f", amt) : "0.00";
    }

    public byte[] generateBillImage(Bill bill) throws IOException {     
        int width = 900;
        int height = 1200;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Colors
        Color primaryBlue = new Color(43, 59, 111);
        Color textColor = Color.BLACK;

        // Fonts
        java.awt.Font titleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 28);
        java.awt.Font subtitleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 18);
        java.awt.Font boldFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 14);
        java.awt.Font regularFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 14);
        java.awt.Font smallFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 10);

        // --- Header Section ---
        try {
            java.net.URL logoUrl = getClass().getResource("/static/logo.png");
            if (logoUrl != null) {
                BufferedImage logoImg = ImageIO.read(logoUrl);
                g2d.drawImage(logoImg, 50, 40, 60, 60, null);
            } else {
                g2d.setColor(primaryBlue);
                g2d.fillRoundRect(50, 40, 60, 60, 15, 15);
            }
        } catch (Exception e) {
            // ignore missing logo
        }

        g2d.setColor(primaryBlue);
        g2d.setFont(titleFont);
        g2d.drawString("NATIONAL WATER SUPPLY BOARD", 150, 70);
        g2d.setFont(smallFont);
        g2d.setColor(textColor);
        g2d.drawString("HEAD OFFICE, KAMPALA ROAD, NAIROBI", 150, 90);
        g2d.drawString("EMAIL: info@watercompany.com", 150, 105);

        g2d.setColor(textColor);
        g2d.setFont(boldFont);
        String accNo = bill.getCustomer() != null ? bill.getCustomer().getSubscriptionNumber() : "N/A";
        g2d.drawString("ACCOUNT NO.: " + accNo, 650, 110);

        // Separator
        g2d.drawLine(50, 130, width - 50, 130);

        // --- Customer Details ---
        g2d.setFont(regularFont);
        g2d.drawString("Name: " + (bill.getCustomer() != null ? bill.getCustomer().getAccountHolderName() : "N/A"), 50, 160);
        g2d.drawString("Email: " + (bill.getCustomer() != null ? bill.getCustomer().getEmail() : "N/A"), 50, 180);
        
        String category = bill.getCustomer() != null ? bill.getCustomer().getConnectionType() : "N/A";
        g2d.setFont(boldFont);
        g2d.drawString("Category: " + category.toUpperCase(), 50, 210);
        
        g2d.drawLine(50, 220, 400, 220); // Category underline

        // --- Bill Meta ---
        int topY = 250;
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(50, topY, 800, 40);
        g2d.drawLine(450, topY, 450, topY + 40);

        g2d.setFont(boldFont);
        g2d.drawString("Bill No.: " + bill.getBillId(), 60, topY + 25);
        
        g2d.setFont(regularFont);
        g2d.drawString("Billing Period: " + bill.getBillingPeriod(), 460, topY + 25);

        // --- Main Table ---
        int tableY = 300;
        int tableHeight = 400;
        g2d.drawRect(50, tableY, 800, tableHeight);
        
        // Columns: reduced to 3 columns (Bill Items, Consumption, Charge)
        int[] cols = {50, 350, 600, 850};
        for(int c : cols) {
            g2d.drawLine(c, tableY, c, tableY + tableHeight);
        }
        
        // Header Row
        g2d.drawLine(50, tableY + 40, 850, tableY + 40);
        g2d.setFont(boldFont);
        g2d.drawString("Bill Items", 150, tableY + 25);
        g2d.drawString("Consumption", 420, tableY + 25);
        g2d.drawString("CHARGE", 680, tableY + 25);

        // Data Rows
        int dataY = tableY + 70;
        g2d.setFont(boldFont);
        g2d.drawString("WATER", 60, dataY);
        
        g2d.setFont(regularFont);
        String usage = bill.getUsageUnits() != null ? bill.getUsageUnits().toString() : "0";
        
        g2d.drawString(usage, 460, dataY);
        g2d.drawString(formatAmt(bill.getUsageCharge()), 720, dataY);

        dataY += 40;
        g2d.setFont(boldFont);
        g2d.drawString("METER RENT", 60, dataY);
        g2d.setFont(regularFont);
        g2d.drawString(formatAmt(bill.getBaseCharge()), 720, dataY);

        dataY += 40;
        g2d.setFont(boldFont);
        g2d.drawString("TAX", 60, dataY);
        g2d.setFont(regularFont);
        g2d.drawString(formatAmt(bill.getTaxAmount()), 720, dataY);

        dataY += 40;
        g2d.setFont(boldFont);
        g2d.drawString("BAL. B/FWD", 60, dataY);
        g2d.setFont(regularFont);
        g2d.drawString(formatAmt(bill.getOutstandingAtIssue()), 720, dataY);

        // Debt Analysis Table
        int debtY = tableY + tableHeight;
        g2d.drawRect(50, debtY, 800, 80);
        g2d.drawLine(50, debtY + 30, 850, debtY + 30);
        g2d.drawLine(600, debtY, 600, debtY + 80); // Total due separator
        
        // Debt Cols (spread across the 550 pixels available in the first big column)
        g2d.drawLine(187, debtY + 30, 187, debtY + 80);
        g2d.drawLine(324, debtY + 30, 324, debtY + 80);
        g2d.drawLine(461, debtY + 30, 461, debtY + 80);

        g2d.setFont(boldFont);
        g2d.drawString("DEBT ANALYSIS", 250, debtY + 20);
        g2d.drawString("TOTAL DUE", 680, debtY + 20);

        g2d.setFont(smallFont);
        g2d.drawString("Debt > 60 days:", 60, debtY + 50);
        g2d.drawString("Debt < 60 days:", 197, debtY + 50);
        g2d.drawString("Debt < 30 days:", 334, debtY + 50);
        g2d.drawString("Current:", 471, debtY + 50);

        g2d.setFont(regularFont);
        g2d.drawString("0.00", 90, debtY + 70);
        g2d.drawString("0.00", 227, debtY + 70);
        g2d.drawString(formatAmt(bill.getOutstandingAtIssue()), 364, debtY + 70);
        g2d.drawString(formatAmt(bill.getTotalAmount()), 501, debtY + 70);

        BigDecimal totalDue = bill.getTotalAmount();
        if(bill.getOutstandingAtIssue() != null) {
            totalDue = totalDue.add(bill.getOutstandingAtIssue());
        }
        g2d.setFont(boldFont);
        g2d.drawString(formatAmt(totalDue), 700, debtY + 65);

        // Footer Section
        int footerY = debtY + 120;
        g2d.setFont(regularFont);
        g2d.drawString("Billing Date: " + (bill.getBillDate() != null ? bill.getBillDate() : "N/A"), 50, footerY);
        
        g2d.setFont(boldFont);
        g2d.drawString("Due Date: " + (bill.getDueDate() != null ? bill.getDueDate() : "N/A"), 400, footerY);

        g2d.setFont(smallFont);
        g2d.drawString("NOTICE IS HEREBY GIVEN THAT IF THIS BILL IS NOT PAID BY THE DUE DATE, YOUR SUPPLY WILL BE DISCONNECTED.", 50, footerY + 50);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
