package com.backend.water_management_system.payments.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.backend.water_management_system.payments.dto.BankSlipExtractResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AIBankSlipVisionService {

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${GEMINI_MODEL}")
    private String geminiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public BankSlipExtractResponse extractSlipData(MultipartFile file) {

        try {

            // 1. Resolve image bytes and media type (handles PDF → PNG conversion)
            String[] resolved = resolveImageBytes(file);
            String base64Image = resolved[0];
            String mediaType = resolved[1];

            // 2. Send image to Gemini
            String jsonResult = callGeminiVision(base64Image, mediaType);
            log.info("Gemini raw response: {}", jsonResult);

            // 3. Parse Gemini response
            JsonNode root = objectMapper.readTree(jsonResult);

            BigDecimal amount = parseAmount(root);
            LocalDate bankPaymentDate = parsePaymentDate(root);
            String bankReference = parseBankReference(root);

            // 4. Build and return the response
            return buildResponse(amount, bankPaymentDate, bankReference);

        } catch (Exception e) {
            log.error("Failed to extract data from bank slip: {}", e.getMessage(), e);

            return BankSlipExtractResponse.builder()
                    .extracted(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    // Converts the uploaded file to a Base64-encoded PNG (converting PDF if needed)
    // and resolves the MIME type.
    private String[] resolveImageBytes(MultipartFile file) throws Exception {

        String contentType = file.getContentType();
        byte[] imageBytes;
        String mediaType;

        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            imageBytes = convertPdfToPng(file.getBytes());
            mediaType = "image/png";
        } else {
            imageBytes = file.getBytes();
            mediaType = (contentType != null && !contentType.isBlank()) ? contentType : "image/png";
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return new String[] { base64Image, mediaType };
    }

    // Parses the payment amount from the Gemini JSON response node.
    private BigDecimal parseAmount(JsonNode root) {

        if (!root.hasNonNull("amount")) {
            return null;
        }

        try {
            String amountText = root.get("amount")
                    .asText()
                    .replaceAll("[^0-9.]", "");

            return amountText.isEmpty() ? null : new BigDecimal(amountText);

        } catch (Exception e) {
            log.warn("Could not parse amount from Gemini response: {}", root.get("amount"));
            return null;
        }
    }

    // Parses the bank payment date from the Gemini JSON response node.
    private LocalDate parsePaymentDate(JsonNode root) {

        if (!root.hasNonNull("bankPaymentDate")) {
            return null;
        }

        // If AI cannot find the date, parseDateSafely returns null.
        return parseDateSafely(root.get("bankPaymentDate").asText());
    }

    // Parses the bank reference from the Gemini JSON response node.
    private String parseBankReference(JsonNode root) {

        if (!root.hasNonNull("bankReference")) {
            return null;
        }

        String rawReference = root.get("bankReference").asText().trim();
        return (!rawReference.equalsIgnoreCase("null") && !rawReference.isBlank())
                ? rawReference
                : null;
    }

    // Assembles the final {@link BankSlipExtractResponse}.
    private BankSlipExtractResponse buildResponse(
            BigDecimal amount, LocalDate bankPaymentDate, String bankReference) {

        boolean extracted = amount != null || bankPaymentDate != null || bankReference != null;

        return BankSlipExtractResponse.builder()
                .amount(amount)
                .bankPaymentDate(bankPaymentDate)
                .bankReference(bankReference)
                .extracted(extracted)
                .message(extracted
                        ? "Bank slip processed successfully"
                        : "Could not read payment details from slip.")
                .build();
    }

    private LocalDate parseDateSafely(String dateStr) {

        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("null")) {
            return null;
        }

        String clean = dateStr.trim();

        // YYYY-MM-DD
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            log.debug("Could not parse date '{}' as YYYY-MM-DD: {}", clean, e.getMessage());
        }

        // DD/MM/YYYY
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            return LocalDate.parse(clean, formatter);

        } catch (Exception e) {
            log.debug("Could not parse date '{}' as DD/MM/YYYY: {}", clean, e.getMessage());
        }

        // DD-MM-YYYY
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            return LocalDate.parse(clean, formatter);

        } catch (Exception e) {
            log.debug("Could not parse date '{}' as DD-MM-YYYY: {}", clean, e.getMessage());
        }

        log.warn("Could not parse date '{}'", dateStr);

        return null;
    }

    private byte[] convertPdfToPng(byte[] pdfBytes)
            throws Exception {

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageIO.write(bufferedImage, "png", baos);

            return baos.toByteArray();
        }
    }

    private String callGeminiVision(String base64Image, String mediaType) throws Exception {

        String prompt = """
                You are analyzing a bank transfer slip or payment receipt.

                Extract these three values:

                1. amount
                    - Extract the actual transfer/payment amount.
                    - Return only a number.
                    - Remove LKR, Rs, commas and other currency symbols.
                    - Do not use fee or service charge amounts.

                2. bankPaymentDate
                    - Extract the actual date on which the bank transfer/payment
                      was made.
                    - Return it in YYYY-MM-DD format.
                    - Do not use the date the slip was generated if a different
                      transaction date is shown.
                    - If the date cannot be confidently identified, return null.

                3. bankReference
                    - Extract the identifier that most likely identifies this specific
                    bank payment, deposit, or transaction.
                    - The identifier may be explicitly labeled OR completely unlabeled.
                    - Do NOT require the presence of a "Reference Number" label.

                    If a reference/transaction identifier is explicitly labeled:
                    - Prefer values labeled:
                        "Reference Number", "Reference No", "Reference",
                        "Transaction ID", "Transaction Number", "Transaction Reference",
                        "Transfer ID", "Confirmation Number", "Trace Number",
                        "Journal Number", "Receipt Number", "Slip Number",
                    or similar terms.
                    - Return the complete value exactly as shown.

                    If there is NO reference label:
                    - Inspect the ENTIRE slip carefully, including:
                        * machine-printed transaction details
                        * transaction summary sections
                        * receipt/confirmation sections
                        * bottom sections containing printed transaction information
                        * handwritten and printed areas
                    - Look for a number or alphanumeric code that appears to uniquely
                    identify the specific payment or transaction.
                    - An unlabeled transaction identifier may appear next to or near
                    transaction details such as:
                    "CASH", "CREDIT", "DEBIT", "LKR", transaction date, transaction
                    amount, or other transaction information.
                    - Pay special attention to machine-printed numbers that are located
                    in the transaction/receipt section and are separate from the
                    customer's account number.
                    - Use the position, formatting, length, surrounding text, and
                    relationship to the transaction details to determine which value
                    is most likely the transaction identifier.

                IMPORTANT:
                - Do NOT return null merely because the identifier has no label.
                - If a strong unlabeled transaction identifier is visible, return it.
                - For example, if the transaction section contains a machine-printed
                  identifier such as "1621554 33 1101" and it is clearly separate
                  from the account number, consider it the bankReference.
                - Preserve spaces and characters exactly as they appear in the slip.

                Do NOT use:
                - Customer account numbers
                - Account holder numbers
                - Branch codes
                - Telephone/mobile numbers
                - Card numbers
                - Amounts
                - Dates
                - Currency values
                - Other values that clearly identify the customer or account rather
                  than the specific transaction.

                If multiple possible identifiers exist:
                - First prefer an explicitly labeled reference/transaction identifier.
                - Otherwise prefer the identifier located in the machine-printed
                  transaction/receipt section that most strongly appears to identify
                  the specific transaction.
                - Do not select an account number simply because it is a long number.

                Only return null when there is no reasonable transaction/reference
                identifier anywhere on the slip.

                Important:
                    - Do not guess values.
                    - If a value is unclear, return null.
                    - Carefully inspect the entire image.
                    - Return ONLY valid JSON.

                Required JSON format:

                    {
                        "amount": 0,
                        "bankPaymentDate": "YYYY-MM-DD",
                        "bankReference": "string"
                    }

                Use null when a value cannot be identified.
                        """;

        Map<String, Object> requestPayload = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                prompt),
                                        Map.of(
                                                "inline_data",
                                                Map.of(
                                                        "mime_type",
                                                        mediaType,
                                                        "data",
                                                        base64Image))))));

        String uri = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        log.info("Calling Gemini API with model: {}", geminiModel);

        String responseBody = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        log.info("Gemini API raw response body: {}", responseBody);

        JsonNode responseJson = objectMapper.readTree(responseBody);

        return responseJson
                .get("candidates")
                .get(0)
                .get("content")
                .get("parts")
                .get(0)
                .get("text")
                .asText()
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}