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

    public Equipment(String name, String serialNumber, String description, String location) {
        this(-1, name, serialNumber, description, location);
    }

    public int getId() {
        return id;
    }

    // ⚡ Добавляем setId — пригодится, когда СУБД возвращает новый ID
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }
}
