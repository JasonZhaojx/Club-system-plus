package com.backend.sever.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应结构")
public record Result<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0")
        int code,
        @Schema(description = "响应消息", example = "success")
        String message,
        @Schema(description = "响应数据")
        T data
) {
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
