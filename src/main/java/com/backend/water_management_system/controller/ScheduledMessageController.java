package com.backend.water_management_system.controller;

import com.backend.water_management_system.dto.ScheduledMessageDto;
import com.backend.water_management_system.dto.SentMessageFailureDto;
import com.backend.water_management_system.dto.SentMessageHistoryDto;
import com.backend.water_management_system.service.MessagePlaceholder;
import com.backend.water_management_system.service.ScheduledMessageService;
import com.backend.water_management_system.service.SentMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
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
    public ResponseEntity<List<SentMessageHistoryDto>> getHistory() {
        return ResponseEntity.ok(sentMessageService.getHistory());
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
    ResponseEntity<List<SentMessageFailureDto>> getFailures(@PathVariable Long sentMessageId) {
        return ResponseEntity.ok(sentMessageService.getFailures(sentMessageId));
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
