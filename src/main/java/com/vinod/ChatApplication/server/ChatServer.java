package com.vinod.ChatApplication.server;

import com.vinod.ChatApplication.client.ChatClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;

public class ChatServer implements Initializable {
    private static final int PORT = 5000;
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private boolean isRunning = false;
    private ServerSocket serverSocket;

    @FXML
    private Label serverlabelId;

    @FXML
    private TextArea txtServerStatus;

    @FXML
    void btnAddClientOnAction(ActionEvent event) {
        if (!isRunning){
            startServer();
        }

        openClientWindow();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        appendStatus("Server initialized, click 'Add client' to start");
        System.out.println("Server initialized, click 'Add client' to start");
    }

  /*  public static void main(String[] args) {////

    }*/


        private void appendStatus(String message) {
        Platform.runLater(() -> txtServerStatus.appendText(message + "\n"));
    }

    private void openClientWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vinod/ChatApplication/client.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Client");
                stage.setScene(scene);
                stage.setResizable(false);
                ;               stage.setOnCloseRequest(event -> {
                    ChatClient controller = loader.getController();
                    controller.closeConnection();
                });
                stage.show();
                appendStatus("New client window opened");
                System.out.println("New client window opened");
            } catch (IOException e) {
                appendStatus("Error opening client window");
                System.out.println("Error opening client window");
                e.printStackTrace();
            }
        });
    }

    private void startServer() {
        isRunning = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendStatus("Server started on port " + PORT);
                System.out.println("Server started on port " + PORT);

                //server eka start wunama
                while(isRunning){
                    Socket clientSocket = serverSocket.accept();
                    appendStatus("New client connected");
                    System.out.println("New client connected");

                    //aluth thread ekakin clienHandler class eka handle karanawa
                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isRunning){
                    appendStatus("Error starting server: " + e.getMessage());
                    System.out.println("Error starting server: " + e.getMessage());
                }
            }
        }).start();
    }

    //Client Handler class
    private class ClientHandler implements Runnable{
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;

        ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while(true){
                    out.writeObject("SUBMITNAME");
                    clientName = (String) in.readObject();
                    if (!clientName.trim().isEmpty() && clientName != null){
                        break;
                    }
                    appendStatus("Invalid name, requesting again");
                    System.out.println("Invalid name, requesting again");
                }

                out.writeObject("NAMEACCEPTED");
                appendStatus("Client " + clientName + " connected");
                System.out.println("Client " + clientName + " connected");
                broadcast("TEXT " + clientName + " joined the chat");
                System.out.println("Client " + clientName + " joined the chat");

                synchronized (writers){
                    writers.add(out);
                }

                while(true){
                    try {
                        Object message = in.readObject();
                        if (message == null) break;
                        if (message instanceof String){
                            String text = (String) message;
                            broadcast("TEXT " + clientName + ": " +text);
                            System.out.println("TEXT " + clientName + ": " +text);
                        }
                    } catch (IOException e) {
                        /*throw new RuntimeException(e);*/
                        System.out.println("Client Logout");
                        break;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);

                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                appendStatus("Error connecting to client");
                System.out.println("Error connecting to client");
            }finally {
                if (clientName != null){
                    appendStatus("Client " + clientName + " disconnected");
                    System.out.println("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat");
                    System.out.println("Client " + clientName + " left the chat");
                }

                synchronized (writers){
                    writers.remove(out);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    appendStatus("Error closing client socket");
                    System.out.println("Error closing client socket");
                    e.printStackTrace();
                }
            }
        }
    }

    private void broadcast ( String message){
        synchronized (writers){
            for (ObjectOutputStream writer : writers){
                try {
                    writer.writeObject(message);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    appendStatus("Error broadcasting the message..");
                    System.out.println("Error broadcasting the message..");
                }
            }
        }
    }




}
