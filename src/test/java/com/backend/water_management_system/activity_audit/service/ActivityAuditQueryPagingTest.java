package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.entity.ActivityAuditLog;
import com.backend.water_management_system.activity_audit.mapper.ActivityAuditMapper;
import com.backend.water_management_system.activity_audit.repository.ActivityAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAuditQueryPagingTest {

    @Mock
    private ActivityAuditLogRepository repository;
    @Mock
    private ActivityAuditMapper mapper;

    @SuppressWarnings("unchecked")
    @Test
    void defaultSortIncludesStableDescendingIdTieBreaker() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<ActivityAuditLog>(List.of(), pageable, 0);
                });
        ActivityAuditQueryService service = new ActivityAuditQueryService(repository, mapper);

        service.findAll(0, 20, "occurredAt", "desc",
                null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("occurredAt").isDescending()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }
}
