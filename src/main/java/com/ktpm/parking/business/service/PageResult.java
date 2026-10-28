package com.ktpm.parking.business.service;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = List.copyOf(items);
        if (page < 0 || size < 1 || totalElements < 0) {
            throw new IllegalArgumentException("Invalid page metadata");
        }
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }
}
