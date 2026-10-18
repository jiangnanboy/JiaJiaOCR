package com.jiajia.common_object;

import java.util.Objects;

/**
 * 简单的不可变三元组 (Triple)。
 * 
 * @param <L> left 元素类型
 * @param <M> middle 元素类型
 * @param <R> right 元素类型
 */
public final class Triple<L, M, R> {
    private final L left;
    private final M middle;
    private final R right;

    private Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    /**
     * 静态工厂方法。
     */
    public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
        return new Triple<>(left, middle, right);
    }

    public L getLeft() {
        return left;
    }

    public M getMiddle() {
        return middle;
    }

    public R getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triple)) return false;
        Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
        return Objects.equals(left, other.left)
                && Objects.equals(middle, other.middle)
                && Objects.equals(right, other.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, middle, right);
    }

    @Override
    public String toString() {
        return "Triple{" +
                "left=" + left +
                ", middle=" + middle +
                ", right=" + right +
                '}';
    }
}
