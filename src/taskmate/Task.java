package taskmate;

public class Task {
    private String name;
    private int priority; // 1 = High, 2 = Medium, 3 = Low

    public Task(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public String getName() { return name; }
    public int getPriority() { return priority; }

    public void setName(String name) { this.name = name; }
    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return name + " (" + (priority==1 ? "High" : priority==2 ? "Medium" : "Low") + ")";
    }
}
