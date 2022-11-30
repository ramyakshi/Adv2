package components;

import java.util.*;

public class Graph {

	List<List<Integer>> adj;
	List<Integer> cyclicNodes;
	// Assuming max number of transactions
	private final int v = 1000000;

	public Graph() {
		this.adj = new ArrayList<>();
		this.cyclicNodes = new ArrayList<>();
		for(int i = 0; i< v; i++) {
			this.adj.add(new ArrayList<>());
		}
	}

	public boolean isCyclicUtil(int i, boolean[] visited,
			boolean[] recStack)
	{
		if (recStack[i])
			return true;

		if (visited[i])
			return false;

		visited[i] = true;

		recStack[i] = true;
		List<Integer> children = adj.get(i);

		for (Integer c: children)
			if (isCyclicUtil(c, visited, recStack)) {
				cyclicNodes.add(c);
				return true;
			}

		recStack[i] = false;

		return false;
	}
	public void addEdge(int source, int dest) {
		adj.get(source).add(dest);
	}

	public void removeEdge(int source) {
		adj.get(source).clear();
        for (int i = 0; i < adj.size(); i++) {
            adj.get(i).removeAll(Collections.singleton(source));
        }
    }
	

	public boolean isCyclic(){
		boolean[] visited = new boolean[v];
		boolean[] recStack = new boolean[v];
		for (int i = 0; i < v; i++)
			if (isCyclicUtil(i, visited, recStack))
				return true;
		return false;
	}

	public List<Integer> getCyclicNodes() {
		return this.cyclicNodes;
	}

}
