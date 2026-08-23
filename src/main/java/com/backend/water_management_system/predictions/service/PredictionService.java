package com.backend.water_management_system.predictions.service;

import com.backend.water_management_system.predictions.dto.AreaPredictionResponse;
import com.backend.water_management_system.predictions.dto.CustomerPredictionResponse;
import com.backend.water_management_system.predictions.dto.MonthlyPredictionResponse;
import com.backend.water_management_system.reports.repository.UsageRecordRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

@Service
public class PredictionService {

    @Value("${prediction.service.url:https://m5zktklv-5000.asse.devtunnels.ms/predict}")
private String flaskUrl;

    private final UsageRecordRepository repository;
    private final RestTemplate restTemplate;

    public PredictionService(
            UsageRecordRepository repository
    ) {
        this.repository = repository;
        this.restTemplate = new RestTemplate();
    }

    // =====================================================
    // MONTHLY PREDICTION
    // =====================================================

    public List<MonthlyPredictionResponse> getMonthlyPrediction(
            int year
    ) {
        List<Object[]> results =
                repository.getMonthlyPredictionData(year);

        List<MonthlyPredictionResponse> finalData =
                new ArrayList<>();

        List<PredictionPoint> usagePoints =
                new ArrayList<>();

        List<PredictionPoint> revenuePoints =
                new ArrayList<>();

        YearMonth lastActualMonth = null;

        for (Object[] row : results) {
            String month = row[0].toString();
            int monthNumber =
                    ((Number) row[1]).intValue();

            double usage =
                    ((Number) row[2]).doubleValue();

            double revenue =
                    ((Number) row[3]).doubleValue();

            YearMonth recordMonth =
                    YearMonth.of(year, monthNumber);

            lastActualMonth = recordMonth;

            finalData.add(
                    new MonthlyPredictionResponse(
                            month,
                            usage,
                            revenue,
                            null,
                            null
                    )
            );

            usagePoints.add(
                    new PredictionPoint(
                            recordMonth.atDay(1),
                            usage
                    )
            );

            revenuePoints.add(
                    new PredictionPoint(
                            recordMonth.atDay(1),
                            revenue
                    )
            );
        }

        if (lastActualMonth == null) {
            return finalData;
        }

        List<Double> predictedUsage =
                requestPredictions(usagePoints);

        List<Double> predictedRevenue =
                requestPredictions(revenuePoints);

        int predictionCount = Math.min(
                predictedUsage.size(),
                predictedRevenue.size()
        );

        for (int index = 0;
             index < predictionCount;
             index++) {

            YearMonth predictedMonth =
                    lastActualMonth.plusMonths(index + 1);

            finalData.add(
                    new MonthlyPredictionResponse(
                            getMonthName(predictedMonth),
                            null,
                            null,
                            predictedUsage.get(index),
                            predictedRevenue.get(index)
                    )
            );
        }

        return finalData;
    }

    // =====================================================
    // CUSTOMER PREDICTION
    // =====================================================

    public List<CustomerPredictionResponse> getCustomerPrediction(
            String customerId,
            int year
    ) {
        String normalizedCustomerId =
                customerId == null
                        ? ""
                        : customerId.trim().toUpperCase();

        List<Object[]> results =
                repository.getCustomerPredictionData(
                        normalizedCustomerId,
                        year
                );

        List<CustomerPredictionResponse> finalData =
                new ArrayList<>();

        List<PredictionPoint> usagePoints =
                new ArrayList<>();

        List<PredictionPoint> revenuePoints =
                new ArrayList<>();

        YearMonth lastActualMonth = null;

        for (Object[] row : results) {
            String month = row[0].toString();
            int monthNumber =
                    ((Number) row[1]).intValue();

            double usage =
                    ((Number) row[2]).doubleValue();

            double revenue =
                    ((Number) row[3]).doubleValue();

            YearMonth recordMonth =
                    YearMonth.of(year, monthNumber);

            lastActualMonth = recordMonth;

            finalData.add(
                    new CustomerPredictionResponse(
                            month,
                            usage,
                            revenue,
                            null,
                            null
                    )
            );

            usagePoints.add(
                    new PredictionPoint(
                            recordMonth.atDay(1),
                            usage
                    )
            );

            revenuePoints.add(
                    new PredictionPoint(
                            recordMonth.atDay(1),
                            revenue
                    )
            );
        }

        if (lastActualMonth == null) {
            return finalData;
        }

        List<Double> predictedUsage =
                requestPredictions(usagePoints);

        List<Double> predictedRevenue =
                requestPredictions(revenuePoints);

        int predictionCount = Math.min(
                predictedUsage.size(),
                predictedRevenue.size()
        );

        for (int index = 0;
             index < predictionCount;
             index++) {

            YearMonth predictedMonth =
                    lastActualMonth.plusMonths(index + 1);

            finalData.add(
                    new CustomerPredictionResponse(
                            getMonthName(predictedMonth),
                            null,
                            null,
                            predictedUsage.get(index),
                            predictedRevenue.get(index)
                    )
            );
        }

        return finalData;
    }

    // =====================================================
    // AREA PREDICTION
    // =====================================================

