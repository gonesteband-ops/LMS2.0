package com.lms.dto;

import java.util.List;

/**
 * DTO genérico para respuestas paginadas.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
