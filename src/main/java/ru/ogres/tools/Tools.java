package ru.ogres.tools;

import com.squareup.javapoet.CodeBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zed on 17.08.16.
 */
public final class Tools {

    public static CodeBlock buildTable(String name, TypeElement typeElement, Iterable<Field> fields, String page, String pageSize) {
        CodeBlock.Builder code = CodeBlock.builder();
        List<Field> list = Field.sorted(fields);

        code.add("$T<$T> page = repo.findAll(new $T($L, $L));", Page.class, typeElement, PageRequest.class, page, pageSize);
        code.add("$T table = new $T();\n", Table.class, Table.class);
        code.add("table.first = page.isFirst();\n" +
                "table.last = page.isLast();\n" +
                "table.totalPages = page.getTotalPages();\n" +
                "table.totalElements = page.getTotalElements();\n" +
                "table.number = page.getNumber();\n" +
                "table.size = page.getSize();\n" +
                "table.name = $S;\n", name);

        code.add("$T<$T> data = page.getContent();\n", Iterable.class, typeElement);

        code.add("table.fields = new $T[]{\n", Field.class);
        for(int i=0;i<list.size();i++){
            code.add("  $L", buildField(list.get(i)));
            if (i<list.size()-1) code.add(", \n");
        }
        code.add("\n};\n");

        code.add("for($T item : data) {\n", typeElement);
        code.add("  table.data.add(new $T[] {", Object.class);
        for(int i=0;i<list.size();i++){
            code.add("item.get$L()", StringUtils.capitalize(list.get(i).field));
            if (i<list.size()-1) code.add(", ");
        }
        code.add("});\n");
        code.add("}\n");
        code.add("map.put($S,$L);\n", "table", "table");
        return code.build();
    }



    public static CodeBlock buildField(Field i) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("new $T($S, $S, $S, $S, $L)", Field.class, i.field, i.name, i.type, i.description, i.index);
        return code.build();
    }

    public static CodeBlock buildFields(Iterable<Field> fields) {

        CodeBlock.Builder code = CodeBlock.builder();
        code.add("$T<$T> fields = new $T<>();\n", List.class, Field.class, ArrayList.class);
        Field.sorted(fields).forEach((i) -> code.add("fields.add($L);\n", buildField(i)));

        return code.build();
    }

    public static CodeBlock buildData(Iterable<Data> datas) {
        CodeBlock.Builder code = CodeBlock.builder();
        return code.build();
    }

}
