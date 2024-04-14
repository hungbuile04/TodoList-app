package project.todolist;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import project.todolist.datamodel.TodoData;
import project.todolist.datamodel.TodoItem;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

public class HelloController {
    @FXML
    private ListView<TodoItem> todoListView ;
    @FXML
    private TextArea itemDetailsTextArea;
    @FXML
    private Label deadlineLabel;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ContextMenu listContextMenu;
    @FXML
    private ToggleButton filterToggleButton;

    private FilteredList<TodoItem> filteredList;
    private Predicate<TodoItem> wantAllItems;
    private Predicate<TodoItem> wantTodayItems;

    private List<TodoItem> todoItems;
    public void initialize(){
        // Nhấp chuột phải để hiện chức năng Delete
        listContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                deleteItem(item);
            }
        });
        listContextMenu.getItems().addAll(deleteMenuItem);
        // tạo listener theo dõi sự thay đổi của todoitem
        todoListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TodoItem>() {
            @Override
            public void changed(ObservableValue<? extends TodoItem> observableValue, TodoItem oldvalue, TodoItem newvalue) {
                if(newvalue!= null){
                    //TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                    //itemDetailsTextArea.setText(item.getDetails());
                    itemDetailsTextArea.setText(newvalue.getDetails());
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    deadlineLabel.setText(df.format(newvalue.getDeadline()));
                }
            }
        });
        wantAllItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem todoItem) {
                return true;
            }
        };
        wantTodayItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem todoItem) {
                return (todoItem.getDeadline().equals(LocalDate.now()));
            }
        };
        //Sắp xếp list theo ngày đến hạn
        filteredList = new FilteredList<>(TodoData.getInstance().getTodoItems(),wantAllItems);
        SortedList<TodoItem> sortedList = new SortedList<TodoItem>(filteredList,
                new Comparator<TodoItem>() {
                    @Override
                    public int compare(TodoItem o1, TodoItem o2) {
                        return o1.getDeadline().compareTo(o2.getDeadline());
                    }
                }
        );
        todoListView.setItems(sortedList);
        //todoListView.setItems(TodoData.getInstance().getTodoItems()); //đưa dữ liệu vào
        todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE); //chọn từng cái một
        todoListView.getSelectionModel().selectFirst(); // khi mở chương trình tự động chọn mục đầu tiên
        todoListView.setCellFactory(new Callback<ListView<TodoItem>, ListCell<TodoItem>>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> param) {
                ListCell<TodoItem> cell = new ListCell<>(){
                       @Override
                       protected void updateItem(TodoItem item, boolean empty){
                           super.updateItem(item, empty);
                           if(empty){
                               setText(null);
                           }else{
                               setText(item.getShortDescription());
                               if(item.getDeadline().isBefore(LocalDate.now().plusDays(1))){
                                   setTextFill(Color.RED);
                               }
                           }
                       }
                };
                cell.emptyProperty().addListener(
                        (obs, wasEmpty, isNowEmpty) -> {
                            if(isNowEmpty){
                                cell.setContextMenu(null);
                            } else {
                                cell.setContextMenu(listContextMenu);
                            }
                        }
                );
                return cell;
            }
        });
    }
    // Ấn delete thì hiện alert
    @FXML
    public void handleKeyPressed(KeyEvent event){
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(selectedItem != null){
            if(event.getCode().equals(KeyCode.DELETE)){
                deleteItem(selectedItem);
            }
        }
    }
    // Hiện những Item đến hạn hôm nay
    @FXML
    public void handleFilterButton(){
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(filterToggleButton.isSelected()){
            filteredList.setPredicate(wantTodayItems);
            if(filteredList.isEmpty()){
                itemDetailsTextArea.clear();
                deadlineLabel.setText("");
            }else if(filteredList.contains(selectedItem)){
                todoListView.getSelectionModel().select(selectedItem);
            }else{
                todoListView.getSelectionModel().selectFirst();
            }
        }else{
            filteredList.setPredicate(wantAllItems);
        }
    }

    // tạo cửa sổ dialog để nhập dữ liệu khi ta ấn File -> New
    @FXML
    public void showNewItemDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Add new TodoItem");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml")); //fxmlLoader.setLocation(): Phương thức này được sử dụng để đặt vị trí tệp FXML cho FXMLLoader. Khi bạn gọi phương thức load(), nó sẽ sử dụng vị trí này để tải tệp FXML.
        try{
            dialog.getDialogPane().setContent(fxmlLoader.load());
        }catch(IOException e){
            System.out.println("Couldn't load the dialog");
            e.printStackTrace();
            return;
        }
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK){
            DialogController controller = fxmlLoader.getController();
            TodoItem newItem = controller.processResult();
           // todoListView.getItems().setAll(TodoData.getInstance().getTodoItems()); // Sau khi thêm 1 item mới thì nó hiện luôn lên ListView
            todoListView.getSelectionModel().select(newItem); // Sau khi tạo item mới sẽ chọn đến item vừa tạo
            System.out.println("OK pressed");
        }else {
            System.out.println("Cancel pressed");
        }
    }
    public void deleteItem(TodoItem item){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete TodoItem");
        alert.setHeaderText("Delete TodoItem "+ item.getShortDescription());
        alert.setContentText("Press OK to confirm, or Cancel to back out");
        Optional<ButtonType> res = alert.showAndWait();
        if(res.isPresent() && (res.get()==ButtonType.OK)){
            TodoData.getInstance().deleteTodoItem(item);
        }
    }

    public void handleExit(){
        Platform.exit();
    }
}