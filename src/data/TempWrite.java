package data;
import java.util.*;

// key -> variableName
// value -> Integer -> transactionId
// List -> all values that were updated by the transaction
public class TempWrite extends HashMap<String, HashMap<Integer, List<Integer>>> {
	
	public TempWrite() {
		
	}
	
	public boolean contains(String variable) {
		return super.containsKey(variable);
	}
	
	public void initializeEntry(String variable, int tId) {
		List<Integer> siteValues = new ArrayList<>();
		for(int i=0;i<11;i++)
		{
			siteValues.add(null);
		}
		/*Integer[] siteValues = new Integer[11];
		Arrays.fill(siteValues, null);
		HashMap<Integer, Integer[]> value = new HashMap<>();
		value.put(tId, siteValues);
		super.put(variable, value);*/
		HashMap<Integer, List<Integer>> value = new HashMap<>();
		value.put(tId, siteValues);
		super.put(variable, value);
	}

	public HashMap<Integer, List<Integer>> getEntry(String variable) {
		return super.get(variable);
	}
	
	public void addNewValue(String variable, int tId, Integer newValue, int siteId) {
		/*Integer [] arr = super.get(variable).get(tId);
		arr[siteId] = newValue;
		super.get(variable).put(tId, arr);*/
		List<Integer> arr = super.get(variable).getOrDefault(tId, new ArrayList<>());
		arr.set(siteId, newValue);
		super.get(variable).put(tId, arr);
	}

	public void editValue(String variable, int tId, int siteId) {
		List<Integer> arr = super.get(variable).getOrDefault(tId, new ArrayList<>());
		arr.set(siteId, null);
		super.get(variable).put(tId, arr);
	}
	
}
