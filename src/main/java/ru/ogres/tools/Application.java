package ru.ogres.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zed on 20.07.16.
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@Controller
@Slf4j
public class Application {

    @Autowired
    RepoTest repoTest;

    public class Box<T> {

    }



    @RequestMapping(path="/")
    @ResponseBody
    public String list() {
        log.info("Type[] genericTypes =((ParameterizedType)(e.getClass().getGenericSuperclass())).getActualTypeArguments();");
        log.info("repoTest.getClass() = {}", RepoTest.class.getName());
        //log.info("repoTest.getClass().getGenericSuperclass().typeName() = {}", RepoTest.class.getClass().getGenericSuperclass()..getTypeName());
        log.info("{}",((ParameterizedType)(RepoTest.class.getGenericInterfaces()[0])).getActualTypeArguments()[0]);
        log.info("{}",((ParameterizedType)(RepoTest.class.getGenericInterfaces()[0])).getActualTypeArguments()[1]);

        for(Type t : RepoTest.class.getInterfaces()){
            log.info("Type:{}", t.getTypeName());
            log.info("Type: == CrudRepository {}", t == CrudRepository.class);
        }

//        for(Type t : ((ParameterizedType)(RepoTest.class.getGenericSuperclass())).getActualTypeArguments()) {
//            log.info(t.getClass().getName());
//        }
        return "ok";
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
