package manager;

import java.io.File;
import java.util.*;

import components.Data;
import components.Site;

public class TransactionManager {
    List<Site> sites;
    Map<String, List<Site>> dataSitesMap;

    public TransactionManager() {
        this.sites = new ArrayList<>();
        dataSitesMap = new HashMap<>();
    }

    /**
     * Initializes all the sites with some data
     */
    public void initialize() {

        for (int i = 1; i <= 10; i++) {
            Site site = new Site(i);
            sites.add(site);
        }

        for (int i = 1; i <= 20; i++) {
            Data data = new Data("x" + i, 10 * i);
            // Old variables will go to one site only
            if (i % 2 == 1) {
                Site s = sites.get(i % 10);
                s.setData(data);
                s.setLastCommittedTime(0, i);
                List<Site> usedSites;
                if (dataSitesMap.containsKey(data.getVarName())) {
                    usedSites = dataSitesMap.get(data.getVarName());
                } else {
                    usedSites = new ArrayList<>();
                }
                usedSites.add(s);
                dataSitesMap.put(data.getVarName(), usedSites);
            }
            // Even variables Will go to all sites
            else if (i % 2 == 0) {
                for (int j = 1; j <= 10; j++) {
                    Site s = sites.get(j - 1);
                    s.setData(data);
                    s.setLastCommittedTime(0, i);
                    List<Site> usedSites;
                    if (dataSitesMap.containsKey(data.getVarName())) {
                        usedSites = dataSitesMap.get(data.getVarName());
                    } else {
                        usedSites = new ArrayList<>();
                    }
                    usedSites.add(s);
                    dataSitesMap.put(data.getVarName(), usedSites);
                }

            }
        }
    }


    public void printAll() {
        for(int i = 0; i<10; i++) {
            Site s = sites.get(i);
            System.out.println(s.toString());
        }
    }

}
