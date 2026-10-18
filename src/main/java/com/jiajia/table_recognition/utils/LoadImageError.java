package com.jiajia.table_recognition.utils;

/**
 * 自定义异常类，用于在加载图片或处理图片时抛出错误
 */
public class LoadImageError extends Exception {
    public LoadImageError(String message) {
        super(message);
    }

    public LoadImageError(String message, Throwable cause) {
        super(message, cause);
    }
}