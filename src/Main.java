import java.io.File;
import components.*;
import manager.TransactionManager;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the complete input file path: ");
        String file = scanner.nextLine();
       String fileName = new File(file).getAbsolutePath();

       TransactionManager transactionManager = new TransactionManager();
       transactionManager.initialize();
        transactionManager.readFile(fileName);
    }
}
