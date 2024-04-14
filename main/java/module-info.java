module project.todolist {
    requires javafx.controls;
    requires javafx.fxml;


    opens project.todolist to javafx.fxml;
    exports project.todolist;
}