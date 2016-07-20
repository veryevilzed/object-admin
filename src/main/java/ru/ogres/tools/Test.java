package ru.ogres.tools;

import lombok.AllArgsConstructor;

import lombok.NoArgsConstructor;
import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

/**
 * Created by zed on 20.07.16.
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @lombok.Setter @lombok.Getter private long id;

    @lombok.Setter @lombok.Getter private String someText;

}
