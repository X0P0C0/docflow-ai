package com.docflow.ai.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;

    public static <T> PageResponse<T> of(List<T> records, long total, long page, long size) {
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .page(page)
                .size(size)
                .pages(size > 0 ? (total + size - 1) / size : 0)
                .build();
    }

    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

}
