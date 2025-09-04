package model;

public class Equipment {
    private int id;
    private String name;
    private String serialNumber;
    private String description;
    private String location;

    public Equipment(int id, String name, String serialNumber, String description, String location) {
        this.id = id;
        this.name = name;
        this.serialNumber = serialNumber;
        this.description = description;
        this.location = location;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSerialNumber() { return serialNumber; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
}
