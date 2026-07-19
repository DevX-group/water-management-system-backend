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
import com.backend.water_management_system.alerts.service.AlertService;
import com.backend.water_management_system.customer.service.CustomerAccessService;
import com.backend.water_management_system.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*") 
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('SYSTEM_ADMIN') or hasRole('CUSTOMER')")
public class AlertsController {

    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private CustomerAccessService customerAccessService;

    @PostMapping
public Alert createAlert(@RequestBody Alert alert) {
    return alertRepository.save(alert);
}

    @GetMapping
    public List<Alert> getAlerts(@RequestParam(required = false) String severity, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getUser().getRole() == com.backend.water_management_system.user.enums.Role.CUSTOMER) {
            String sub = customerAccessService.getSubscriptionNumber(principal);
            return alertService.getActiveAlertsForCustomer(sub, severity);
        }
        if (severity != null && !severity.equalsIgnoreCase("all")) {
            return alertRepository.findBySeverityAndDismissedFalse    //alert is still active
            (severity.toLowerCase());
        }
        return alertRepository.findByDismissedFalseOrderByTimeDesc();
    }

    @GetMapping("/counts")
    public Map<String, Long> getCounts(@AuthenticationPrincipal UserPrincipal principal) {      //severity name:count
        if (principal != null && principal.getUser().getRole() == com.backend.water_management_system.user.enums.Role.CUSTOMER) {
            String sub = customerAccessService.getSubscriptionNumber(principal);
            return alertService.getSeverityCountsForCustomer(sub);
        }
        List<Alert> active = alertRepository.findByDismissedFalseOrderByTimeDesc();
        return active.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));
    }

    @PatchMapping("/{id}/dismiss")
    public void dismiss(@PathVariable Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setDismissed(true);
            alertRepository.save(alert);
        });
    }
}