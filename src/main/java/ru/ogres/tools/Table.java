package ru.ogres.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by zed on 17.08.16.
 */
public class Table  {

    public String name;
    public Field[] fields;
    public List<Object[]> data;

    public boolean first = false;
    public boolean last = false;
    public int totalPages = 0;
    public long totalElements = 0;
    public int number = 0;
    public int size = 0;

    public List<Integer> getRange() {
          return IntStream.range(0, totalPages).boxed().collect(Collectors.toList());
    }


    public Table() {
        data = new ArrayList<>();
    }



}
