package Collection_1;

import java.util.ArrayList;

public class CourseEnrollment {
    public static void main(String[] args) {

        ArrayList<String> students = new ArrayList<>();

        students.add("Arun");
        students.add("Meena");
        students.add("Rahul");
        students.add("Divya");
        students.add("Kiran");

        if (students.contains("Meena")) {
            System.out.println("Enrolled");
        } else {
            System.out.println("Not Enrolled");
        }
    }
}