package com.backend.water_management_system.inquiry.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.water_management_system.inquiry.entity.Inquiry;
import com.backend.water_management_system.inquiry.entity.InquiryMessage;
import com.backend.water_management_system.inquiry.repository.InquiryRepository;

@ExtendWith(MockitoExtension.class)
public class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryService inquiryService;

    private Inquiry mockInquiry;
    private InquiryMessage mockMessage;

    @BeforeEach
    void setUp() {
        mockInquiry = new Inquiry();
        mockInquiry.setId("INQ123");
        mockInquiry.setStatus("open");
        mockInquiry.setMessages(new ArrayList<>());

        mockMessage = new InquiryMessage();
        mockMessage.setText("Need help with bill.");
    }

    @Test
    void testCreateInquiry() {
        // Arrange
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(mockInquiry);

        // Act
        Inquiry created = inquiryService.createInquiry(mockInquiry);

        // Assert
        assertNotNull(created);
        assertEquals("INQ123", created.getId());
        verify(inquiryRepository, times(1)).save(mockInquiry);
    }

    @Test
    void testGetAllInquiries() {
        // Arrange
        when(inquiryRepository.findAll()).thenReturn(List.of(mockInquiry));

        // Act
        List<Inquiry> list = inquiryService.getAllInquiries();

        // Assert
        assertEquals(1, list.size());
        verify(inquiryRepository, times(1)).findAll();
    }

    @Test
    void testAddMessage_Success() {
        // Arrange
        when(inquiryRepository.findById("INQ123")).thenReturn(Optional.of(mockInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(mockInquiry);

        // Act
        Inquiry updated = inquiryService.addMessage("INQ123", mockMessage);

        // Assert
        assertEquals(1, updated.getMessages().size());
        assertEquals("Need help with bill.", updated.getMessages().get(0).getText());
        verify(inquiryRepository, times(1)).findById("INQ123");
        verify(inquiryRepository, times(1)).save(mockInquiry);
    }

    @Test
    void testAddMessage_NotFound() {
        // Arrange
        when(inquiryRepository.findById("INQ999")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            inquiryService.addMessage("INQ999", mockMessage);
        });

        assertEquals("Inquiry not found with id: INQ999", exception.getMessage());
        verify(inquiryRepository, never()).save(any(Inquiry.class));
    }

    @Test
    void testUpdateStatus() {
        // Arrange
        when(inquiryRepository.findById("INQ123")).thenReturn(Optional.of(mockInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(mockInquiry);

        // Act
        Inquiry updated = inquiryService.updateStatus("INQ123", "resolved");

        // Assert
        assertEquals("resolved", updated.getStatus());
        verify(inquiryRepository, times(1)).save(mockInquiry);
    }
}
