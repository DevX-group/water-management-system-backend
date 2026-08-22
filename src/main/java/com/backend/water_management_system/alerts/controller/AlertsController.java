package com.backend.water_management_system.alerts.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.backend.water_management_system.alerts.entity.Alert;
import com.backend.water_management_system.alerts.repository.AlertRepository;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*") 
public class AlertsController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private com.backend.water_management_system.customer.repository.CustomerRepository customerRepository;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public Alert createAlert(@RequestBody Alert alert) {
        return alertRepository.save(alert);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('CUSTOMER')")
    public List<Alert> getAlerts(@RequestParam(required = false) String severity, org.springframework.security.core.Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_CUSTOMER".equals(role)) {
            String nic = authentication.getName();
            com.backend.water_management_system.customer.entity.Customer customer = customerRepository.findByUser_Nic(nic).orElseThrow(() -> new RuntimeException("Customer not found"));
            return alertService.getActiveAlertsForCustomer(severity, customer.getSubscriptionNumber());
        }
        return alertService.getActiveAlerts(severity);
    }

    @Autowired
    private com.backend.water_management_system.alerts.service.AlertService alertService;

    @GetMapping("/counts")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('CUSTOMER')")
    public Map<String, Long> getCounts(org.springframework.security.core.Authentication authentication) {      //severity name:count
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if ("ROLE_CUSTOMER".equals(role)) {
            String nic = authentication.getName();
            com.backend.water_management_system.customer.entity.Customer customer = customerRepository.findByUser_Nic(nic).orElseThrow(() -> new RuntimeException("Customer not found"));
            return alertService.getSeverityCountsForCustomer(customer.getSubscriptionNumber());
        }
        return alertService.getSeverityCounts();
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public void dismiss(@PathVariable Long id) {
        alertService.dismissAlert(id);
    }
}