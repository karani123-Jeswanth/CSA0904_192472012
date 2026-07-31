public class UniversityAdmissionSystem {
    public static void main(String[] args) {
        Admission a = new Admission();

        System.out.println("Undergraduate Fee: $" + a.calculateFee(50000));
        System.out.println("Postgraduate Fee: $" + a.calculateFee(70000, true));
        System.out.println("Scholarship Fee: $" + a.calculateFee(50000, 25));
    }
}
class Admission {
    double calculateFee(double ugFee) {
        return ugFee;
    }

    double calculateFee(double pgFee, boolean postgraduate) {
        return pgFee;
    }

    double calculateFee(double fee, double scholarshipPercent) {
        return fee - (fee * scholarshipPercent / 100);
    }
}
