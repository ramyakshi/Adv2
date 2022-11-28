import components.*;
import manager.*;
public class Test {

	public static void TestLockTable()
	{
		try {
		LockTable table = new LockTable();
        Data test = new Data("x",1);
        table.put(test,LockType.WriteLock);
        System.out.println(table.isLocked(test));     
        System.out.println(table.getLockType(test));
        table.unlock(test);
        System.out.println(table.isLocked(test));
        
        Data test1 = new Data("x",2);
        System.out.println(table.getLockType(test1));
		}
		catch(Exception ex)
		{
			System.out.println(ex);
		}
	}
}
