/**
 * @author Shraddha Iyer and Rmayakshi Mallik
 * @version 1.0.0
 * @date 11/29/2021
 */
package manager;

import java.io.*;
import java.util.*;
import components.Data;
import components.LockType;
import components.Site;
import components.Transaction;
import components.LockTable;
import data.Pair;
import data.TempWrite;
import data.VariableValue;
//Todo: On read do we check for commit time of variable
public class TransactionManager {
	List<Site> sites;
	Set<Integer> availableSites;
	Set<Integer> failedSites;
	Map<String, List<Site>> dataSitesMap;
	List<Transaction> transactions;
	Map<String, Pair> variableLockMap;  
	Map<Integer, List<VariableValue>> transVarMap;
	List<Integer> affectedTransaction;
	LinkedList<Transaction> waitQueue;
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
		this.transVarMap = new HashMap<>();
		this.affectedTransaction = new ArrayList<>();
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
				s.addsiteUpDownMap(0, Integer.MAX_VALUE);
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
					s.addsiteUpDownMap(0, Integer.MAX_VALUE);
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
	public void cleanUpForTransaction(Transaction transaction, boolean ended, String reason)
	{
		// Add print reason for abort or end
		
		// Release locks assuming all entries exist in variableLockMap
		int currId = transaction.getId();
		Iterator<Map.Entry<String,Pair>> iterator = variableLockMap.entrySet().iterator();
		Set<Pair> toRemove = new HashSet<>();
		if(!ended)
		{
			transactions.remove(currId);
		}
		while(iterator.hasNext())
		{
			Map.Entry<String, Pair> info = iterator.next();
			if(info.getValue().getTransactionId()==currId)
			{
				iterator.remove();
			}
			
		}
		
		//Remove from LockTable on sites
		for(Site s : sites)
		{
			if(availableSites.contains(s.getId())) {
				LockTable lockTable = s.getLockTable();
				Iterator<Map.Entry<String,List<Pair>>> siteiterator = s.getLockTable().entrySet().iterator();
				while(siteiterator.hasNext())
				{
					Map.Entry<String, List<Pair>> item = siteiterator.next();
					List<Pair> pairsInMap = item.getValue();
					List<Pair> pairsToRemove = new ArrayList<>();
					//System.out.println("Site-"+s.getId()+" "+item.getKey());
					for(Pair p : pairsInMap)
					{
						if(p.getTransactionId()==transaction.getId())
						{
							String variable = item.getKey();
							if(tempWrite.contains(variable))
							{
								if(tempWrite.get(variable).containsKey(transaction.getId()))
								{
									tempWrite.remove(variable);
								}
							}
							pairsToRemove.add(p);
						}
					}
					pairsInMap.removeAll(pairsToRemove);
					
						//System.out.println(pairsToRemove.get(0).getTransactionId()+" is removed");
					
					
					s.getLockTable().put(item.getKey(),pairsInMap);
				}
			}
		}
			
		while(waitQueue.contains(transaction))
		{
			waitQueue.remove(transaction);
		}
		
		if(!ended && transVarMap.containsKey(transaction.getId()))
		{
			transVarMap.remove(transaction.getId());
		}

	}

	public boolean conflictsWithWaitQueueForRead(Queue<Transaction> queue, Transaction curr, boolean isFirst)
	{
		// if curr is a Read transaction
		for(Transaction transaction : queue)
		{
			if(transaction.getType().equals("W"))
			{
				if(!transaction.getVariable().equals(curr.getVariable()))
					continue;

				String varBeingOperated = curr.getVariable();
				List<Site> sites = dataSitesMap.getOrDefault(varBeingOperated,new ArrayList<Site>());
				int currId = curr.getId();
				for(Site dataSite : sites)
				{
					// D- What to do when not available
					if(availableSites.contains(dataSite.getId()))
					{
						LockTable locktable = dataSite.getLockTable();
						try {
							List<Pair> tranLockPairs = locktable.getTransactionsThatHoldLock(varBeingOperated);

							for(Pair p : tranLockPairs)
							{
								if(currId == p.getTransactionId())
								{
									return false;
								}

							}
						}
						catch(Exception e)
						{
							System.out.println(e);
						}
					}
				}
				if(isFirst)
				{
					boolean printSiteDown = false;
					//List<Site> sites = dataSiteMap.getOrDeFault(varBeingOperated,new List<Sites>());
					if(sites.size()==1) {
						Site site = sites.get(0);
						if(!availableSites.contains(site.getId())) {
							System.out.print("Site "+ site.getId() + " is down. ");
							printSiteDown = true;
						}
					}
					
					System.out.print("Transaction " + currId + " is being added to the wait queue");
					if(printSiteDown)
						System.out.println(" because site is down.");
					else
						System.out.println(" because of lock conflict");
				}
				deadlockManager.graph.addEdge(currId, transaction.getId());
				waitQueue.add(curr);
                return true;
			}
			

		}
		return false;
	}
	
