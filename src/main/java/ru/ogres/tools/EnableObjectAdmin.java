package ru.ogres.tools;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by zed on 23.07.16.
 */
@ComponentScan(basePackages = {"ru.ogres.tools.repo"})
@EnableJpaRepositories(basePackages = {"ru.ogres.tools.repo"})
public @interface EnableObjectAdmin {
}
