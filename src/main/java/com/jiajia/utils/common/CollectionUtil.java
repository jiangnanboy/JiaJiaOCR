package com.jiajia.utils.common;

import java.util.*;

/**
 * @author jiangnanboy
 * @date 2024/7/1 19:23
 */
public class CollectionUtil<E, T> {

    public static <E> List<E> newArrayList(List<E> list) {
        return new ArrayList<>(list);
    }

    public static <E> List<E> newArrayList() {
        return new ArrayList<>();
    }

    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<>();
    }

    public static <E> List<E> newArrayList(int N) {
        return new ArrayList<>(N);
    }

    public static <E> List<E> newArrayList(Set<E> entry) {
        return new ArrayList<>(entry);
    }

    public static <E> Set<E> newHashset() {
        return new HashSet<>();
    }

    public static <E> Set<E> newHashset(List<E> entry) {
        return new HashSet<>(entry);
    }

    public static <E,T> Map<E, T> newHashMap() {
        return new HashMap<>();
    }

    public static <E,T> LinkedHashMap<E, T> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    public static <E,T> Map<E, T> newTreeMap() {
        return new TreeMap<>();
    }

}
