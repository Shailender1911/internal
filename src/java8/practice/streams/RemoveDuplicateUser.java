package java8.practice.streams;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
class User {
    private int id;
    private String name;
}

public class RemoveDuplicateUser {
    public static void main(String[] args) {
        List<User> users = Arrays.asList(
                new User(5, "Alice"),
                new User(2, "Bob"),
                new User(8, "Alice"),
                new User(1, "Charlie"),
                new User(3, "Bob"),
                new User(9, "Alice")
        );

        System.out.println("Original list:");
        users.forEach(System.out::println);

        List<User> filtered = removeDuplicates(users);

        System.out.println("\nFiltered list (lowest ID kept):");
        filtered.forEach(System.out::println);
    }

    // **Approach 1: Using Stream API (Best for Java 21)** ⭐
    public static List<User> removeDuplicates(List<User> users) {
        return new ArrayList<>(users.stream().collect(Collectors.toMap(User::getName, user -> user, (u1, u2) -> u1.getId() < u2.getId() ? u1 : u2)).values());
    }

    // **Approach 2: Using Collectors.groupingBy** 🎯
    public static List<User> removeDuplicatesV2(List<User> users) {
        return users.stream()
                .collect(Collectors.groupingBy(User::getName))
                .values()
                .stream()
                .map(userList -> userList.stream()
                        .min(Comparator.comparingInt(User::getId))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // **Approach 3: Manual (No Streams)** 🔧
    public static List<User> removeDuplicatesManual(List<User> users) {
        Map<String, User> map = new HashMap<>();

        for (User user : users) {
            String name = user.getName();
            if (!map.containsKey(name) || map.get(name).getId() > user.getId()) {
                map.put(name, user);
            }
        }

        return new ArrayList<>(map.values());
    }
}
