package components;

import java.util.*;

import data.Pair;
// Key - Data's varname
// VAlue - Transaction Id and Lock
public class LockTable extends HashMap<String,List<Pair>>{
    
    public LockTable(){

    }

    public boolean contains(String data) {
        if(!super.containsKey(data))
            return false;
        return true;
    }

    public boolean isLocked(String data) {
        if(super.containsKey(data)) {
            if(super.get(data).size() > 0)
                return true;
        }
        return false;
    }

    public boolean isOnlyReadLocked(String data) throws Exception {
        if(!super.containsKey(data)) {
            throw new Exception("Data not in LockTable");
        }
        boolean isOnlyReadLocked = true;

        for(Pair pair: super.get(data)) {
            if(pair.getLockType().equals(LockType.WriteLock))
                isOnlyReadLocked = false;
        }
        return isOnlyReadLocked;
    }


    public List<Pair> getLockType(int data) throws Exception
    {
    	if(!super.containsKey(data)) {
    		return null;
    	}
        return super.get(data);
    }

    public void initializeLockType(String data) {
        List<Pair> lockTypes = new ArrayList<Pair>();
        super.put(data, lockTypes);
    }


    public void setLock(Pair pair, String data) throws Exception {
        if(!super.containsKey(data))
            throw new Exception("Data not in LockTable");
        else
        {
        	if(super.get(data)==null)
        	{
        		super.put(data, new ArrayList<>());
        	}
        	super.get(data).add(pair);
        }
    }

    public boolean isWriteLockedBySameTransaction(String data, int transactionId) throws Exception {
        if(!super.containsKey(data))
            throw new Exception("Data not in LockTable");

        boolean writeLocked = false;
        List<Pair> pairs = super.get(data);

        for(Pair p : pairs) {
            int tId = p.getTransactionId();

            if(tId == transactionId) { 
                if(p.getLockType().equals(LockType.WriteLock)) {
                    writeLocked = false;
                    break;
                }       
            }
        }

        return writeLocked;
    }


    public Pair getTransactionThatHoldsLock(String data) throws Exception {
        if(!super.containsKey(data))
            throw new Exception("Data not in LockTable");

        for(Pair p: super.get(data)) {
            if(p.getLockType().equals(LockType.WriteLock))
                return p;
        }
        return null;
    }


    public List<Pair> getTransactionsThatHoldLock(String data) throws Exception {
        if(!super.containsKey(data))
            throw new Exception("Data not in LockTable");

        List<Pair> pairs = new ArrayList<Pair>();

        for(Pair p: super.get(data)) {
            if(p.getLockType().equals(LockType.WriteLock) || p.getLockType().equals(LockType.ReadLock)) {
                pairs.add(p);
            }
        }
        return pairs;
    }

    public void resetLockTable() {
        for(String s: super.keySet()) {
            List<Pair> list = new ArrayList<Pair>();
            super.put(s, list);
        }
    }

}

