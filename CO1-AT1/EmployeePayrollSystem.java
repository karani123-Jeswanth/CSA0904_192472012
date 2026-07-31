public class EmployeePayrollSystem {
    public static void main(String[] args) {
        Employee e1 = new PermanentEmployee(101, "Arun", 40000, 5000);
        Employee e2 = new ContractEmployee(102, "Bala", 1500, 20);

        e1.display();
        System.out.println("Salary: $" + e1.calculateSalary());

        e2.display();
        System.out.println("Salary: $" + e2.calculateSalary());
    }
}
class Employee {
    protected int id;
    protected String name;

    Employee(int id, String name) {
        this.id = id;
        this.name = name;
    }

    double calculateSalary() {
        return 0;
    }

    void display() {
        System.out.println("ID: " + id + ", Name: " + name);
    }
}

class PermanentEmployee extends Employee {
    private double basicSalary;
    private double allowance;

    PermanentEmployee(int id, String name, double basicSalary, double allowance) {
        super(id, name);
        this.basicSalary = basicSalary;
        this.allowance = allowance;
    }

    @Override
    double calculateSalary() {
        return basicSalary + allowance;
    }
}

class ContractEmployee extends Employee {
    private double dailyRate;
    private int daysWorked;

    ContractEmployee(int id, String name, double dailyRate, int daysWorked) {
        super(id, name);
        this.dailyRate = dailyRate;
        this.daysWorked = daysWorked;
    }

    @Override
    double calculateSalary() {
        return dailyRate * daysWorked;
    }
}
