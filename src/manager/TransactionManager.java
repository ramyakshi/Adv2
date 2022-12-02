package manager;

import java.io.*;
import java.util.*;
import components.Data;
import components.LockType;
import components.Site;
import components.Transaction;
import data.Pair;
import data.TempWrite;
//Todo: On read do we check for commit time of variable
public class TransactionManager {
	List<Site> sites;
	Set<Integer> availableSites;
	Set<Integer> failedSites;
	Map<String, List<Site>> dataSitesMap;
	List<Transaction> transactions;
	// Set<Integer> affectedTransaction = new HashSet<>();
	Map<String, Pair> variableLockMap;  
	Queue<Transaction> waitQueue;
	DeadLockManager deadlockManager;
	TempWrite tempWrite;
	int time = 0;

	public TransactionManager() {
		this.transactions = new ArrayList<Transaction>();
		this.sites = new ArrayList<>();
		this.dataSitesMap = new HashMap<>();
		this.variableLockMap = new HashMap<>();
		this.waitQueue = new LinkedList<>();
		this.deadlockManager = new DeadLockManager();
		this.availableSites = new HashSet<>();
		this.failedSites = new HashSet<>();
		this.tempWrite = new TempWrite();
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
				if(!s.getLockTable().contains(data.getVarName())) {
					s.getLockTable().initializeLockType(data.getVarName());
				}	
				List<Site> usedSites;
				if (dataSitesMap.containsKey(data.getVarName())) {
					usedSites = dataSitesMap.get(data.getVarName());
				} else {
					usedSites = new ArrayList<>();
				}
				usedSites.add(s);
				dataSitesMap.put(data.getVarName(), usedSites);
				availableSites.add(s.getId());
			}
			// Even variables Will go to all sites
			else if (i % 2 == 0) {
				for (int j = 1; j <= 10; j++) {
					Site s = sites.get(j - 1);
					s.setData(data);
					s.setLastCommittedTime(0, i);
					// Added data to the site's locktable 
					if(!s.getLockTable().contains(data.getVarName())) {
						s.getLockTable().initializeLockType(data.getVarName());
					}
					List<Site> usedSites;
					if (dataSitesMap.containsKey(data.getVarName())) {
						usedSites = dataSitesMap.get(data.getVarName());
					} else {
						usedSites = new ArrayList<>();
					}
					usedSites.add(s);
					dataSitesMap.put(data.getVarName(), usedSites);
					availableSites.add(s.getId());
				}

			}
		}
	}


	public void readFile(String fileName) throws Exception {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line = reader.readLine();
			while (line != null) {
				// Step 1: Check for deadlock
				// Step 2: Check if any transaction is waiting in Queue
				if(deadlockManager.isDeadlockPresent()) {
					Transaction victim = deadlockManager.removeYoungestDeadlock(transactions);
					System.out.println("Aborting youngest transacation due to deadlock. Transaction Id: "+victim.getId());
				}
				time++;
				if(line.startsWith("beginRO")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					Transaction transaction = new Transaction(transactionId, time, "RO");
					transactions.add(transaction);
				}
				else if(line.startsWith("begin")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					Transaction transaction = new Transaction(transactionId, time, null);
					transactions.add(transaction);


				}
				else if(line.startsWith("end")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					System.out.println("End transaction "+ transactionId);

				}
				else if(line.startsWith("fail")) {
					int siteId = Integer.parseInt(line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim());
					handleFailRequest(siteId);

				}
				else if(line.startsWith("recover")) {
					int siteId = Integer.parseInt(line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim());
					handleRecoverRequest(siteId);

				}
				else if(line.startsWith("dump")){
					System.out.println("Dump " + line );
				}
				else if(line.startsWith("R")) {
					System.out.println("Starting Read: "+line);
					Transaction transaction = null;
					String fields = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();
					String tId = fields.split(",")[0].trim();
					int transactionId = Integer.parseInt(tId.substring(1));
					String variable = fields.split(",")[1].trim();

					for(Transaction trans: transactions) {
						if(trans.getId() == transactionId && trans.getType() == null) {
							transaction = trans;
							trans.setType("R");
							trans.setVariable(variable);
						}
						else if(trans.getId() == transactionId) {
							transaction = trans;
							trans.setVariable(variable);
						}              
					}
					if(transaction.getType().equals("R")) {
						handleReadRequest(transaction);
					}
					else if(transaction.getType().equals("RO")) {
						handleReadOnlyRequest(transaction);
					}
				}

				else if(line.startsWith("W")) {
					Transaction transaction = null;
					String fields = line.substring(line.indexOf('(') + 1, line.indexOf(')')).trim();
					String tId = fields.split(",")[0].trim();
					int transactionId = Integer.parseInt(tId.substring(1));
					String variable = fields.split(",")[1].trim();
					int value = Integer.parseInt(fields.split(",")[2].trim());

					for(Transaction trans: transactions) {
						if(trans.getId() == transactionId)
							transaction = trans;
					}
					transaction.setType("W");
					transaction.setVariable(variable);
					handleWriteRequest(transaction, value);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleRecoverRequest(int siteId) {
		Site site = null;
		for(Site s: sites) {
			if(s.getId() == siteId) {
				site = s;
				break;
			}
		}
		if(site == null) {
			System.out.println("Site not present. Can not recover");
			return;
		}
		if(failedSites.contains(siteId)) {
			failedSites.remove(siteId);
		}
		availableSites.add(siteId);
	}

	private void handleFailRequest(int siteId) {
		Site site = null;
		for(Site s: sites) {
			if(s.getId() == siteId) {
				site = s;
				break;
			}
		}
		if(site == null) {
			System.out.println("Site not present. Can not fail");
			return;
		}
		if(availableSites.contains(siteId))
			availableSites.remove(site.getId());
		failedSites.add(site.getId());
		site.getLockTable().resetLockTable();
		for(Data data: site.getData()) {
			site.setStaleData(data);
			site.removeData(data);
		}
		// tid -> list of varNames
		Map<Integer, List<String>> map = site.getTransactionVarMap();
		for(Map.Entry<Integer, List<String>> entry: map.entrySet()) {
			int tId = entry.getKey();
			for(String varName: entry.getValue()) {
				if(tempWrite.contains(varName))
					tempWrite.editValue(varName, tId, siteId);
			}
		}
		System.out.println("Site failed "+siteId);

	}

	private void handleWriteRequest(Transaction transaction, int value) throws Exception {
		int transactionId = transaction.getId();
		String variable = transaction.getVariable();	
		// To get sites that hold the variable
		List<Site> usedSites = dataSitesMap.get(variable);
		boolean areAllSitesAvailable = true;
		Set<Pair> transactionsThatHoldLock = new HashSet<Pair>();
		int numOfAvailableSites = 0;
		for(Site s: usedSites) {
			int siteId = s.getId();

			if(availableSites.contains(siteId))
				numOfAvailableSites++;

			if(availableSites.contains(siteId) &&  s.getLockTable().isLocked(variable)) {
				areAllSitesAvailable = false;
				for(Pair p : s.getLockTable().getTransactionsThatHoldLock(variable)) {
					if(transactionsThatHoldLock.contains(p))
						continue;
					else
						transactionsThatHoldLock.add(p);
				}
			}
		}
		if(!areAllSitesAvailable) {
			for(Pair p : transactionsThatHoldLock) {
				int prevTransactionId = p.getTransactionId();
				deadlockManager.graph.addEdge(transactionId, prevTransactionId);
			}	

			if(numOfAvailableSites <= 0) {
				System.out.println("Transaction "+transactionId+" could not write since no sites are available");
				waitQueue.add(transaction);
			}

			else {
				System.out.println("Transaction "+transactionId+" could not write since some other transaction holds lock on variable "+ variable);
				waitQueue.add(transaction);
			}
		}
		if(areAllSitesAvailable)
			startWriteAction(transaction, usedSites, variable, value);
	}

	private void startWriteAction(Transaction transaction, List<Site> usedSites, String variable, int value) throws Exception {
		int transactionId = transaction.getId();
		int transactionTime = transaction.getTime();
		for(Site s: usedSites) {
			List<Data> staleData = s.getStaleData();
			Data data = null;
			for(Data d: staleData) {
				if(d.getVarName().equals(variable)) {
					data = d;
					s.removeStaleData(variable);
					break;
				}
			}
			// Check if it is a stale data then move to regular, update time
			if(data == null) {
				//must be regular data
				List<Data> regularData = s.getData();
				for(Data d: regularData) {
					data = d;
					break;
				}
			}
			// Acquire lock on data 
			Pair p = new Pair(transactionId, LockType.WriteLock);
			s.getLockTable().setLock(p, variable);
			s.putTransactionVarMap(transactionId, variable);
			startTempWrite(transaction, s, value);
		}		
	}

	public void startTempWrite(Transaction t, Site s, int value) {
		String varName = t.getVariable();
		int tId = t.getId();
		if(!tempWrite.contains(varName)) {
			tempWrite.initializeEntry(varName, tId);
		}
		
		int siteId = s.getId();
		Integer newValue = Integer.valueOf(value);
		tempWrite.addNewValue(varName, tId, newValue, siteId);
	}

	private void handleReadOnlyRequest(Transaction transaction) throws Exception {
		int transactionId = transaction.getId();
		String variable = transaction.getVariable();
		int variableID = Integer.parseInt(variable.substring(1));
		boolean isAlreadyLocked = false;
		boolean foundData = false;
		List<Site> usedSites = dataSitesMap.get(variable);
		boolean isStale = false;
		int numOfAvailableSites = 0;

		for(Site s: usedSites) {
			int sitedId = s.getId();
			isStale = false;
			// Data can be read only when site is up, not stale and not write locked
			if(availableSites.contains(sitedId)) {
				numOfAvailableSites++;
				List<Data> allData = s.getData();
				Data dataToRead = null;
				// To get Data object from the string name passed as parameter
				for(Data data: allData) {
					if(data.getVarName().equals(variable)) {
						dataToRead = data;
						break;
					}
				}
				List<Data> staleData = s.getStaleData();
				for(Data d: staleData) {
					if(d.getVarName().equals(variable))
					{
						isStale = true;
						break;
					}
				}
				//Check if next site is available
				if(isStale)
					continue;
				// Now check if variable is locked in site
				if(s.getLockTable().contains(variable)) {
					if(!foundData && 
					(!s.getLockTable().isLocked(variable) || s.getLockTable().isOnlyReadLocked(variable) || s.getLockTable().isWriteLockedBySameTransaction(variable, transactionId))
					&& s.canReadData(transaction.getTime(), variableID))
					{
						foundData = true;
						System.out.println("Transaction "+transactionId+" read variable with value"+variable+ ": "+dataToRead.getValue());
					}
					else if(!isAlreadyLocked && s.getLockTable().isLocked(variable) && !s.getLockTable().isOnlyReadLocked(variable))
						{
							isAlreadyLocked = true;
							Pair p = s.getLockTable().getTransactionThatHoldsLock(variable);
							variableLockMap.put(variable, p);
						} 
				}
			}
		}
		// Find why it failed
		// Case 1: Some transaction was alreading holding a lock on the variable
		if(!foundData) {
			if(isAlreadyLocked) {
			Pair p = variableLockMap.get(variable);
			int prevTransactionId = p.getTransactionId();
			deadlockManager.graph.addEdge(transactionId, prevTransactionId);
			waitQueue.add(transaction);	
			System.out.println("Transaction "+transactionId+" could NOT read the varaible "+variable+" since transaction "+prevTransactionId+" holds write lock on it");
			}
		//Case 2: All sites with the variable are down
			else if(numOfAvailableSites <= 0) {
				waitQueue.add(transaction);	

			System.out.println("Transaction "+transactionId+" could not read the variable since all sites contain stale data");
			}
		//Case 3: All sites have staleData
			else if(isStale) {
			waitQueue.add(transaction);	
			System.out.println("Transaction "+transactionId+" could not read the variable since all sites contain stale data");
		}
	}
	}

	private void handleReadRequest(Transaction transaction) throws Exception {
		int transactionId = transaction.getId();
		String variable = transaction.getVariable();
		int variableId = Integer.parseInt(variable.substring(1));
		boolean isAlreadyLocked = false;
		boolean foundData = false;
		List<Site> usedSites = dataSitesMap.get(variable);
		boolean isStale = false;
		int numOfAvailableSites = 0;
	
		for(Site s: usedSites) {
			int sitedId = s.getId();
			isStale = false;
			// Data can be read only when site is up, not stale and not write locked
			if(availableSites.contains(sitedId)) {
				numOfAvailableSites++;
				List<Data> allData = s.getData();
				Data dataToRead = null;
				// To get Data object from the string name passed as parameter
				for(Data data: allData) {
					if(data.getVarName().equals(variable)) {
						dataToRead = data;
						break;
					}
				}
				List<Data> staleData = s.getStaleData();
				for(Data d: staleData) {
					if(d.getVarName().equals(variable))
					{
						isStale = true;
						break;
					}
				}
				//Check if next site is available
				if(isStale)
					continue;

				// Now check if variable is locked in site
				if(s.getLockTable().contains(variable)) {
					if(!foundData && 
					(!s.getLockTable().isLocked(variable) || s.getLockTable().isOnlyReadLocked(variable) || s.getLockTable().isWriteLockedBySameTransaction(variable, transactionId))
					&& s.canReadData(transaction.getTime(), variableId))
					{
						foundData = true;
						Pair pair = new Pair(transactionId, LockType.ReadLock);
						s.getLockTable().setLock(pair, variable);
						System.out.println("Transaction "+transactionId+" read variable "+variable+ ": "+dataToRead.getValue());
					}
					else if(!isAlreadyLocked && (s.getLockTable().isLocked(variable) && !s.getLockTable().isOnlyReadLocked(variable)))
						{
							isAlreadyLocked = true;
							Pair p = s.getLockTable().getTransactionThatHoldsLock(variable);
							variableLockMap.put(variable, p);
						} 
				}
			}
		}
		// Find why it failed
		// Case 1: Some transaction was  holding a lock on the variable
		if(!foundData) {
			if(isAlreadyLocked) {
			Pair p = variableLockMap.get(variable);
			int prevTransactionId = p.getTransactionId();
			deadlockManager.graph.addEdge(transactionId, prevTransactionId);
			waitQueue.add(transaction);	
			System.out.println("Transaction "+transactionId+" could NOT read the varaible "+variable+" since transaction "+prevTransactionId+" holds write lock on it");
			}
		//Case 2: All sites with the variable are down
			else if(numOfAvailableSites <= 0) {
			System.out.println("Transaction "+transactionId+" could NOT read the variable since all sites contain stale data");
			}
		//Case 3: All sites have staleData
			else if(isStale) {
			System.out.println("Transaction "+transactionId+" could NOT read the variable since all sites contain stale data");
		}
	}
}
}
