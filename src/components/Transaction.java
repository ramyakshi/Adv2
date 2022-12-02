package components;

public class Transaction {
    int id;
    int time;
    String type; 
    String variable;
    boolean isActive;
    public Transaction(int id, int time, String type) {
        this.id = id;
        this.time = time;
        this.type = type;
        this.isActive = true;
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

}
