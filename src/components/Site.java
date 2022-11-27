package components;

import java.util.*;

public class Site {
    int id;
    int lastCommittedTime[];
    List<Data> data;
    public Site(int id) {
        this.id = id;
        lastCommittedTime = new int[21];
        //Empty data in sites will be initialized with -1 for time
        Arrays.fill(lastCommittedTime, -1);
        data = new ArrayList<Data>();
    }   

    public void setLastCommittedTime(int t, int index) {
        lastCommittedTime[index] = t;
    }

    public void setData(Data d) {
        data.add(d);
    }

    
    
}
