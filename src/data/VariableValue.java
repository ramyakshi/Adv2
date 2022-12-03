/**
 * @author Shraddha Iyer
 * @version 1.0.0
 * @date 11/19/2021
 */
package data;

public class VariableValue {

    String varName;
    Integer value;

    public VariableValue(String varName, int value) {
        this.varName = varName;
        this.value = value;
    }

    public String getVarName() {
        return this.varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }
    
}
