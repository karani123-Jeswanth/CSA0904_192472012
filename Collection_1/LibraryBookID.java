package Collection_1;

import java.util.HashSet;

public class LibraryBookID {
    public static void main(String[] args) {

        HashSet<Integer> bookIDs = new HashSet<>();

        int[] ids = {101, 102, 103, 101, 104};

        for (int id : ids) {
            if (!bookIDs.add(id)) {
                System.out.println("Duplicate Book ID Found");
            }
        }

        System.out.println("Unique Book IDs:");
        for (int id : bookIDs) {
            System.out.println(id);
        }
    }
}