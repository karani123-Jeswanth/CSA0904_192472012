package Collection_1;

import java.util.ArrayList;

public class EmployeeList {
    public static void main(String[] args) {

        ArrayList<String> employees = new ArrayList<>();

        employees.add("Ramesh");
        employees.add("Suresh");
        employees.add("Priya");
        employees.add("Anu");
        employees.add("Karthik");

        System.out.println("Employee Names:");
        for (String name : employees) {
            System.out.println(name);
        }
    }
}