package app;

import bot.EquipmentBot;
import model.Equipment;
import repository.EquipmentRepository;
import repository.SQLiteEquipmentRepository;

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
    private final EquipmentRepository repository = new SQLiteEquipmentRepository();
    private ObservableList<Equipment> equipmentData;

    @Override
    public void start(Stage stage) {
        // --- Поля ввода ---
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

        // --- Поиск ---
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

        // Автозаполнение полей при выборе строки
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                nameField.setText(newSel.getName());
                serialField.setText(newSel.getSerialNumber());
                descriptionField.setText(newSel.getDescription());
                locationField.setText(newSel.getLocation());
            }
        });

        // --- Данные ---
        equipmentData = FXCollections.observableArrayList(repository.getAllEquipment());

        // --- Поиск и фильтрация ---
        FilteredList<Equipment> filteredData = new FilteredList<>(equipmentData, p -> true);
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

        SortedList<Equipment> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // ✅ Запускаем бота
        new Thread(() -> {
            try {
                String token = "---";
                String username = "RusenegroEquipmentBot";
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(new EquipmentBot(token, username));
                System.out.println("✅ Бот запущен вместе с приложением");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // --- Обработчики кнопок ---
        addButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String serial = serialField.getText().trim();
            String desc = descriptionField.getText().trim();
            String loc = locationField.getText().trim();

            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Введите название оборудования!").showAndWait();
                return;
            }

            Equipment eq = new Equipment(name, serial, desc, loc);
            repository.insertEquipment(eq);
            equipmentData.setAll(repository.getAllEquipment());

            nameField.clear();
            serialField.clear();
            descriptionField.clear();
            locationField.clear();
        });

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

            Equipment updated = new Equipment(selected.getId(), name, serial, desc, loc);
            repository.updateEquipment(updated);
            equipmentData.setAll(repository.getAllEquipment());

            nameField.clear();
            serialField.clear();
            descriptionField.clear();
            locationField.clear();
        });

        deleteButton.setOnAction(e -> {
            Equipment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Выберите запись для удаления!").showAndWait();
                return;
            }

            repository.deleteEquipment(selected.getId());
            equipmentData.setAll(repository.getAllEquipment());
        });

        qrButton.setOnAction(e -> {
            Equipment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.WARNING, "Выберите запись для QR!").showAndWait();
                return;
            }

            String qrText = "ID:" + selected.getId() +
                    "\nНазвание: " + selected.getName() +
                    "\nСерийный номер: " + selected.getSerialNumber() +
                    "\nОписание: " + selected.getDescription() +
                    "\nРасположение: " + selected.getLocation();

            qr.QrGenerator.generateQr(qrText, "qr_" + selected.getId());
            new Alert(Alert.AlertType.INFORMATION, "QR-код сгенерирован: qr_" + selected.getId() + ".png").showAndWait();
        });

        // --- UI ---
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
