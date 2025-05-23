package com.vinod.ChatApplication.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class ChatClient {

    @FXML
    private Label ClientTitleId;


    @FXML
    private Button btnSend;

    @FXML
    private ListView<Object> messageView;

    @FXML
    private TextField txtMessage;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private boolean nameAccepted = false;


    @FXML
    void btnSendOnAction(ActionEvent event) {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;
        try {
            out.writeObject(message);
            out.flush();
            txtMessage.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

  /*  public static void main (String[] args) {///

    }*/

    public void closeConnection() {
        try {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void initialize() {
        messageView.setCellFactory(listView -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String) {
                    setText((String) item);
                    setGraphic(null);
                }
            }
        });


        try {
            Socket socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(() -> listenForMessages());
            thread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void listenForMessages() {
        try {
            while(true){
                Object message = in.readObject();
                if (message == null) break;
                if (message instanceof String){
                    String text = (String) message;
                    if (text.startsWith("SUBMITNAME")){
                        if (!nameAccepted){
                            promptForName();
                        }
                    }else if (text.startsWith("NAMEACCEPTED")){
                        nameAccepted = true;
                        Platform.runLater(() -> messageView.getItems().add("Connected as " + clientName));
                        System.out.println("Client Side: Connected as"+clientName);
                        Platform.runLater(() -> ClientTitleId.setText("Client - " + clientName));
                    }else if (text.startsWith("TEXT")){
                        Platform.runLater(() ->{
                            if (text.startsWith("TEXT " + clientName + ": "+"HELP")){
                                messageView.getItems().add("Special Command List:\nTIME\nDATE\nUPDATE\nBYE");

                            }
                            if (text.startsWith("TEXT " + clientName + ": "+"TIME")){
                                messageView.getItems().add("Local Time Now:"+ LocalTime.now());
                            }
                            if (text.startsWith("TEXT " + clientName + ": "+"DATE")){
                                messageView.getItems().add("Local Date Now:"+ LocalDate.now());
                            }
                            if (text.startsWith("TEXT " + clientName + ": "+"BYE")){
                                messageView.getItems().add("Disconnected");
                                System.out.println("Client Side: "+"Disconnected");
                                closeConnection();
                                Platform.exit();
                            }
                            if (text.startsWith("TEXT " + clientName + ": "+"UPTIME")){
                                messageView.getItems().add("Server Uptime: "+text.substring(5+clientName.length()+2));
                            }
                            else if (text.startsWith("TEXT " + clientName + ": ")&&(!text.startsWith("TEXT " + clientName + ": "+"HELP"))){
                                messageView.getItems().add("You: " + text.substring(clientName.length()+2+5));
                                /*System.out.println("Client Side: You: " +text);///*/
                            }else {
                                messageView.getItems().add(text.substring(5));
                            }
                        });
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> messageView.getItems().add("Disconnected" + e.getMessage()));
        }finally {
            closeConnection();
        }
    }

    private void promptForName(){
        Platform.runLater(()->{
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your name");
            dialog.setHeaderText("Please enter your name");

            dialog.showAndWait().ifPresent(name ->{
                clientName = name.trim();
                if (clientName.isEmpty()){
                    messageView.getItems().add("Name cannot be empty, Please try again");
                    System.out.println("Client Side: "+"Name cannot be empty, Please try again");
                    promptForName();
                }else {
                    try {
                        out.writeObject(clientName);
                        out.flush();
                        dialog.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageView.getItems().add("Error sending name, Please try again");
                        System.out.println("Client Side: "+"Error sending name, Please try again");
                    }
                }
            });

            if (dialog.getResult().isEmpty()){
                Platform.exit();
            }
        });
    }


}
