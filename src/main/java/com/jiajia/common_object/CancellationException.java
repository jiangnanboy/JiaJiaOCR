package com.jiajia.common_object;

/**
 * 取消异常，当 OCR 操作被用户通过 {@link CancellationToken} 请求取消时抛出。
 * <p>
 * 这是一个运行时异常，用于中断长时间运行的 OCR 流程。
 * </p>
 */
public class CancellationException extends RuntimeException {

    public CancellationException() {
        super();
    }

    public CancellationException(String message) {
        super(message);
    }

    public CancellationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancellationException(Throwable cause) {
        super(cause);
    }
}
