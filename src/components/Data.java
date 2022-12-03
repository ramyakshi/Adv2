/**
 * @author Shraddha Iyer
 * @version 1.0.0
 * @date 11/19/2021
 */
package components;

import java.util.ArrayList;

public class Data {
    String varName;
    int value;


    
    public Data(String varName, int value) {
        this.varName = varName;
        this.value = value;
    }

    public String getVarName() {
        return this.varName;
    }

    public int getValue() {
        return this.value;
    }
    public void setValue(int v) {
        this.value = v;
    }

    @Override
    public String toString() {
        return "{" +
            " varName='" + getVarName() + "'" +
            ", value='" + getValue() + "'" +
            "}";
    }  
    
}

