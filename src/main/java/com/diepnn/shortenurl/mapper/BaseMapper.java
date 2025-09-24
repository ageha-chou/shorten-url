package com.diepnn.shortenurl.mapper;

/**
 * Base mapper for converting between entity and DTO
 * @param <E> Entity
 * @param <D> DTO
 */
public interface BaseMapper<E,D> {
    D toDto(E s);
}
