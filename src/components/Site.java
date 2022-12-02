package components;

import java.util.*;

public class Site {
    int id;
    int lastCommittedTime[];
    List<Data> data;
    LockTable lockTable;
    List<Data> staleData;
    // map stores data about transaction that holds write lock on variable
    Map<Integer, List<String>> transactionVariableMap;
    public Site(int id) {
        this.id = id;
        lastCommittedTime = new int[21];
        //Empty data in sites will be initialized with -1 for time
        Arrays.fill(lastCommittedTime, -1);
        data = new ArrayList<Data>();
        this.lockTable = new LockTable();
        this.staleData = new ArrayList<>();
        this.transactionVariableMap = new HashMap<>();
    }   

    public void setLastCommittedTime(int t, int index) {
        lastCommittedTime[index] = t;
    }
    public List<Data> getData() {
        return this.data;
    }

    public void setData(Data d) {
        data.add(d);
    }

    public void removeData(Data d) {
        data.remove(d);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getLastCommittedTime() {
        return this.lastCommittedTime;
    }

    public List<Data> getStaleData() {
        return this.staleData;
    }

    public void setStaleData(Data data) {
       this.staleData.add(data);
    }

    public void removeStaleData(String id) {
        for(Data d : staleData) {
            if(d.getVarName() == id) {
                staleData.remove(d);
            }
        }
    }

    public LockTable getLockTable() {
        return this.lockTable;
    }

    public boolean canReadData(int transactionTime, int indexOfVariable) {
        if(transactionTime >= lastCommittedTime[indexOfVariable])
            return true;
        else    
            return false;

    }

    public Map<Integer, List<String>> getTransactionVarMap() {
        return this.transactionVariableMap;
    }

    public void putTransactionVarMap(int tId, String var) {
        if(transactionVariableMap.containsKey(tId))
            transactionVariableMap.get(tId).add(var);

        else {
            List<String> l = new ArrayList<String>();
            l.add(var);
            transactionVariableMap.put(tId, l);
        }
    }

    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", lastCommittedTime[]='" + getLastCommittedTime() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }
    
    
}

