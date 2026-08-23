package com.backend.water_management_system.inquiry.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.water_management_system.inquiry.entity.Inquiry;
import com.backend.water_management_system.inquiry.entity.InquiryMessage;
import com.backend.water_management_system.inquiry.service.InquiryService;

@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin(origins = "*") 
public class InquiryController {

    @Autowired
    private InquiryService inquiryService; // Use the service instead of repository

    @Autowired
    private com.backend.water_management_system.payments.service.CloudinaryService cloudinaryService;

    @PostMapping     // Create a new inquiry
    @PreAuthorize("hasRole('CUSTOMER')")
    public Inquiry createInquiry(@RequestBody Inquiry inquiry) {
        return inquiryService.createInquiry(inquiry);
    }

    @GetMapping   // Get inquiries (admin gets all, customer gets their own)
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CUSTOMER_HANDLER') or hasRole('CUSTOMER')")
    public List<Inquiry> getAllInquiries(org.springframework.security.core.Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_CUSTOMER".equals(role)) {
            String nic = authentication.getName();
            return inquiryService.getInquiriesForCustomer(nic);
        }
        return inquiryService.getAllInquiries();
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CUSTOMER_HANDLER') or hasRole('CUSTOMER')")
    public org.springframework.data.domain.Page<Inquiry> getInquiriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            org.springframework.security.core.Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        if ("ROLE_CUSTOMER".equals(role)) {
            String nic = authentication.getName();
            return inquiryService.getInquiriesForCustomerPaginated(nic, pageable);
        }
        return inquiryService.getAllInquiriesPaginated(pageable);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPER_ADMIN') or hasRole('CUSTOMER_HANDLER')")
    public Inquiry addMessage(@PathVariable String id, @RequestBody InquiryMessage message) {
        return inquiryService.addMessage(id, message);
    }

    @PostMapping(value = "/upload-attachment", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CUSTOMER_HANDLER') or hasRole('CUSTOMER')")
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> uploadAttachment(
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (cloudinaryService.isConfigured()) {
            com.backend.water_management_system.payments.dto.CloudinaryUploadResponse res = cloudinaryService.uploadFile(file);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("url", res.getUrl()));
        } else {
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of("error", "Cloudinary not configured"));
        }
    }

    @PatchMapping("/{id}/status")     // Update the status of an inquiry   
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CUSTOMER_HANDLER')")
    public Inquiry updateStatus(@PathVariable String id, @RequestParam String status) {
        return inquiryService.updateStatus(id, status);
    }
}
