package com.homeschool.platform.common.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class PageQuery {

    private long page = 1;
    private long size = 10;

    public long normalizedPage() {
        return Math.max(1, page);
    }

    public long normalizedSize() {
        return Math.min(100, Math.max(1, size));
    }
}

