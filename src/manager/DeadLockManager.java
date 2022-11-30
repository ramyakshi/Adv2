package manager;

import java.util.List;

import components.Graph;
import components.Transaction;

public class DeadLockManager {
	
	
	Graph graph;
	
	public DeadLockManager() {
		this.graph = new Graph();
	}

	public boolean isDeadlockPresent() {
		return this.graph.isCyclic();
	}

	public Transaction removeYoungestDeadlock(List<Transaction> transactions) {
		List<Integer> cyclicNodes = this.graph.getCyclicNodes();
		Transaction youngest = null;
		int latest = Integer.MIN_VALUE;

		for(Transaction t : transactions) {
			for(int i : cyclicNodes) {
				if(t.getId() == i && t.getTime() > latest) {
					latest = t.getTime();
					youngest = t;
				}
			}
		}
		this.graph.removeEdge(youngest.getId());
		return youngest;
	}

}
