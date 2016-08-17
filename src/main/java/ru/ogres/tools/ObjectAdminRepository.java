package ru.ogres.tools;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.Serializable;

/**
 * Created by zed on 17.08.16.
 */
public interface ObjectAdminRepository<T, ID extends Serializable> {

    Page<T> findAll(Pageable pageable);

    T findOne(ID id);

    <S extends T> S save(S obj);

    boolean exists(ID id);

    long count();

    void delete(ID id);

    void delete(Iterable<? extends T> ids);
}
