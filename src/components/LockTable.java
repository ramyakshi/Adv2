package components;

import java.util.HashMap;
public class LockTable extends HashMap<Data,LockType>{
    
    public LockTable(){

    }

    public boolean isLocked(Data data) throws Exception{
    	if(!super.containsKey(data)) {
    		throw new Exception("Data not in LockTable");
    	}
        if(super.get(data)==LockType.UnLocked)
        {
            return false;
        }
        return true;
    }

    public LockType getLockType(Data data) throws Exception
    {
    	if(!super.containsKey(data)) {
    		throw new Exception("Data not in LockTable");
    	}
        return super.get(data);
    }
    
    public void setLockType(Data data,LockType lock) {
    	super.put(data,lock);
    }
    
    public void unlock(Data data) throws Exception
    {
    	if(!super.containsKey(data)) {
    		throw new Exception("Data not in LockTable");
    	}
        super.put(data,LockType.UnLocked);
    }
}

