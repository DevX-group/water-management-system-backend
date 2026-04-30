package com.backend.water_management_system.service;

import com.backend.water_management_system.entity.Bill;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class BillDocumentService {

    public byte[] generateBillPdf(Bill bill) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);

        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        com.lowagie.text.Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        com.lowagie.text.Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        // Title
        Paragraph title = new Paragraph("WATER BILL INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // Bill Header
        document.add(new Paragraph("Subscription Number: " + (bill.getCustomer() != null ? bill.getCustomer().getSubscriptionNumber() : "N/A"), regularFont));
        document.add(new Paragraph("Customer Name: " + (bill.getCustomer() != null ? bill.getCustomer().getAccountHolderName() : "N/A"), regularFont));
        document.add(new Paragraph("Billing Period: " + (bill.getBillingPeriod() != null ? bill.getBillingPeriod() : "N/A"), regularFont));
        document.add(new Paragraph("Bill Date: " + (bill.getBillDate() != null ? bill.getBillDate().toString() : "N/A"), regularFont));
        document.add(new Paragraph("Due Date: " + (bill.getDueDate() != null ? bill.getDueDate().toString() : "N/A"), boldFont));
        document.add(new Paragraph("Status: " + (bill.getStatus() != null ? bill.getStatus() : "N/A"), boldFont));
        document.add(new Paragraph("\n"));

        // Usage Info
        document.add(new Paragraph("Usage Summary", subtitleFont));
        document.add(new Paragraph("Usage Units: " + (bill.getUsageUnits() != null ? bill.getUsageUnits() : 0) + " units", regularFont));
        document.add(new Paragraph("\n"));

        // Cost Breakdown
        document.add(new Paragraph("Charges Breakdown", subtitleFont));
        document.add(new Paragraph("Base Charge: LKR " + String.format("%.2f", bill.getBaseCharge() != null ? bill.getBaseCharge() : 0.0), regularFont));
        if (bill.getUsageCharge() != null && bill.getUsageCharge().doubleValue() > 0) {
            document.add(new Paragraph("Usage Charge: LKR " + String.format("%.2f", bill.getUsageCharge()), regularFont));
        }
        document.add(new Paragraph("Tax Amount: LKR " + String.format("%.2f", bill.getTaxAmount() != null ? bill.getTaxAmount() : 0.0), regularFont));
        document.add(new Paragraph("\n"));

        Paragraph total = new Paragraph("Total Amount: LKR " + String.format("%.2f", bill.getTotalAmount() != null ? bill.getTotalAmount() : 0.0), titleFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();

        return baos.toByteArray();
    }

    public byte[] generateBillImage(Bill bill) throws IOException {
        int width = 800;
        int height = 1000;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Colors
        Color primaryColor = new Color(14, 165, 233); // Cyan/Blue
        Color textColor = new Color(30, 41, 59); // Slate 800
        Color mutedColor = new Color(100, 116, 139); // Slate 500

        // Fonts
        java.awt.Font titleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 36);
        java.awt.Font subtitleFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 24);
        java.awt.Font regularFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 18);
        java.awt.Font boldFont = new java.awt.Font("Arial", java.awt.Font.BOLD, 18);

        // Header Background
        g2d.setColor(primaryColor);
        g2d.fillRect(0, 0, width, 120);

        // Header Text
        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString("WATER BILL INVOICE", 50, 70);

        int y = 180;
        int x = 50;

        g2d.setColor(textColor);
        g2d.setFont(boldFont);
        g2d.drawString("Customer Details", x, y);
        y += 30;

        g2d.setFont(regularFont);
        g2d.setColor(mutedColor);
        g2d.drawString("Subscription Number: ", x, y);
        g2d.setColor(textColor);
        g2d.drawString(bill.getCustomer() != null ? bill.getCustomer().getSubscriptionNumber() : "N/A", x + 200, y);
        y += 30;

        g2d.setColor(mutedColor);
        g2d.drawString("Customer Name: ", x, y);
        g2d.setColor(textColor);
        g2d.drawString(bill.getCustomer() != null ? bill.getCustomer().getAccountHolderName() : "N/A", x + 200, y);
        y += 50;

        // Bill details
        int rightX = 450;
        int topY = 180;

        g2d.setFont(boldFont);
        g2d.drawString("Bill Information", rightX, topY);
        topY += 30;

        g2d.setFont(regularFont);
        g2d.setColor(mutedColor);
        g2d.drawString("Billing Period: ", rightX, topY);
        g2d.setColor(textColor);
        g2d.drawString(bill.getBillingPeriod() != null ? bill.getBillingPeriod() : "N/A", rightX + 150, topY);
        topY += 30;

        g2d.setColor(mutedColor);
        g2d.drawString("Bill Date: ", rightX, topY);
        g2d.setColor(textColor);
        g2d.drawString(bill.getBillDate() != null ? bill.getBillDate().toString() : "N/A", rightX + 150, topY);
        topY += 30;

        g2d.setColor(mutedColor);
        g2d.drawString("Due Date: ", rightX, topY);
        g2d.setColor(new Color(225, 29, 72)); // Red 600
        g2d.setFont(boldFont);
        g2d.drawString(bill.getDueDate() != null ? bill.getDueDate().toString() : "N/A", rightX + 150, topY);
        topY += 30;

        g2d.setColor(mutedColor);
        g2d.setFont(regularFont);
        g2d.drawString("Status: ", rightX, topY);
        String status = bill.getStatus() != null ? bill.getStatus() : "N/A";
        g2d.setColor(status.equalsIgnoreCase("PAID") ? new Color(22, 163, 74) : new Color(202, 138, 4));
        g2d.setFont(boldFont);
        g2d.drawString(status, rightX + 150, topY);

        y = Math.max(y, topY) + 60;

        // Line separator
        g2d.setColor(new Color(226, 232, 240));
        g2d.drawLine(x, y, width - 50, y);
        y += 40;

        // Charges
        g2d.setColor(textColor);
        g2d.setFont(subtitleFont);
        g2d.drawString("Charges Breakdown", x, y);
        y += 40;

        g2d.setFont(regularFont);
        g2d.setColor(mutedColor);
        g2d.drawString("Usage Units (" + (bill.getUsageUnits() != null ? bill.getUsageUnits() : 0) + ")", x, y);
        g2d.setColor(textColor);
        g2d.drawString("LKR " + String.format("%.2f", bill.getUsageCharge() != null ? bill.getUsageCharge() : 0.0), width - 250, y);
        y += 30;

        g2d.setColor(mutedColor);
        g2d.drawString("Base Charge", x, y);
        g2d.setColor(textColor);
        g2d.drawString("LKR " + String.format("%.2f", bill.getBaseCharge() != null ? bill.getBaseCharge() : 0.0), width - 250, y);
        y += 30;

        g2d.setColor(mutedColor);
        g2d.drawString("Tax Amount", x, y);
        g2d.setColor(textColor);
        g2d.drawString("LKR " + String.format("%.2f", bill.getTaxAmount() != null ? bill.getTaxAmount() : 0.0), width - 250, y);
        y += 40;

        // Line separator
        g2d.setColor(new Color(226, 232, 240));
        g2d.drawLine(x, y, width - 50, y);
        y += 40;

        // Total
        g2d.setColor(primaryColor);
        g2d.fillRect(x, y, width - 100, 80);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(subtitleFont);
        g2d.drawString("Total Amount Due", x + 30, y + 50);
        
        g2d.setFont(titleFont);
        g2d.drawString("LKR " + String.format("%.2f", bill.getTotalAmount() != null ? bill.getTotalAmount() : 0.0), width - 350, y + 55);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
