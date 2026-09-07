package com.library.model.vo;

import java.time.LocalDateTime;

public record CategoryVO(Integer id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
