package com.jiajia.common_object;

/**
 * 取消令牌，用于请求取消长时间运行的 OCR 操作。
 * <p>
 * 用户可以通过调用 {@link #cancel()} 方法来请求取消，
 * 核心功能类在循环或耗时操作处检查 {@link #isCancelled()} 状态，
 * 如果检测到取消请求，则抛出 {@link CancellationException}。
 * </p>
 */
public class CancellationToken {

    private volatile boolean cancelled = false;

    /**
     * 请求取消操作。
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 检查是否已请求取消。
     *
     * @return 如果已请求取消则返回 true，否则返回 false
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * 检查是否已请求取消，如果是则抛出 CancellationException。
     */
    public void throwIfCancelled() {
        if (this.cancelled) {
            throw new CancellationException("Operation was cancelled by user.");
        }
    }
}
