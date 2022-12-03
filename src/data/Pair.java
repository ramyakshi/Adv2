/**
 * @author Shraddha Iyer
 * @version 1.0.0
 * @date 11/19/2021
 */
package data;

import components.LockType;


public class Pair {
	int transactionId;
	LockType lockType;

	public Pair(int transactionId, LockType lockType) {
		this.transactionId = transactionId;
		this.lockType = lockType;
	}

	public int getTransactionId() {
		return this.transactionId;
	}

	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	public LockType getLockType() {
		return this.lockType;
	}

	public void setLockType(LockType lockType) {
		this.lockType = lockType;
	}

	@Override
	public String toString() {
		return "{" +
			" transactionId='" + getTransactionId() + "'" +
			", lockType='" + getLockType() + "'" +
			"}";
	}

}