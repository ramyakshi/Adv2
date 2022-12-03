/**
 * @author Shraddha Iyer
 * @version 1.0.0
 * @date 11/17/2021
 */

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
    TreeMap<Integer, Integer> siteUpDownMap;
    public Site(int id) {
        this.id = id;
        lastCommittedTime = new int[21];
        //Empty data in sites will be initialized with -1 for time
        Arrays.fill(lastCommittedTime, -1);
        data = new ArrayList<Data>();
        this.lockTable = new LockTable();
        this.staleData = new ArrayList<>();
        this.transactionVariableMap = new HashMap<>();
        this.siteUpDownMap = new TreeMap<>();
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
    
    public TreeMap<Integer, Integer> getsiteUpDownMap() {
        return siteUpDownMap;
    }

    public void setsiteUpDownMap(TreeMap<Integer, Integer> siteUpDownMap) {
        this.siteUpDownMap = siteUpDownMap;
    }
    
    public void addsiteUpDownMap(int start, int end)
    {
    	this.siteUpDownMap.put(start,end);
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

    public void resetLastCommittedTime() {
        Arrays.fill(this.lastCommittedTime, -1);
    }

    public boolean canReadOnlyProceed(String variable, Transaction t)
    {
    	int tStart = t.getTime();
    	int varId = Integer.parseInt(variable.substring(1));
    	int lastWriteTime = lastCommittedTime[varId];
    	
    	int lastUpTime = siteUpDownMap.lowerKey(lastWriteTime+1);
    	for(int i =lastWriteTime; i<=tStart;i++)
    	{
    		if(siteUpDownMap.get(lastUpTime) <= tStart)
    			return false;
    	}
    	return true;
    }
    public void print() {
        System.out.print("site " + this.id + " - ");
        List<Data> copy = data;
        Collections.sort(copy, new Comparator<Data>() {
            @Override
            public int compare(Data a, Data b) {
                int i1 = Integer.parseInt(a.getVarName().substring(1));
                int i2 = Integer.parseInt(b.getVarName().substring(1));
                return i1-i2;
            }
        });

        for(Data d : copy) {
            System.out.print(d.getVarName() + ": "+d.getValue()+ " ");
        }
    }
    
    
}

