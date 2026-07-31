public class SmartHomeDeviceController {
    public static void main(String[] args) {
        Device[] devices = {
            new Light("Living Room Light", 20),
            new Fan("Bedroom Fan", 75),
            new AirConditioner("Hall AC", 1500)
        };

        for (Device d : devices) {
            d.turnOn();
            d.displayPowerConsumption(2);
            d.turnOff();
        }
    }
}
class Device {
    protected String name;
    protected double power;
    protected boolean on;

    Device(String name, double power) {
        this.name = name;
        this.power = power;
        this.on = false;
    }

    void turnOn() {
        on = true;
        System.out.println(name + " turned ON.");
    }

    void turnOff() {
        on = false;
        System.out.println(name + " turned OFF.");
    }

    void displayPowerConsumption(int hours) {
        System.out.println(name + " consumption: " +
                (on ? power * hours : 0) + " Wh");
    }
}

class Light extends Device {
    Light(String name, double power) { super(name, power); }
}

class Fan extends Device {
    Fan(String name, double power) { super(name, power); }
}

class AirConditioner extends Device {
    AirConditioner(String name, double power) { super(name, power); }
}
