package manager;

import java.io.*;
import java.util.*;
import components.Data;
import components.LockType;
import components.Site;
import components.Transaction;
import data.Pair;

public class TransactionManager {
	List<Site> sites;
	List<Site> availableSites;
	List<Site> failedSites;
	Map<String, List<Site>> dataSitesMap;
	List<Transaction> transactions;
	// key -> variableName Value-> String = {Transaction, LockType}
	Map<String, Pair> variableLockMap;
	Queue<Transaction> waitQueue;
	DeadLockManager deadlockManager;
	int time = 0;

	public TransactionManager() {
		this.transactions = new ArrayList<Transaction>();
		this.sites = new ArrayList<>();
		this.dataSitesMap = new HashMap<>();
		this.variableLockMap = new HashMap<>();
		this.waitQueue = new LinkedList<>();
		this.deadlockManager = new DeadLockManager();
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
				s.getLockTable().setLockType(data, LockType.UnLocked);
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
					// Added data to the site's locktable and set it to Unlocked
					s.getLockTable().setLockType(data, LockType.UnLocked);
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
				if(deadlockManager.isDeadlockPresent()) {
					Transaction victim = deadlockManager.removeYoungestDeadlock(transactions);
					System.out.println("Aborting youngest transacation due to deadlock. Transaction Id: "+victim.getId());
				}
				time++;
				if(line.startsWith("beginRO")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					Transaction transaction = new Transaction(transactionId, time, "RO");
					System.out.println("Begin RO transaction "+ transactionId);
					transactions.add(transaction);
				}
				else if(line.startsWith("begin")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					Transaction transaction = new Transaction(transactionId, time, null);
					System.out.println("Begin transaction "+ transactionId);
					transactions.add(transaction);


				}
				else if(line.startsWith("end")) {
					int transactionId = Integer.parseInt(line.substring(line.indexOf('(') + 2, line.indexOf(')')).trim());
					System.out.println("End: "+ transactionId);
					System.out.println(variableLockMap);
					for(Map.Entry<String,Pair> entry : variableLockMap.entrySet()) {
						Pair p = entry.getValue();
						if(p.getTransactionId() == transactionId) {
							//Todo: Should we remove all entries of transaction from variableLockMap?
						}
					}
					System.out.println("End transaction "+ transactionId);

				}
				else if(line.startsWith("fail")) {
					System.out.println("Fail transaction " +line);

				}
				else if(line.startsWith("recover")) {
					System.out.println("Recover transaction " + line );

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
					System.out.println(fields);
					String tId = fields.split(",")[0].trim();
					int transactionId = Integer.parseInt(tId.substring(1));
					String variable = fields.split(",")[1].trim();
					for(Transaction trans: transactions) {
						if(trans.getId() == transactionId)
							transaction = trans;
					}

					transaction.setType("W");
					transaction.setVariable(variable);

					handleWriteRequest(transaction);

				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleWriteRequest(Transaction transaction) {
		/**
		 * Before allowing a request to proceed. Things I need to do:
		 * Check if the variable is locked -> add the transaction to the wait queue and create a link from transaction -> prevTransaction
		 * VraibleLockMap -> To check which transaction has a lock on which variable
		 */		
		int transactionId = transaction.getId();
		String variable = transaction.getVariable();
		
		// No transaction holds lock on the variable
		if(!variableLockMap.containsKey(variable)) {
			Pair pair = new Pair(transactionId, LockType.WriteLock);
			variableLockMap.put(variable, pair);
			startWriteAction(transaction);
		}
		// If the transaction holds a write lock on the variable
		else if(variableLockMap.containsKey(variable) && 
			(variableLockMap.get(variable).getTransactionId() == transactionId) && (variableLockMap.get(variable).getLockType().equals(LockType.WriteLock))) {
			startWriteAction(transaction);
		}
		else {
			Pair pair = variableLockMap.get(variable);
			int prevTransactionId = pair.getTransactionId();
			deadlockManager.graph.addEdge(transactionId, prevTransactionId);
			waitQueue.add(transaction);	
		}
	}

	private void startWriteAction(Transaction transaction) {
		// TODO Auto-generated method stub
		
	}

	private void handleReadOnlyRequest(Transaction transaction) {
	}

	private void handleReadRequest(Transaction transaction) {

		int transactionId = transaction.getId();
		String variable = transaction.getVariable();
		
		// No transaction holds lock on the variable
		if(!variableLockMap.containsKey(variable)) {
			Pair pair = new Pair(transactionId, LockType.ReadLock);
			variableLockMap.put(variable, pair);
			startWriteAction(transaction);
		}
		// If the transaction holds a write lock on the variable or a read lock
		else if(variableLockMap.containsKey(variable) && 
			(variableLockMap.get(variable).getTransactionId() == transactionId) && 
			((variableLockMap.get(variable).getLockType().equals(LockType.WriteLock)) || (variableLockMap.get(variable).getLockType().equals(LockType.ReadLock)))) {
			startWriteAction(transaction);
		}
		else {
			Pair pair = variableLockMap.get(variable);
			int prevTransactionId = pair.getTransactionId();
			deadlockManager.graph.addEdge(transactionId, prevTransactionId);
			waitQueue.add(transaction);	
		}
	}
}
