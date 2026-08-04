import java.util.Scanner;

public class CharacterIndex {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String str = sc.nextLine();
        char ch = sc.nextLine().charAt(0);

        int index = -1;

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                index = i;
                break;
            }
        }

        if (index == -1)
            System.out.println("Character not found");
        else
            System.out.println(ch + " " + index);
        sc.close();
    }
}