package ru.ogres.tools;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by zed on 17.08.16.
 */
public class Field {

    String field;
    String name;
    String type;
    String description = "";
    Integer index = 1;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Field{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", index=" + index +
                '}';
    }

    public Field() {}

    public Field(String field, String name, String type, String description, Integer index) {
        this.field = field;
        this.name = name;
        this.type = type;
        this.description = description;
        this.index = index;
    }

    public static List<Field> sorted(Iterable<Field> fields) {
        List<Field> list = new ArrayList<>();
        for(Field field : fields)
            list.add(field);
        return list.stream().sorted((f1, f2) -> Integer.compare(f1.index,f2.index)).collect(Collectors.toList());
    }


}
