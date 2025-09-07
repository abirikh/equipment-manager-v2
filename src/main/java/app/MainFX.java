package app;

import bot.EquipmentBot;
import db.Database;
import model.Equipment;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class MainFX extends Application {
    private final Database database = new Database();
    private ObservableList<Equipment> equipmentData;

    @Override
    public void start(Stage stage) {
        // --- Поля ввода для добавления ---
        TextField nameField = new TextField();
        nameField.setPromptText("Название");

        TextField serialField = new TextField();
        serialField.setPromptText("Серийный номер");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Описание");

        TextField locationField = new TextField();
        locationField.setPromptText("Расположение");

        Button addButton = new Button("Добавить");
        Button editButton = new Button("Редактировать");
        Button deleteButton = new Button("Удалить");
        Button qrButton = new Button("Сгенерировать QR");

        HBox formBox = new HBox(8, nameField, serialField, descriptionField, locationField, addButton, editButton, deleteButton, qrButton);
        formBox.setStyle("-fx-padding: 8;");



        // --- Поле поиска ---
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск (название / серийный / описание / расположение)");
        Button clearSearch = new Button("×");
        clearSearch.setOnAction(e -> searchField.clear());
        HBox searchBox = new HBox(6, new Label("Поиск:"), searchField, clearSearch);
        searchBox.setStyle("-fx-padding: 6 8 6 8; -fx-alignment: center-left;");

        // --- Таблица ---
        TableView<Equipment> table = new TableView<>();

        TableColumn<Equipment, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()));
        colId.setPrefWidth(50);

        TableColumn<Equipment, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colName.setPrefWidth(180);

        TableColumn<Equipment, String> colSerial = new TableColumn<>("Серийный номер");
        colSerial.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSerialNumber()));
        colSerial.setPrefWidth(140);

        TableColumn<Equipment, String> colDescription = new TableColumn<>("Описание");
        colDescription.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDescription()));
        colDescription.setPrefWidth(200);

        TableColumn<Equipment, String> colLocation = new TableColumn<>("Расположение");
        colLocation.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocation()));
        colLocation.setPrefWidth(140);

        table.getColumns().addAll(colId, colName, colSerial, colDescription, colLocation);

        // --- Данные ---
        equipmentData = FXCollections.observableArrayList(database.getAllEquipment());

        // FilteredList для поиска
        FilteredList<Equipment> filteredData = new FilteredList<>(equipmentData, p -> true);

        // Листенер для поля поиска (реагирует на каждое изменение)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = (newVal == null) ? "" : newVal.trim().toLowerCase();
            if (filter.isEmpty()) {
                filteredData.setPredicate(e -> true);
            } else {
                filteredData.setPredicate(e -> {
                    if (e.getName() != null && e.getName().toLowerCase().contains(filter)) return true;
                    if (e.getSerialNumber() != null && e.getSerialNumber().toLowerCase().contains(filter)) return true;
                    if (e.getDescription() != null && e.getDescription().toLowerCase().contains(filter)) return true;
                    if (e.getLocation() != null && e.getLocation().toLowerCase().contains(filter)) return true;
                    return false;
                });
            }
        });
        // ✅ Запускаем бота в отдельном потоке
        new Thread(() -> {
            try {
                String token = "8458380504:AAFTHb47N4DdyGtaSblyzGwaJgr_xBobiaU";
                String username = "RusenegroEquipmentBot";
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(new EquipmentBot(token, username));
                System.out.println("✅ Бот запущен вместе с приложением");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        stage.setTitle("База оборудования");
        stage.show();
        // SortedList чтобы сортировка столбцов работала корректно
        SortedList<Equipment> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // --- Обработчик кнопки Добавить ---
        addButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String serial = serialField.getText().trim();
            String desc = descriptionField.getText().trim();
            String loc = locationField.getText().trim();

            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Введите название оборудования!").showAndWait();
                return;
            }

            database.insertEquipment(name, serial, desc, loc);

            // Обновляем список — проще перезаполнить из БД (гарантия согласованности)
            equipmentData.setAll(database.getAllEquipment());

            // Очищаем поля
            nameField.clear();
            serialField.clear();
            descriptionField.clear();
            locationField.clear();
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.getName());
                serialField.setText(newSel.getSerialNumber());
                descriptionField.setText(newSel.getDescription());
                locationField.setText(newSel.getLocation());
            }
        });


        // Обработчик кнопки Редактировать
        editButton.setOnAction(e -> {
            Equipment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Выберите запись для редактирования!").showAndWait();
                return;
            }

            String name = nameField.getText().trim();
            String serial = serialField.getText().trim();
            String desc = descriptionField.getText().trim();
            String loc = locationField.getText().trim();

            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Введите название!").showAndWait();
                return;
            }

            database.updateEquipment(selected.getId(), name, serial, desc, loc);
            equipmentData.setAll(database.getAllEquipment());

            nameField.clear();
            serialField.clear();
            descriptionField.clear();
            locationField.clear();
        });

// Обработчик кнопки Удалить
        deleteButton.setOnAction(e -> {
            Equipment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Выберите запись для удаления!").showAndWait();
                return;
            }

            database.deleteEquipment(selected.getId());
            equipmentData.setAll(database.getAllEquipment());
        });

        qrButton.setOnAction(e -> {
            Equipment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Выберите запись для QR!").showAndWait();
                return;
            }

            // Можно закодировать все данные или только ID
            String qrText = "ID:" + selected.getId() +
                    "\nНазвание: " + selected.getName() +
                    "\nСерийный номер: " + selected.getSerialNumber() +
                    "\nОписание: " + selected.getDescription() +
                    "\nРасположение: " + selected.getLocation();

            qr.QrGenerator.generateQr(qrText, "qr_" + selected.getId());
            new Alert(Alert.AlertType.INFORMATION, "QR-код сгенерирован: qr_" + selected.getId() + ".png").showAndWait();
        });


        // --- Макет ---
        VBox topBox = new VBox(formBox, searchBox);
        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(table);

        stage.setScene(new Scene(root, 900, 500));
        stage.setTitle("Учёт оборудования — поиск/фильтрация");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
