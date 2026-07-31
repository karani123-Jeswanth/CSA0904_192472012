package Collection_1;

import java.util.HashSet;

public class EmailRegistration {
    public static void main(String[] args) {

        HashSet<String> emails = new HashSet<>();

        emails.add("a@gmail.com");
        emails.add("b@gmail.com");
        emails.add("a@gmail.com");
        emails.add("c@gmail.com");

        System.out.println("Unique Email Addresses:");
        for (String email : emails) {
            System.out.println(email);
        }

        System.out.println("Total Unique Users: " + emails.size());
    }
}