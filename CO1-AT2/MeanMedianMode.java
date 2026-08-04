import java.util.*;

public class MeanMedianMode {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n = 7;
        int[] a = new int[n];

        int sum = 0;

        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
            sum += a[i];
        }

        int mean = sum / n;

        Arrays.sort(a);
        int median = a[n / 2];

        int mode = a[0];
        int max = 0;

        for (int i = 0; i < n; i++) {
            int count = 1;
            for (int j = i + 1; j < n; j++) {
                if (a[i] == a[j])
                    count++;
            }

            if (count > max) {
                max = count;
                mode = a[i];
            }
        }

        System.out.println("Mean = " + mean);
        System.out.println("Median = " + median);
        System.out.println("Mode = " + mode);
        sc.close();
    }
}