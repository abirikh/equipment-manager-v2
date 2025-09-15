package repository;

import model.Equipment;
import java.util.List;

public interface EquipmentRepository {
    List<Equipment> getAllEquipment();
    Equipment getEquipmentById(int id);
    void insertEquipment(Equipment equipment);
    void updateEquipment(Equipment equipment);
    void deleteEquipment(int id);

    void updateDescription(int id, String newDescription);
    void updateLocation(int id, String newLocation);

    // <-- добавляем
    List<Equipment> searchEquipment(String query);
}
