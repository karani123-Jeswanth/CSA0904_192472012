public class VehicleRentalSystem {
    public static void main(String[] args) {
        Vehicle[] vehicles = {
            new Car("CAR101", "Sedan"),
            new Bike("BIKE202", "Sports Bike"),
            new Bus("BUS303", "Tourist Bus")
        };

        int days = 3;
        for (Vehicle v : vehicles)
            v.displayBill(days);
    }
}
class Vehicle {
    String number, model;

    Vehicle(String number, String model) {
        this.number = number;
        this.model = model;
    }

    double calculateRent(int days) {
        return 0;
    }

    void displayBill(int days) {
        System.out.println(model + " (" + number + ") - " +
                days + " days - Rent: $" + calculateRent(days));
    }
}

class Car extends Vehicle {
    Car(String n, String m) { super(n, m); }
    @Override double calculateRent(int days) { return days * 2500; }
}

class Bike extends Vehicle {
    Bike(String n, String m) { super(n, m); }
    @Override double calculateRent(int days) { return days * 800; }
}

class Bus extends Vehicle {
    Bus(String n, String m) { super(n, m); }
    @Override double calculateRent(int days) { return days * 5000; }
}
