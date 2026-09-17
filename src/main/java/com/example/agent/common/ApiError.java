package com.example.agent.common;

/**
 * 统一错误响应体。
 *
 * @param error  给调用方看的简短分类
 * @param detail 具体原因（不含堆栈）
 */
public record ApiError(String error, String detail) {
}
