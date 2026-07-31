public class HospitalManagementSystem {
    public static void main(String[] args) {
        MedicalRecord patient = new Patient(101, "Ravi", "Fever");
        MedicalRecord doctor = new Doctor(201, "Dr. Kumar", "Cardiology");

        patient.addRecord();
        patient.displayRecord();

        doctor.addRecord();
        doctor.displayRecord();
    }
}
interface MedicalRecord {
    void addRecord();
    void displayRecord();
}

class Patient implements MedicalRecord {
    private int id;
    private String name;
    private String disease;

    Patient(int id, String name, String disease) {
        this.id = id;
        this.name = name;
        this.disease = disease;
    }

    public void addRecord() {
        System.out.println("Patient record added.");
    }

    public void displayRecord() {
        System.out.println("Patient ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Disease: " + disease);
    }
}

class Doctor implements MedicalRecord {
    private int id;
    private String name;
    private String specialization;

    Doctor(int id, String name, String specialization) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
    }

    public void addRecord() {
        System.out.println("Doctor record added.");
    }

    public void displayRecord() {
        System.out.println("Doctor ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Specialization: " + specialization);
    }
}
