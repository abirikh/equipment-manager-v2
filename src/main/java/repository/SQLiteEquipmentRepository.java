package repository;

import model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteEquipmentRepository implements EquipmentRepository {
    private final String url = "jdbc:sqlite:equipment.db";

    public SQLiteEquipmentRepository() {
        // создаём таблицу при первом запуске
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    serialNumber TEXT,
                    description TEXT,
                    location TEXT
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Equipment> getAllEquipment() {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM equipment";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Equipment eq = new Equipment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("serialNumber"),
                        rs.getString("description"),
                        rs.getString("location")
                );
                list.add(eq);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Equipment getEquipmentById(int id) {
        String sql = "SELECT * FROM equipment WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Equipment(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("serialNumber"),
                        rs.getString("description"),
                        rs.getString("location")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void insertEquipment(Equipment equipment) {
        String sql = "INSERT INTO equipment(name, serialNumber, description, location) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, equipment.getName());
            pstmt.setString(2, equipment.getSerialNumber());
            pstmt.setString(3, equipment.getDescription());
            pstmt.setString(4, equipment.getLocation());
            pstmt.executeUpdate();

            // получаем ID, который сгенерировала SQLite
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                equipment.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateEquipment(Equipment equipment) {
        String sql = "UPDATE equipment SET name = ?, serialNumber = ?, description = ?, location = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, equipment.getName());
            pstmt.setString(2, equipment.getSerialNumber());
            pstmt.setString(3, equipment.getDescription());
            pstmt.setString(4, equipment.getLocation());
            pstmt.setInt(5, equipment.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEquipment(int id) {
        String sql = "DELETE FROM equipment WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Equipment> searchEquipment(String query) {
        List<Equipment> list = new ArrayList<>();
        String sql = "SELECT * FROM equipment WHERE " +
                "LOWER(name) LIKE ? OR LOWER(serialNumber) LIKE ? OR LOWER(description) LIKE ? OR LOWER(location) LIKE ? " +
                "ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String filter = "%" + (query == null ? "" : query.toLowerCase()) + "%";
            pstmt.setString(1, filter);
            pstmt.setString(2, filter);
            pstmt.setString(3, filter);
            pstmt.setString(4, filter);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Equipment(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("serialNumber"),
                            rs.getString("description"),
                            rs.getString("location")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void updateDescription(int id, String newDescription) {
        String sql = "UPDATE equipment SET description = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDescription);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateLocation(int id, String newLocation) {
        String sql = "UPDATE equipment SET location = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newLocation);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
