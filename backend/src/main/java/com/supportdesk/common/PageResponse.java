package com.supportdesk.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** A stable JSON shape for paginated lists (Spring's own Page type is not meant to be serialised directly). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