    public List<AreaPredictionResponse> getAreaPrediction(
            String area,
            int year
    ) {
        String selectedArea =
                area == null || area.isBlank()
                        ? "all"
                        : area.trim().toLowerCase();

        List<Object[]> results =
                repository.getAreaPredictionData(
                        year,
                        selectedArea
                );

        /*
         * Keeps one DTO for each month. This preserves the
         * wide object structure required by the area chart.
         */
        Map<YearMonth, AreaPredictionResponse> monthlyData =
                new LinkedHashMap<>();

        Map<String, List<PredictionPoint>> usageByArea =
                new LinkedHashMap<>();

        Map<String, List<PredictionPoint>> revenueByArea =
                new LinkedHashMap<>();

        Map<String, YearMonth> lastMonthByArea =
                new HashMap<>();

        for (Object[] row : results) {
            String month = row[0].toString();
            int monthNumber =
                    ((Number) row[1]).intValue();

            String recordArea =
                    row[2].toString().toLowerCase();

            double usage =
                    ((Number) row[3]).doubleValue();

            double revenue =
                    ((Number) row[4]).doubleValue();

            YearMonth recordMonth =
                    YearMonth.of(year, monthNumber);

            AreaPredictionResponse response =
                    monthlyData.computeIfAbsent(
                            recordMonth,
                            ignored ->
                                    new AreaPredictionResponse(month)
                    );

            setActualAreaValues(
                    response,
                    recordArea,
                    usage,
                    revenue
            );

            usageByArea
                    .computeIfAbsent(
                            recordArea,
                            ignored -> new ArrayList<>()
                    )
                    .add(
                            new PredictionPoint(
                                    recordMonth.atDay(1),
                                    usage
                            )
                    );

            revenueByArea
                    .computeIfAbsent(
                            recordArea,
                            ignored -> new ArrayList<>()
                    )
                    .add(
                            new PredictionPoint(
                                    recordMonth.atDay(1),
                                    revenue
                            )
                    );

            lastMonthByArea.put(
                    recordArea,
                    recordMonth
            );
        }

        /*
         * Generate predictions separately for every area
         * returned by the database.
         */
        for (String recordArea : usageByArea.keySet()) {
            List<Double> predictedUsage =
                    requestPredictions(
                            usageByArea.get(recordArea)
                    );

            List<Double> predictedRevenue =
                    requestPredictions(
                            revenueByArea.get(recordArea)
                    );

            int predictionCount = Math.min(
                    predictedUsage.size(),
                    predictedRevenue.size()
            );

            YearMonth lastActualMonth =
                    lastMonthByArea.get(recordArea);

            for (int index = 0;
                 index < predictionCount;
                 index++) {

                YearMonth predictedMonth =
                        lastActualMonth.plusMonths(index + 1);

                AreaPredictionResponse response =
                        monthlyData.computeIfAbsent(
                                predictedMonth,
                                ignored ->
                                        new AreaPredictionResponse(
                                                getMonthName(
                                                        predictedMonth
                                                )
                                        )
                        );

                setPredictedAreaValues(
                        response,
                        recordArea,
                        predictedUsage.get(index),
                        predictedRevenue.get(index)
                );
            }
        }

        return new ArrayList<>(monthlyData.values());
    }

    // =====================================================
    // FLASK COMMUNICATION
    // =====================================================

    private List<Double> requestPredictions(
            List<PredictionPoint> points
    ) {
        /*
         * Prophet needs at least two valid observations.
         * Return no predicted rows when there is not enough
         * historical data.
         */
        if (points == null || points.size() < 2) {
            return List.of();
        }

        List<Map<String, Object>> flaskData =
                new ArrayList<>();

        for (PredictionPoint point : points) {
            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "date",
                    point.date().toString()
            );

            item.put(
                    "value",
                    point.value()
            );

            flaskData.add(item);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<List<Map<String, Object>>> request =
                new HttpEntity<>(
                        flaskData,
                        headers
                );

        String responseBody =
                restTemplate.postForObject(
                        flaskUrl,
                        request,
                        String.class
                );

        if (responseBody == null ||
                responseBody.isBlank()) {
            return List.of();
        }

        JSONArray predictionArray =
                new JSONArray(responseBody);

        List<Double> predictions =
                new ArrayList<>();

        for (int index = 0;
             index < predictionArray.length();
             index++) {

            JSONObject prediction =
                    predictionArray.getJSONObject(index);

            predictions.add(
                    prediction.getDouble("yhat")
            );
        }

        return predictions;
    }

    // =====================================================
    // AREA VALUE MAPPING
    // =====================================================

    private void setActualAreaValues(
            AreaPredictionResponse response,
            String area,
            double usage,
            double revenue
    ) {
        switch (area) {
            case "area1" -> {
                response.setArea1Usage(usage);
                response.setArea1Revenue(revenue);
            }

            case "area2" -> {
                response.setArea2Usage(usage);
                response.setArea2Revenue(revenue);
            }

            case "area3" -> {
                response.setArea3Usage(usage);
                response.setArea3Revenue(revenue);
            }

            default -> {
                // Unknown area values are ignored.
            }
        }
    }

    private void setPredictedAreaValues(
            AreaPredictionResponse response,
            String area,
            double predictedUsage,
            double predictedRevenue
    ) {
        switch (area) {
            case "area1" -> {
                response.setPredictedArea1Usage(
                        predictedUsage
                );
                response.setPredictedArea1Revenue(
                        predictedRevenue
                );
            }

            case "area2" -> {
                response.setPredictedArea2Usage(
                        predictedUsage
                );
                response.setPredictedArea2Revenue(
                        predictedRevenue
                );
            }

            case "area3" -> {
                response.setPredictedArea3Usage(
                        predictedUsage
                );
                response.setPredictedArea3Revenue(
                        predictedRevenue
                );
            }

            default -> {
                // Unknown area values are ignored.
            }
        }
    }

    private String getMonthName(
            YearMonth yearMonth
    ) {
        return yearMonth
                .getMonth()
                .getDisplayName(
                        TextStyle.SHORT,
                        Locale.ENGLISH
                );
    }

    private record PredictionPoint(
            LocalDate date,
            double value
    ) {
    }
}