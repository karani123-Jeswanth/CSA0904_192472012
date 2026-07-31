public class BankAccountSystem{
    public static void main(String[] args) {
        BankAccount account = new BankAccount("ACC1001", "Jeswanth", 10000);

        account.display();
        account.deposit(5000);
        account.withdraw(3000);
        account.withdraw(20000);
        account.deposit(-500);
        System.out.println("Final Balance: $" + account.getBalance());
    }
}
class BankAccount {
    private String accountNumber;
    private String holderName;
    private double balance;

    BankAccount(String accountNumber, String holderName, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            System.out.println("Deposit successful.");
        } else {
            System.out.println("Invalid deposit amount.");
        }
    }

    public void withdraw(double amount) {
        if (amount <= 0)
            System.out.println("Invalid withdrawal amount.");
        else if (amount > balance)
            System.out.println("Insufficient balance.");
        else {
            balance -= amount;
            System.out.println("Withdrawal successful.");
        }
    }

    public double getBalance() {
        return balance;
    }

    public void display() {
        System.out.println("Account: " + accountNumber);
        System.out.println("Holder: " + holderName);
        System.out.println("Balance: $" + balance);
    }
}
