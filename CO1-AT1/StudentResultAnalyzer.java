public class StudentResultAnalyzer {
    public static void main(String[] args) {
        Student[] students = {
            new Student(1, "Arun", 90, 85, 95),
            new Student(2, "Bala", 80, 75, 85),
            new Student(3, "Charan", 70, 65, 75)
        };

        Student topper = students[0];

        for (Student s : students) {
            s.display();
            if (s.total() > topper.total())
                topper = s;
        }

        System.out.println("Class Topper: " + topper.name);
    }
}
class Student {
    int rollNo;
    String name;
    int m1, m2, m3;

    Student(int rollNo, String name, int m1, int m2, int m3) {
        this.rollNo = rollNo;
        this.name = name;
        this.m1 = m1;
        this.m2 = m2;
        this.m3 = m3;
    }

    int total() {
        return m1 + m2 + m3;
    }

    double average() {
        return total() / 3.0;
    }

    char grade() {
        double avg = average();
        if (avg >= 90) return 'A';
        if (avg >= 75) return 'B';
        if (avg >= 60) return 'C';
        if (avg >= 50) return 'D';
        return 'F';
    }

    void display() {
        System.out.println(rollNo + " | " + name +
                " | Total: " + total() +
                " | Average: " + average() +
                " | Grade: " + grade());
    }
}
