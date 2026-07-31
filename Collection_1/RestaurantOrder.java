package Collection_1;

import java.util.ArrayList;

public class RestaurantOrder {
    public static void main(String[] args) {

        ArrayList<String> order = new ArrayList<>();

        order.add("Dosa");
        order.add("Idli");
        order.add("Pongal");
        order.add("Coffee");

        order.remove("Pongal");

        System.out.println("Updated Order List:");
        for (String item : order) {
            System.out.println(item);
        }

        System.out.println("Total Items: " + order.size());
    }
}