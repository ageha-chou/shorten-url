package com.diepnn.shortenurl.utils;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class SpecUtils {
    private SpecUtils() {}

    public static <T> Specification<T> equalsTo(String attr, Object value) {
        if (value == null) return null;
        return (root, q, cb) -> cb.equal(root.get(attr), value);
    }

    public static <T> Specification<T> equalsIgnoreCase(String attr, String value) {
        if (value == null || value.isBlank()) return null;
        return (root, q, cb) -> cb.equal(
                cb.lower(root.get(attr)),
                value.toLowerCase()
        );
    }

    public static <T> Specification<T> containsIgnoreCase(String attr, String value) {
        if (value == null || value.isBlank()) return null;

        String escaped = value.replace("%", "\\%").replace("_", "\\_").toLowerCase();
        return (root, q, cb) -> cb.like(
                cb.lower(root.get(attr)),
                "%" + escaped + "%",
                '\\'
        );
    }

    public static <T, Y> Specification<T> inValues(String attr, Collection<? extends Y> values) {
        if (values == null || values.isEmpty()) return null;
        return (root, q, cb) -> root.get(attr).in(values);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> between(String attr, Y from, Y to) {
        if (from == null && to == null) return null;

        return (root, query, cb) -> {
            Path<Y> path = root.get(attr);

            if (from != null && to != null) {
                return cb.between(path, from, to);
            }

            if (from != null) {
                return cb.greaterThanOrEqualTo(path, from);
            }

            return cb.lessThanOrEqualTo(path, to);
        };
    }
}
