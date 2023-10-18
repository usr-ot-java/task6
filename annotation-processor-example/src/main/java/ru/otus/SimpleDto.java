package ru.otus;

import ru.otus.annotation.CustomToString;

@CustomToString
public class SimpleDto {
    static String STATIC = "Some static string";

    private int x = 5;
    public int y = 10;
    public Integer z = 100;
}
