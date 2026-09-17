package com.kartify.api.shared.dto;

import java.util.List;

public record PaginationResponse<T>(
    List<T> payload,
    int currentPage,
    int pageSize,
    long totalItems,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}
