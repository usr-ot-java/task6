package ru.otus;

import ru.otus.annotation.CustomToString;

@CustomToString
public class SimpleDtoMyToString {
    static String STATIC = "Some static string";

    private int x = 5;

    public int y = 10;
    public Integer z = 100;

    @Override
    public String toString() {
        return "SimpleDtoMyToString{}";
    }
}
