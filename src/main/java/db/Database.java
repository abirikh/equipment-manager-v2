package db;

import java.sql.*;
import model.Equipment;
import java.util.ArrayList;
import java.util.List;


public class Database {
    private final String url = "jdbc:sqlite:equipment.db"; // база создастся в корне проекта

    public Database() {
        // Создание таблицы, если её ещё нет
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS equipment (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "serial_number TEXT," +
                    "description TEXT," +
                    "location TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Удаление по id
    public void deleteEquipment(int id) {
        String sql = "DELETE FROM equipment WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Оборудование удалено: " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обновление записи
    public void updateEquipment(int id, String name, String serial, String description, String location) {
        String sql = "UPDATE equipment SET name=?, serial_number=?, description=?, location=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, serial);
            pstmt.setString(3, description);
            pstmt.setString(4, location);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
            System.out.println("Оборудование обновлено: " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Добавление оборудования
    public void insertEquipment(String name, String serial, String description, String location) {
        String sql = "INSERT INTO equipment (name, serial_number, description, location) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, serial);
            pstmt.setString(3, description);
            pstmt.setString(4, location);
            pstmt.executeUpdate();
            System.out.println("Оборудование добавлено: " + name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Получение всех записей
    public List<Equipment> getAllEquipment() {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM equipment";
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Equipment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("serial_number"),
                        rs.getString("description"),
                        rs.getString("location")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Чтение одной записи по id
    public String getEquipmentInfo(int id) {
        String sql = "SELECT * FROM equipment WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "Оборудование: " + rs.getString("name") +
                        "\nСерийный номер: " + rs.getString("serial_number") +
                        "\nОписание: " + rs.getString("description") +
                        "\nРасположение: " + rs.getString("location");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Оборудование не найдено.";
    }
}
