public class OnlineShoppingOrderSystem {
    public static void main(String[] args) {

        Product p1 = new Product(1, "Laptop", 800);
        Product p2 = new Product(2, "Mouse", 25);

        Order order = new Order();

        order.addProduct(p1);
        order.addProduct(p2);

        System.out.println("Order placed successfully.");
        order.displayOrder();
    }
}
class Product {
    int id;
    String name;
    double price;

    Product(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}

class Order {
    Product[] products = new Product[10];
    int count = 0;

    void addProduct(Product product) {
        products[count] = product;
        count++;
    }

    double calculateTotal() {
        double total = 0;

        for (int i = 0; i < count; i++) {
            total = total + products[i].price;
        }

        return total;
    }

    void displayOrder() {
        System.out.println("Order Details:");

        for (int i = 0; i < count; i++) {
            System.out.println(
                products[i].name + " - $" + products[i].price
            );
        }

        System.out.println("Total Amount: $" + calculateTotal());
    }
}