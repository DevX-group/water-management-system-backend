package com.backend.water_management_system.service;

import com.backend.water_management_system.dto.AreaReportDTO;
import com.backend.water_management_system.repository.UsageRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaReportServiceTest {

    @Mock
    private UsageRecordRepository repository;

    private AreaReportService service;

    @BeforeEach
    void setUp() {
        service = new AreaReportService(repository);
    }

    @Test
    void getAreaReport_withData_mapsCorrectly() {
        // Arrange
        int year = 2026;
        List<Object[]> mockRows = new ArrayList<>();
        // Row 1: Month "Jan", MonthNum 1, Area "area1", Usage 100.0, Amount 500.0
        mockRows.add(new Object[]{"Jan", 1, "area1", 100.0, 500.0});
        // Row 2: Month "Jan", MonthNum 1, Area "area2", Usage 150.0, Amount 750.0
        mockRows.add(new Object[]{"Jan", 1, "area2", 150.0, 750.0});
        // Row 3: Month "Jan", MonthNum 1, Area "area3", Usage 200.0, Amount 1000.0
        mockRows.add(new Object[]{"Jan", 1, "area3", 200.0, 1000.0});
        // Row 4: Month "Feb", MonthNum 2, Area "area1", Usage 120.0, Amount 600.0
        mockRows.add(new Object[]{"Feb", 2, "area1", 120.0, 600.0});

        when(repository.getAreaReport(year)).thenReturn(mockRows);

        // Act
        List<AreaReportDTO> report = service.getAreaReport(year);

        // Assert
        assertNotNull(report);
        assertEquals(2, report.size());

        // Verify January values
        AreaReportDTO janReport = report.stream()
                .filter(r -> "Jan".equals(r.getMonth()))
                .findFirst()
                .orElseThrow();
        assertEquals(100.0, janReport.getArea1Usage());
        assertEquals(500.0, janReport.getArea1Revenue());
        assertEquals(150.0, janReport.getArea2Usage());
        assertEquals(750.0, janReport.getArea2Revenue());
        assertEquals(200.0, janReport.getArea3Usage());
        assertEquals(1000.0, janReport.getArea3Revenue());

        // Verify February values
        AreaReportDTO febReport = report.stream()
                .filter(r -> "Feb".equals(r.getMonth()))
                .findFirst()
                .orElseThrow();
        assertEquals(120.0, febReport.getArea1Usage());
        assertEquals(600.0, febReport.getArea1Revenue());
        assertEquals(0.0, febReport.getArea2Usage()); // Default values
        assertEquals(0.0, febReport.getArea2Revenue());

        verify(repository, times(1)).getAreaReport(year);
    }

    @Test
    void getAreaReport_emptyData_returnsEmptyList() {
        // Arrange
        int year = 2026;
        when(repository.getAreaReport(year)).thenReturn(Collections.emptyList());

        // Act
        List<AreaReportDTO> report = service.getAreaReport(year);

        // Assert
        assertNotNull(report);
        assertTrue(report.isEmpty());
        verify(repository, times(1)).getAreaReport(year);
    }
}
