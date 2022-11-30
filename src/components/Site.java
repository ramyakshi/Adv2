package components;

import java.util.*;

public class Site {
    int id;
    int lastCommittedTime[];
    List<Data> data;
    LockTable lockTable;
    public Site(int id) {
        this.id = id;
        lastCommittedTime = new int[21];
        //Empty data in sites will be initialized with -1 for time
        Arrays.fill(lastCommittedTime, -1);
        data = new ArrayList<Data>();
        this.lockTable = new LockTable();

    }   

    public void setLastCommittedTime(int t, int index) {
        lastCommittedTime[index] = t;
    }

    public void setData(Data d) {
        data.add(d);
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


    public List<Data> getData() {
        return this.data;
    }

    public LockTable getLockTable() {
        return this.lockTable;
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

