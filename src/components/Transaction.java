/**
 * @author Shraddha Iyer
 * @version 1.0.0
 * @date 11/19/2021
 */

package components;

import data.VariableValue;

public class Transaction {
    int id;
    int time;
    String type; 
    String variable;
    boolean isActive;
    int value;
    VariableValue readVar;
    String currAction;
    public String getCurrAction() {
		return currAction;
	}

	public void setCurrAction(String currAction) {
		this.currAction = currAction;
	}

	public Transaction(int id, int time, String type) {
        this.id = id;
        this.time = time;
        this.type = type;
        this.isActive = true;
        this.value = Integer.MAX_VALUE;        
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTime() {
        return this.time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVariable() {
        return this.variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }
    
    public int getValue()
    {
    	return this.value;
    }
    
    public void setValue(int value)
    {
    	this.value = value;
    }

    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", time='" + getTime() + "'" +
            ", type='" + getType() + "'" +
            ", variable='" + getVariable() + "'" +
            ", value='" + getValue() + "'" +
            "}";
    }

}
