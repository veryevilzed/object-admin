package ru.ogres.tools;

import com.google.common.base.Strings;
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
public class Table  {

    public Field[] fields;
    public List<Object[]> data;

    public boolean first = false;
    public boolean last = false;
    public int totalPages = 0;
    public long totalElements = 0;
    public int number = 0;
    public int size = 0;

    public Table() {
        data = new ArrayList<>();
    }

    public static CodeBlock build(TypeElement typeElement, Iterable<Field> fields, String page, String pageSize) {
        CodeBlock.Builder code = CodeBlock.builder();
        List<Field> list = Field.sorted(fields);

        code.add("$T<$T> page = repo.findAll(new $T($L, $L));", Page.class, typeElement, PageRequest.class, page, pageSize);
        code.add("$T table = new $T();\n", Table.class, Table.class);
        code.add("table.first = page.isFirst();\n" +
                "table.last = page.isLast();\n" +
                "table.totalPages = page.getTotalPages();\n" +
                "table.totalElements = page.getTotalElements();\n" +
                "table.number = page.getNumber();\n" +
                "table.size = page.getSize();\n");

        code.add("$T<$T> data = page.getContent();\n", Iterable.class, typeElement);

        code.add("table.fields = new $T[]{\n", Field.class);
        for(int i=0;i<list.size();i++){
            code.add("  $L", Field.buildField(list.get(i)));
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

}