	public boolean conflictsWithWaitQueueForWrite(Queue<Transaction> queue, Transaction curr, boolean isFirst)
	{
		for(Transaction transaction : queue) {
			if(transaction.getType().equals("W"))
			{
				//Check if all sites for that variable are down
				boolean allDown = true;
				if(!transaction.getVariable().equals(curr.getVariable()))
					continue;
				
				String varBeingOperated = curr.getVariable();
				List<Site> sites = dataSitesMap.getOrDefault(varBeingOperated,new ArrayList<Site>());
				int currId = curr.getId();
				
				for(Site dataSite : sites)
				{
					if(availableSites.contains(dataSite.getId()))
					{
						//At least 1 is up
						allDown = false;
						//Assuming I got all locks by all transactions on the variable
						List<Pair> locks;
						try {
							locks = dataSite.getLockTable().getTransactionsThatHoldLock(varBeingOperated);
							for(Pair tranLock : locks)
							{
								int id = tranLock.getTransactionId();
								LockType lock = tranLock.getLockType();
								if(currId==id && lock==LockType.WriteLock)
									return false;
								// There is a conflict
								if(isFirst)
								{
									System.out.println("Transaction " +currId + " is being added to the wait queue because of lock conflict");
								}
								deadlockManager.graph.addEdge(currId, transaction.getId());
								waitQueue.add(curr);
	                            return true;
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(allDown)
						{
							if(isFirst)
							{
								boolean printSiteDown = false;
								//List<Site> sites = dataSitesMap.getOrDeFault(varBeingOperated,new List<Sites>());
								if(sites.size()==1) {
									Site site = sites.get(0);
									if(!availableSites.contains(site)) {
										System.out.print("Site "+ site.getId() + " is down. ");
										printSiteDown = true;
									}
								}
								
								System.out.print("Transaction " + curr.getId() + " is being added to the wait queue");
								if(printSiteDown)
									System.out.println(" because site is down.");
								else
									System.out.println(" because of lock conflict");
							}
							
							waitQueue.add(curr);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public void resolveWaitQueue() throws Exception
	{
		Queue<Transaction> checkQueue = new LinkedList<>();
		while(!waitQueue.isEmpty())
		{
			int size = waitQueue.size();
			for(int i=0;i<size;i++)
			{
				Transaction transaction = waitQueue.peek();
				waitQueue.poll();
				if(transaction.getType().equals("RO"))
				{
					handleReadOnlyRequest(transaction, true);
				}
				else if(transaction.getType().equals("R"))
				{
					if(!conflictsWithWaitQueueForRead(checkQueue,transaction,false))
					{
						checkQueue.add(transaction);
						handleReadRequest(transaction, true);
					}
				} 
				else if(transaction.getType().equals("W"))
				{
					if(!conflictsWithWaitQueueForWrite(checkQueue,transaction,false))
					{
						checkQueue.add(transaction);
						handleWriteRequest(transaction,transaction.getValue(), true);
					}
				}
			}
			
			if(size==waitQueue.size())
				break;
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
					cleanUpForTransaction(victim,true,"");
				}
				//printWaitQueue();
				resolveWaitQueue();
				//printWaitQueue();
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
					Transaction transaction = null;
					for(Transaction trans : transactions) {
						if(trans.getId() == transactionId) {
							transaction = trans;
							break;
						}
					}
					if(transaction == null) {
						System.out.println("Can not end transaction as it does not exist");
					}
					handleEndRequest(transaction, time);

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
					for(Site s: sites) {
						s.print();
						System.out.println("");
					}
				}
				else if(line.startsWith("R")) {
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
					if((transaction.getType().equals("R"))) {
						handleReadRequest(transaction, false);
					}

					if((transaction.getType().equals("W"))) {
						transaction.setType("BOTH");
						handleReadRequest(transaction, false);

					}
					else if(transaction.getType().equals("RO")) {
						handleReadOnlyRequest(transaction, false);
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
					if(transaction.getType() == null) {
						transaction.setType("W");
					}
					else if(transaction.getType().equals("W")) {
						transaction.setType("W");
					}
					else if(transaction.getType().equals("R")) {
						transaction.setType("BOTH");
					}
					transaction.setVariable(variable);
					transaction.setValue(value);
					handleWriteRequest(transaction, value, false);
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

		System.out.println("Site "+siteId+" recovered");
		TreeMap<Integer, Integer> treeMap = site.getsiteUpDownMap();
        treeMap.put(time, Integer.MAX_VALUE);
        site.setsiteUpDownMap(treeMap);
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
		if(availableSites.contains(siteId)) {
			availableSites.remove(site.getId());
		}
		failedSites.add(site.getId());
		site.getLockTable().resetLockTable();
		site.resetLastCommittedTime();
		List<Data> toRemove = new ArrayList<Data>();
		for(Data data: site.getData()) {
			site.setStaleData(data);
			toRemove.add(data);
		}
		site.getData().removeAll(toRemove);
		// tid -> list of varNames
		Map<Integer, List<String>> map = site.getTransactionVarMap();
		Set<Integer> transactionsToAbort = map.keySet();
		for(int t: transactionsToAbort)
			affectedTransaction.add(t);
		System.out.println("Site failed "+siteId);

	}

	private void handleWriteRequest(Transaction transaction, int value, boolean alreadyRead) throws Exception {
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
			// Todo: if same transaction has write lock
			if(availableSites.contains(siteId) &&  s.getLockTable().isLocked(variable)) {
				List<Pair> lockTransactions = s.getLockTable().get(variable);
				for(Pair p : lockTransactions)
				{
					if(p.getTransactionId()!=transaction.getId())
					{
						areAllSitesAvailable = false;
					}
				}
				for(Pair p : s.getLockTable().getTransactionsThatHoldLock(variable)) {
					if(transactionsThatHoldLock.contains(p))
						continue;
					else
						transactionsThatHoldLock.add(p);
				}
			}
		}
		if(!areAllSitesAvailable) {
			if(!alreadyRead) {
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
			
		}
		if(areAllSitesAvailable)
			startWriteAction(transaction, usedSites, variable, value);
	}

	private void startWriteAction(Transaction transaction, List<Site> usedSites, String variable, int value) throws Exception {
		int transactionId = transaction.getId();
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
			// Check if it is a stale data then move to regular
			if(data == null) {
				//must be regular data
				List<Data> regularData = s.getData();
				for(Data d: regularData) {
					if(d.getVarName().equals(variable)) {
						data = d;
						break;
					}
					
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
		if(transVarMap.containsKey(tId)) {
			boolean alreadyPresnt = false;
			for(VariableValue v : transVarMap.get(tId)) {
				if(v.getVarName().equals(varName))
					alreadyPresnt = true;
			}
			if(!alreadyPresnt) {
				VariableValue var = new VariableValue(varName, value);
				transVarMap.get(tId).add(var);
			}
			
		}
		else if(!transVarMap.containsKey(tId)) {
			List<VariableValue> l = new ArrayList<>();
			VariableValue v = new VariableValue(varName, value);
			l.add(v);
			transVarMap.put(tId, l);
		}
		/*List<VariableValue> vals = transVarMap.get(tId);
		for(VariableValue val: vals)
		{
			System.out.println(val.getVarName()+" "+val.getValue());
		}*/
	}

	private void handleReadOnlyRequest(Transaction transaction, boolean alreadyRead) throws Exception {
		int transactionId = transaction.getId();
		String variable = transaction.getVariable();
		int variableID = Integer.parseInt(variable.substring(1));
		boolean isAlreadyLocked = false;
		boolean foundData = false;
		List<Site> usedSites = dataSitesMap.get(variable);
		boolean isStale = false;
		int numOfAvailableSites = 0;

		/*for(Site s: usedSites) {
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
		}*/
		boolean canRead = false;
		boolean allDown = true;
		boolean allNotStale = true;
		Data dataToRead = null;
		for(Site s : usedSites)
		{
			if(availableSites.contains(s.getId()))
			{
				allDown = false;
				List<Data> staleData = s.getStaleData();
				for(Data stale : staleData)
				{
					if(stale.getVarName().equals(variable))
					{
						allNotStale = false;
						break;
					}
				}
				if(s.canReadOnlyProceed(variable, transaction))
				{
					canRead = true;
					int val = Integer.MIN_VALUE;
					for(Data d : s.getData())
					{
						if(d.getVarName().equals(variable))
						{
							dataToRead = d;
						}
					}
					// System.out.println("Transaction-" +transaction.getId()+" read " +variable+" from Site "+s.getId()+": "+val);
				}
			}
		}

		if(dataToRead != null) {
			VariableValue varVal = new VariableValue(dataToRead.getVarName(), dataToRead.getValue());
			transaction.setReadVar(varVal);
		}
		// Find why it failed
		// Case 1: Some transaction was alreading holding a lock on the variable
		/*if(!foundData) {
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
			System.out.println("Transaction "+transactionId+" could not read the variable since all sites contain stale data");*/
		 if(!canRead)
		 {
			 waitQueue.add(transaction);
			 System.out.print("Transaction " + transaction.getId() + " is being added to the wait queue");
             if(allDown)
                 System.out.println(" because site is down.");
             else if(!allNotStale)
                 System.out.println(" because of stale data.");
             else
                 System.out.println(" because of lock conflict");
		 }
		   
		}
	

	private void handleReadRequest(Transaction transaction, boolean alreadyRead) throws Exception {
		System.out.println("Read Trans:" +transaction);
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
						s.putTransactionVarMap(transactionId, variable);
						String varName = variable;
						int varValue = dataToRead.getValue();
						VariableValue varVal = new VariableValue(varName, varValue);
						transaction.setReadVar(varVal);
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
			if(!alreadyRead) {
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
					waitQueue.add(transaction);	

					}
				//Case 3: All sites have staleData
					else if(isStale) {
					System.out.println("Transaction "+transactionId+" could NOT read the variable since all sites contain stale data");
					waitQueue.add(transaction);	

				}
			}
			
	}
}


private void handleEndRequest(Transaction transaction, int endTime) {
	int transactionId = transaction.getId();
	boolean isRead = false;
	boolean bothReadDone = false;
	Iterator<Transaction> it = transactions.iterator();

    boolean aborted = false;
	for(int t: affectedTransaction) {
		if(transactionId == t) {
			System.out.println("Transaction "+transactionId+" aborted since it accessed a failed site.");
			aborted = true;
		}
	}

	if(!aborted){
		if((transaction.getType().equals("R") || transaction.getType().equals("RO"))&&!Objects.isNull(transaction.getReadVar()) ) {
			System.out.println("Transaction "+transactionId+ " read variable "+transaction.getReadVar().getVarName()+":"+transaction.getReadVar().getValue());
			System.out.println("Transaction "+transactionId+" commited");
			isRead = true;
		}

		if(transaction.getType().equals("BOTH") && !Objects.isNull(transaction.getReadVar())) {
			System.out.println("Transaction "+transactionId+ " read variable "+transaction.getReadVar().getVarName()+":"+transaction.getReadVar().getValue());
			bothReadDone = true;
		}

		if(!isRead || bothReadDone) {
			List<VariableValue> varsChanged = transVarMap.getOrDefault(transactionId, new ArrayList<>());
			for(VariableValue v : varsChanged) {
				String varName = v.getVarName();
				int varId = Integer.parseInt(varName.substring(1));
				List<Integer> newValues = tempWrite.get(varName).get(transactionId);
				for(Site s: sites) {
					int siteId = s.getId();
					if(newValues.get(siteId) != null) {
						int newValue = newValues.get(siteId);

						if(s.isPresent(varName) && s.getsiteUpDownMap().lastKey() < transaction.getTime()) {
							s.setValue(varName, newValue);
							s.setLastCommittedTime(endTime, varId);
							System.out.println("Transaction "+transactionId+" updated variable "+varName+" to "+newValue+" at site"+ siteId);
						}

						else if(!s.isPresent(varName) && !failedSites.contains(s.getId()) && s.getsiteUpDownMap().lastKey() < transaction.getTime()) {
							Data d = new Data(varName, newValue);
							s.setData(d);
							s.setLastCommittedTime(endTime, varId);
							System.out.println("Transaction "+transactionId+" updated variable "+varName+" to "+newValue+" at site"+ siteId);
						}
						// List<Data> siteData = s.getData();
						// for(Data d: siteData) {
						// 	if(d.getVarName().equals(varName)) {
						// 		d.setValue(newValue);
						// 		s.setLastCommittedTime(endTime, varId);
						// 		System.out.println("Transaction "+transactionId+" updated variable "+varName+" to "+newValue+" at site"+ siteId);
						// 	}
						// }
					}
				}
			}
			System.out.println("Transaction "+transactionId+" commited");

		}
	}
	cleanUpForTransaction(transaction,true, "");
}
}