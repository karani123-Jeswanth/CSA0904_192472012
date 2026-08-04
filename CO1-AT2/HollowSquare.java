import java.util.Scanner;

public class HollowSquare {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String s = sc.next();

        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                if (i == 1 || i == 4 || j == 1 || j == 4)
                    System.out.print(s + " ");
                else
                    System.out.print("  ");
            }
            System.out.println();
        }
        sc.close();
    }
}