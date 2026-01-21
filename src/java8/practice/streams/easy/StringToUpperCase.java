package java8.practice.streams.easy;

import java.util.Arrays;
import java.util.List;

public class StringToUpperCase {

    public static void main(String[] args) {

        List<String>lowerCaseString = Arrays.asList("apple", "banana", "cherry");
        lowerCaseString.stream().map(String::toUpperCase).toList().forEach(System.out::println);

    }
}
