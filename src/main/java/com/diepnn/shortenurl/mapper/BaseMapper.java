package com.diepnn.shortenurl.mapper;

public interface BaseMapper<E, D> {
    E toEntity(D dto);
    D toDTO(E entity);
}
