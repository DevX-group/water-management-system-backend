package com.backend.water_management_system.predictions.service;

import com.backend.water_management_system.predictions.dto.CustomerPredictionResponse;
import com.backend.water_management_system.predictions.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.reports.repository.UsageRecordRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class PredictionService {

    private final UsageRecordRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PredictionService(UsageRecordRepository repository) {
        this.repository = repository;
    }

    public List<MonthlyPredictionResponse> getMonthlyPrediction(int year) {

        List<MonthlyPredictionResponse> finalData = new ArrayList<>();

        // =========================
        // ACTUAL DATA FROM DATABASE
        // =========================

        List<Object[]> results = repository.getMonthlyReport(year);

        List<Map<String, Object>> flaskData = new ArrayList<>();

        int monthNumber = 1;

        for (Object[] row : results) {

            String month = row[0].toString();

            Double usage = ((Number) row[1]).doubleValue();

            // Add actual data to frontend response
            finalData.add(
                    new MonthlyPredictionResponse(
                            month,
                            usage,
                            null
                    )
            );

            // Prepare data for Flask
            Map<String, Object> item = new HashMap<>();

            String date = year + "-" +
                    String.format("%02d", monthNumber) +
                    "-01";

            item.put("date", date);

            item.put("value", usage);

            flaskData.add(item);

            monthNumber++;
        }

        // =========================
        // CALL FLASK API
        // =========================

        String flaskUrl = "http://127.0.0.1:5000/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Map<String, Object>>> request =
                new HttpEntity<>(flaskData, headers);

        String predictionResponse = restTemplate.postForObject(
                flaskUrl,
                request,
                String.class
        );

        // =========================
        // PROCESS PREDICTIONS
        // =========================

        int lastActualIndex = results.size();
        JSONArray predictions = new JSONArray(predictionResponse);

        for (int i = 0; i < predictions.length(); i++) {

            JSONObject obj = predictions.getJSONObject(i);

            Double predictedUsage = obj.getDouble("yhat");

            // generate next months sequentially
            LocalDate date = LocalDate.of(year, 1, 1)
                    .plusMonths(lastActualIndex + i);

            String monthName = date.getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            finalData.add(
                    new MonthlyPredictionResponse(
                            monthName,
                            null,
                            predictedUsage
                    )
            );
        }

        return finalData;
    }

    public List<CustomerPredictionResponse> getCustomerPrediction(
            String customerId,
            int year
    ) {

        List<CustomerPredictionResponse> finalData = new ArrayList<>();

        List<Object[]> results =
                repository.getCustomerPredictionData(customerId, year);

        List<Map<String, Object>> flaskData = new ArrayList<>();

        int monthNumber = 1;

        // ================= ACTUAL DATA =================

        for (Object[] row : results) {

            String month = row[0].toString();

            Double usage = ((Number) row[1]).doubleValue();

            finalData.add(
                    new CustomerPredictionResponse(
                            month,
                            usage,
                            null
                    )
            );

            Map<String, Object> item = new HashMap<>();

            String date = year + "-" +
                    String.format("%02d", monthNumber) +
                    "-01";

            item.put("date", date);

            item.put("value", usage);

            flaskData.add(item);

            monthNumber++;
        }

        // ================= CALL FLASK =================

        String flaskUrl = "http://127.0.0.1:5000/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Map<String, Object>>> request =
                new HttpEntity<>(flaskData, headers);

        String predictionResponse = restTemplate.postForObject(
                flaskUrl,
                request,
                String.class
        );

        JSONArray predictions = new JSONArray(predictionResponse);

        int lastActualIndex = results.size();

        // ================= PREDICTIONS =================

        for (int i = 0; i < predictions.length(); i++) {

            JSONObject obj = predictions.getJSONObject(i);

            Double predictedUsage = obj.getDouble("yhat");

            LocalDate date = LocalDate.of(year, 1, 1)
                    .plusMonths(lastActualIndex + i);

            String monthName = date.getMonth()
                    .getDisplayName(
                            TextStyle.SHORT,
                            Locale.ENGLISH
                    );

            finalData.add(
                    new CustomerPredictionResponse(
                            monthName,
                            null,
                            predictedUsage
                    )
            );
        }

        return finalData;
    }
}
