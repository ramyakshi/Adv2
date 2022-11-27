package manager;

import java.io.*;
import java.util.*;

import components.Data;
import components.Site;

public class TransactionManager {
    List<Site> sites;
    Map<String, List<Site>> dataSitesMap;
    int time = 0;

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


    public void readFile(String fileName) {
        BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			while (line != null) {
                // Step 1: Check for deadlock
                // Step 2: Check if any transaction is waiting in Queue
                // Step 3: Check for type of incoming transaction
                // Step 3.1 : If BeginRO
                // Step 3.2 : If Begin
                // Step 3.3 : If end
                // Step 3.4 : If fail
                // Step 3.5 : Read
                // Step 3.6 : Write
                // Step 3.7 : Dump
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }


    // public void printAll() {
    //     for(int i = 0; i<10; i++) {
    //         Site s = sites.get(i);
    //         System.out.println(s.toString());
    //     }
    // }

}
