package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.ScheduledMessageDto;
import com.backend.water_management_system.service.ScheduledMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin
public class ScheduledMessageController {

    private final ScheduledMessageService service;

    public ScheduledMessageController(ScheduledMessageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ScheduledMessageDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledMessageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ScheduledMessageDto> create(@RequestBody ScheduledMessageDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledMessageDto> update(@PathVariable Long id, @RequestBody ScheduledMessageDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
