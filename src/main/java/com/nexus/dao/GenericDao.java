package com.nexus.dao;

import java.util.List;
import java.util.Optional;

public interface GenericDao<T> {
    void create(T entity);
    Optional<T> read(Integer id);
    void update(T entity);
    void delete(Integer id);
    List<T> findAll();
}
