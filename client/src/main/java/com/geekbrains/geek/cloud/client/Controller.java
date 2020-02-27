package com.geekbrains.geek.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.lang.Byte.valueOf;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;


public class Controller {

    @FXML
    ListView<String> clientFileList;

    @FXML
    ListView<HBox> listView;


    @FXML
    TextField textField;

    @FXML
    Button btn1;

    @FXML
    HBox bottomPanel;

    @FXML
    HBox upperPanel;

    @FXML
    HBox upperPanel2;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    ListView<String> serverFileList;

    @FXML
    TextField loginFieldReg;

    @FXML
    TextField nicknameFieldReg;

    @FXML
    PasswordField passwordFieldReg;


    private boolean isAuthorized;
    private boolean registration = false;


    volatile boolean uploading = false;
    volatile String uploadingFileName;

    public  void setUploadingFileName(String value){
        this.uploadingFileName = value;
        System.out.println("setUploadingFileName");
    }
    public  String getUploadingFileName(){
        return uploadingFileName;
    }
    public  void setUploading(boolean value){System.out.println("setUploading");this.uploading = value;}
    public  boolean getUploading(){
        return uploading;
    }

    public Controller() throws IOException {
    }


    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;

        if(!isAuthorized){
            upperPanel2.setVisible(true);
            upperPanel2.setManaged(true);
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            serverFileList.setVisible(false);
            serverFileList.setManaged(false);
            clientFileList.setVisible(false);
            clientFileList.setManaged(false);
        }
        else {
            upperPanel2.setVisible(false);
            upperPanel2.setManaged(false);
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            serverFileList.setVisible(true);
            serverFileList.setManaged(true);
            clientFileList.setVisible(true);
            clientFileList.setManaged(true);
        }
    }

    Socket socket;
    BufferedInputStream in;
    BufferedOutputStream out;
    Thread t1;


    final String IP_ADRESS = "localhost";
    final int PORT = 8189;

    String myNick;

    public void connect() {
        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());



            t1 = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String str = "";
                        int n;
                        StringBuilder stringBuilder = new StringBuilder();
                        while (true) {
                            while ((n = in.read()) >= 0) {
                                stringBuilder.append((char) n);
                                if (n == (byte) 17) {
                                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                    str = stringBuilder.toString();
                                    stringBuilder = new StringBuilder();
                                }
                                if (str.startsWith("/authok")) {
                                    setAuthorized(true);
                                    String[] tokens = str.split(" ");
                                    myNick = tokens[1];
                                    break;
                                } else {
                                    String finalStr = str;
                                    Platform.runLater(
                                            () -> {
                                                getMassage(finalStr, "Server: ");
                                            }
                                    );
                                }
                            }
                            break;
                        }

                        while (true) {
                            printClientFiles();
                            String nick;
                            while ((n = in.read()) >= 0) {
                                stringBuilder.append((char) n);
                                if (n == (byte) 17) {
                                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                    str = stringBuilder.toString();
                                    stringBuilder = new StringBuilder();
                                }
                                String[] nickGetter = str.split(": ");
                                nick = nickGetter[0];
                                if (str.equals("/serverclosed")) break;
                                if (getUploading()==true){
                                    System.out.println("get uploading true");
                                    sendMassage("/upload "+ getUploadingFileName());
                                    System.out.println("sending massage about uploading");
                                    upload(getUploadingFileName());
                                    System.out.println("uploaded");
                                }
                                if (str.startsWith("/download")){
                                    download();
                                }
                                if (str.startsWith("/filelist")) {
                                    byte[] ptext = str.getBytes(ISO_8859_1);
                                    String value = new String(ptext, UTF_8);
                                    String[] tokens = value.split("FILE_SPLITTER");
                                    serverFileList(tokens);
                                }
                                else{
                                    String finalStr1 = str;
                                    String finalNick = nick;
                                    Platform.runLater(
                                            () -> {
                                                getMassage(finalStr1, finalNick);
                                            }
                                    );
                                }
                            }
                            break;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            setUploading(false);
                            setUploadingFileName(null);
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            });
            t1.setDaemon(true);
            t1.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serverFileList(String[] tokens) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverFileList.getItems().clear();
                for (int i = 1; i < tokens.length; i++) {
                    serverFileList.getItems().add(tokens[i]);
                }

            }
        });
    }

    public void Dispose() {
        System.out.println("Отправляем сообщение о закрытии");
            if(out != null) {
                sendMassage("/end");
            }

    }


    public void sendMsg() {
            sendMassage(textField.getText());
            textField.clear();
            textField.requestFocus();
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if(socket == null || socket.isClosed()) {
            connect();
        }

            sendMassage("/auth "  + loginField.getText() + " " + passwordField.getText());

            loginField.clear();
            passwordField.clear();

    }

    public void selectServerFile(MouseEvent mouseEvent) {
        String name = null;
        if(mouseEvent.getClickCount() == 2) {
            System.out.println("Двойной клик");
            System.out.println(mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean());
            String[] strspl;
            String tmp = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString();
            if (tmp.startsWith("List")) {
                strspl = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString().split("'");
                name = strspl[1];
            } else if (tmp.startsWith("Text")) {
                strspl = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString().split("\"");
                name = strspl[1];
            }
            System.out.println(name);
            if(name != null && !name.equals("null")){
                sendMassage("/download_requestFILE_SPLITTER" + name);
            }
        }
    }

    public void selectClientFile(MouseEvent mouseEvent) {
        String name = null;
        if(mouseEvent.getClickCount() == 2) {
            System.out.println("Двойной клик");
            printClientFiles();
            System.out.println(mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean());
            String[] strspl;
            String tmp = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString();
            if(tmp.startsWith("List")){
                strspl = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString().split("'");
                name = strspl[1];
            }else if(tmp.startsWith("Text")){
                strspl = mouseEvent.getPickResult().getIntersectedNode().accessibleTextProperty().getBean().toString().split("\"");
                name = strspl[1];
            }
            System.out.println(name);

        }
        if(name != null && !name.equals("null")){
            setUploadingFileName(name);
            setUploading(true);
            sendMassage("");
        }
    }

    public void getMassage(String str, String nick){

            HBox hBox = new HBox();
            Label label = new Label(str + "\n");
            hBox.getChildren().add(label);


        if(nick.equals(myNick)){
            hBox.setAlignment(Pos.TOP_RIGHT);
            label.setAlignment(Pos.TOP_RIGHT);
        }else {
            hBox.setAlignment(Pos.TOP_LEFT);
            label.setAlignment(Pos.TOP_LEFT);
        }

            listView.getItems().add(hBox);

    }

    public void printClientFiles(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    clientFileList.getItems().clear();
                    Files.list(Paths.get("client_repository"))
                            .filter(p -> !Files.isDirectory(p))
                            .forEach(path -> clientFileList.getItems().add(String.valueOf(path.getFileName())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void upload(String name) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            String filename = name;
            int filenameLength = filename.length();
            byte[] filenameBytes = filename.getBytes();
            long fileLength = Files.size(Paths.get("client_repository", name));
            System.out.println("file size " + fileLength);
            byte[] dataPackage = {15, (byte) filenameLength, 16};
            buf.putLong(0, fileLength);
            byte[] filesize = buf.array();

            BufferedInputStream fileIn = new BufferedInputStream(Files.newInputStream(Paths.get("client_repository", name), READ));
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write(dataPackage);
            out.flush();
            out.write(filesize);
            out.flush();
            out.write(16);
            out.flush();
            out.write(filenameBytes);
            out.flush();
            out.write(16);
            out.flush();
            int in;
            while ((in = fileIn.read()) >= 0) {
                out.write(in);
            }
            out.flush();
            fileIn.close();
            setUploading(false);
            setUploadingFileName(null);

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void download() {
        try {
            boolean done = false;
            int n;
            int fileNameLength = 0;
            byte[] tmpSize = new byte[8];
            long fileSize;
            String fileName;

            while ((n = in.read()) != (byte) 16) {
                fileNameLength = n;
            }
            System.out.println("file name lenght: " + fileNameLength);
            in.skip(0);

            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);

            while ((n = in.read()) != (byte) 16) {
                buf.put((byte) n);
            }
            buf.flip();
            tmpSize = buf.array();
            buf.clear();
            buf.put(tmpSize, 0, tmpSize.length);
            buf.flip();
            fileSize = buf.getLong();
            buf.clear();
            in.skip(0);
            System.out.println("file lenght: " + fileSize);

            StringBuilder stringBuilder = new StringBuilder();
            while ((n = in.read()) != (byte) 16) {
                stringBuilder.append((char) n);
            }
            in.skip(0);
            String value = stringBuilder.toString();
            byte[] ptext = value.getBytes(ISO_8859_1);
            fileName = new String(ptext, UTF_8);
            System.out.println("file name: " + fileName);


            DataOutputStream fileOut = new DataOutputStream(Files.newOutputStream(Paths.get("client_repository", fileName), CREATE));
            ByteBuffer filebuf = ByteBuffer.allocate(8 * 1024);
            long byteCounter = fileSize;
            while (byteCounter!=0) {
                byteCounter--;
                filebuf.put((byte) in.read());
                if(filebuf.position() == filebuf.capacity() || byteCounter==0){
                    fileOut.write(filebuf.array(), 0, filebuf.position());
                    fileOut.flush();
                    filebuf.clear();
                    long percent;
                    if(fileSize < 100){
                        percent = 100;
                    }else
                        percent = fileSize;
                    System.out.println(100-(byteCounter/(percent/100)));
                }

            }


            printClientFiles();
            System.out.println("файл отправлен");
        }catch (IOException e){
            e.printStackTrace();
        }

    }


    public void toRegister(ActionEvent actionEvent) {
        registration = true;
        if((socket==null)||socket.isClosed()){
            connect();
        }

        sendMassage("/register " + loginFieldReg.getText() + " " + nicknameFieldReg.getText() + " " + passwordFieldReg.getText());
        loginFieldReg.clear();
        passwordFieldReg.clear();
        nicknameFieldReg.clear();


    }

    public void sendMassage(String str) {
            byte[] bytes1 = str.getBytes(UTF_8);
            byte[] bytes2 = {17};
            byte[] c = new byte[bytes1.length + bytes2.length];
            System.arraycopy(bytes1, 0, c, 0, bytes1.length);
            System.arraycopy(bytes2, 0, c, bytes1.length, bytes2.length);
            try {
                out.write(c, 0, c.length);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

}
