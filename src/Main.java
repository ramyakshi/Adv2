import manager.TransactionManager;

public class Main {
    public static void main(String[] args) {
    //    String fileName = args[0];
       TransactionManager transactionManager = new TransactionManager();
       transactionManager.initialize();
       transactionManager.printAll();
    }
}
