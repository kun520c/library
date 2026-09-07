package com.library.model.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentDTO(
        @NotNull(message = "库存调整量不能为空") Integer delta) {
}
