package ru.ogres.tools;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zed on 20.07.16.
 */
@Transactional
public interface RepoTest extends CrudRepository<Test, Long> {
}
