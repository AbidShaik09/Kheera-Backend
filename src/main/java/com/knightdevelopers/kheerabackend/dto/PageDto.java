package com.knightdevelopers.kheerabackend.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
