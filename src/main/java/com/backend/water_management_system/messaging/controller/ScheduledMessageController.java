package com.backend.water_management_system.messaging.controller;

import com.backend.water_management_system.messaging.dto.ScheduledMessageDto;
import com.backend.water_management_system.messaging.dto.SentMessageFailureDto;
import com.backend.water_management_system.messaging.dto.SentMessageHistoryDto;
import com.backend.water_management_system.messaging.enums.MessagePlaceholder;
import com.backend.water_management_system.messaging.service.ScheduledMessageService;
import com.backend.water_management_system.messaging.service.SentMessageService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduled-messages")
@CrossOrigin
@RequiredArgsConstructor
public class ScheduledMessageController {

    private final ScheduledMessageService service;
    private final SentMessageService sentMessageService;

    @GetMapping
    public ResponseEntity<List<ScheduledMessageDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/history")
    public ResponseEntity<Page<SentMessageHistoryDto>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sentMessageService.getHistory(page, size));
    }

    @GetMapping("/placeholders")
    public ResponseEntity<List<String>> getPlaceholders() {
        return ResponseEntity.ok(MessagePlaceholder.keys());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledMessageDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/failures/{sentMessageId}")
    ResponseEntity<Page<SentMessageFailureDto>> getFailures(
            @PathVariable Long sentMessageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(sentMessageService.getFailures(sentMessageId, page, size));
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
