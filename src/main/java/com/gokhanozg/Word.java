package com.gokhanozg;

import java.util.Objects;

public class Word implements Comparable<Word> {
    private String value;
    private Integer count;

    public Word(String value, Integer count) {
        this.value = value;
        this.count = count;
    }

    @Override
    public int compareTo(Word o) {
        return o.getCount().compareTo(count);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word word = (Word) o;
        return value.equals(word.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Word{" +
                "value='" + value + '\'' +
                '}';
    }
}
