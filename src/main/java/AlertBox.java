/*
 * Copyright 2016 Morton Mo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by Morton on 5/8/16.
 */
public class AlertBox {
    static boolean booleanResult;

    public static void display(String title, String message) {
        displayYesNo(title, message);
    }

    public static boolean displayYesNo(String title, String message) {

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox messagePane = new VBox();
        messagePane.getChildren().add(new Label(message));
        messagePane.setPadding(new Insets(20));
        messagePane.setMinSize(300, 50);
        messagePane.setAlignment(Pos.CENTER);
        HBox buttonBox = new HBox();
        buttonBox.setPadding(new Insets(20, 0, 20, 20));
        buttonBox.setSpacing(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button yesButton;
        Button noButton;
        yesButton = new Button("Yes");
        noButton = new Button("No");
        buttonBox.getChildren().addAll(yesButton, noButton);
        messagePane.getChildren().add(buttonBox);

        yesButton.setOnAction(e -> {
            stage.close();
            booleanResult = true;
        });

        noButton.setOnAction(e -> {
            stage.close();
            booleanResult = false;
        });

        stage.setOnCloseRequest(e -> {
            stage.close();
            booleanResult = false;
        });

        messagePane.setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.ENTER) {
                yesButton.fire();
            }
        });

        stage.setScene(new Scene(messagePane));
        stage.showAndWait();
        return booleanResult;
    }

    /*@Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println(displayYesNo("Title", "Some message,\nwith line breaks as well."));
    }

    public static void main(String[] args) {
        launch(args);
    }*/
}
