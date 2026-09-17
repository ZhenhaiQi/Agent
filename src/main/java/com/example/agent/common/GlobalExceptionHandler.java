package com.example.agent.common;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把上层各类异常映射成稳定的 HTTP 状态码 + 统一 JSON 结构，
 * 堆栈只写服务端日志，不泄漏给调用方。
 *
 * <p>继承 {@link ResponseEntityExceptionHandler} 是为了保留 Spring MVC 内建异常
 * （405 方法不允许 / 415 媒体类型不支持 / 404 / 400 JSON 解析失败等）原本的状态码——
 * 否则一个兜底的 {@code @ExceptionHandler(Exception.class)} 会把它们全变成 500。
 *
 * <p>这也是「LLM 是不稳定的远程依赖」的第一课：上游会 401/429/5xx/超时，
 * 服务端必须把它们翻译成调用方能理解的语义。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 覆盖 Spring MVC 内建异常的响应体，保留其状态码，只把 body 换成统一的 ApiError */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return new ResponseEntity<>(new ApiError("请求错误", ex.getMessage()), headers, statusCode);
    }

    /** 请求体校验失败 → 400 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(new ApiError("参数校验失败", detail), headers, HttpStatus.BAD_REQUEST);
    }

    /** 上游鉴权失败，通常是自己这边 Key 没配好 → 502（不是调用方的错，故不用 401） */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> onUnauthorized(UnauthorizedException ex) {
        log.error("上游鉴权失败：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("上游模型服务鉴权失败", "请检查环境变量 DEEPSEEK_API_KEY 是否已正确设置"));
    }

    /** 上游限流 → 429 */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiError> onRateLimit(RateLimitException ex) {
        log.warn("上游限流：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError("上游模型服务限流", "请稍后重试或降低并发"));
    }

    /** 连接上游失败 / 超时 → 504 */
    @ExceptionHandler(OpenAIIoException.class)
    public ResponseEntity<ApiError> onUpstreamIo(OpenAIIoException ex) {
        log.error("连接上游失败：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ApiError("连接上游模型服务失败", "请检查网络或代理设置"));
    }

    /** 上游返回的其它错误 → 502 */
    @ExceptionHandler(OpenAIServiceException.class)
    public ResponseEntity<ApiError> onUpstream(OpenAIServiceException ex) {
        log.error("上游返回错误：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("上游模型服务返回错误", ex.getMessage()));
    }

    /** 兜底 → 500，只回异常类型与消息，不回堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onOther(Exception ex) {
        log.error("未预期异常", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiError("服务内部错误", ex.getClass().getSimpleName() + ": " + ex.getMessage()));
    }
}
