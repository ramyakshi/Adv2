import java.io.File;
import components.*;
import manager.TransactionManager;

public class Main {
    public static void main(String[] args) {
       String fileName = new File("../inputs/input22.txt").getAbsolutePath();
       TransactionManager transactionManager = new TransactionManager();
       transactionManager.initialize();
    //    transactionManager.printAll();
        transactionManager.readFile(fileName);
    	
    	 /*Test tester = new Test();
    	 tester.TestLockTable();*/
    }
}