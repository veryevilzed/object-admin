package ru.ogres.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

/**
 * Created by zed on 20.07.16.
 */
@Repository
public class RepoTestService {

    @Autowired
    RepoTest repoTest;

    @PostConstruct
    @Transactional
    public void populate() {
        Test t = new Test(1, "test");
        repoTest.save(t);
        t = new Test(2, "zed");
        repoTest.save(t);
    }
}