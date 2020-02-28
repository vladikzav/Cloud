package com.geekbrains.geek.cloud.server;

import sun.security.util.ArrayUtil;

import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public class ClientHandler {

    private Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private Server server;
    private String nick;
    private String strTmp;
    private byte[] bytesTmp;




    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new BufferedInputStream(socket.getInputStream());
            this.out = new BufferedOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String str = "";
                        int n;
                        StringBuilder stringBuilder = new StringBuilder();
                        while (true) {


                            while (socket.isConnected()) {
                                n = in.read();
                                stringBuilder.append((char) n);
                                if (n == (byte) 17) {
                                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                    str = stringBuilder.toString();
                                    stringBuilder = new StringBuilder();
                                }


                                if (str.startsWith("/auth")) {
                                    String[] tokens = str.split(" ");
                                    String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                    if (newNick != null) {
                                        if (!server.onlineCheck(newNick)) {
                                            sendMsg("/authok " + newNick);
                                            nick = newNick;
                                            server.subscribe(ClientHandler.this);
                                            break;
                                        } else {
                                            sendMsg("Server: Такой пользователь уже в сети");
                                        }
                                    } else {
                                        sendMsg("Server: Неверный логин/пароль!");
                                    }
                                }
                                if (str.startsWith("/register")) {
                                    String[] tokens = str.split(" ");
                                    String log = tokens[1];
                                    String nick = tokens[2];
                                    String pass = tokens[3];
                                    if (AuthService.registerCheck(log, nick)) {
                                        AuthService.registerNewClient(log, nick, pass);
                                        sendMsg("Server: Вы успешно зарегистрированы!\n Можете авторизоваться.");
                                    } else
                                        sendMsg("Server: Пользователь с таким логином \n или ником уже существует!");

                                }
                                str = "";
                            }
                            break;

                        }

                        while (true) {

                            while (socket.isConnected()) {
                                n = in.read();
                                stringBuilder.append((char) n);
                                if (n == (byte) 17) {
                                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                    str = stringBuilder.toString();
                                    stringBuilder = new StringBuilder();
                                }
                                System.out.println(nick + ": " + str);

                                if (str.equals("/end")) {
                                    sendMassage("/serverClosed");
                                    break;
                                }
                                if (str.startsWith("/newNick")) {
                                    if (!str.equals("/newNick") && !str.equals("/newNick ")) {
                                        String[] newNickTokens = str.split(" ");
                                        String newNick = newNickTokens[1];
                                        AuthService.setNickname(nick, newNick, ClientHandler.this);
                                    } else
                                        sendMsg("Server: Вы не задали новый ник");

                                }
                                if (str.startsWith("/listoffiles")) {
                                    server.broadcastFileList(ClientHandler.this);
                                }
                                if (str.startsWith("/download_request")){
                                    byte[] ptext = str.getBytes(ISO_8859_1);
                                    String value = new String(ptext, UTF_8);
                                    String[] download_name = value.split("FILE_SPLITTER");
                                    String name = download_name[1];
                                    sendMassage("/download");
                                    download(name);
                                }
                                if (str.startsWith("/upload")) {
                                        upload();
                                        sendMsg("Server: file uploaded");
                                        server.broadcastFileList(ClientHandler.this);
                                }
                                else {
                                    sendMsg("");
                                }
                                str = "";
                            }
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.unsubscribe(ClientHandler.this);
                        System.out.println(nick + " отключился от сервера");
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
            sendMassage(msg);
    }

    public String getNickname(){
        return nick;
    }


    private void upload() {
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


        DataOutputStream fileOut = new DataOutputStream(Files.newOutputStream(Paths.get("server_repository", fileName), CREATE));
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
                //System.out.println(100-(byteCounter/(percent/100)));
                sendMsg(String.valueOf(100-(byteCounter/(percent/100))));
            }

        }


        System.out.println("файл загружен");
        server.broadcastFileList(ClientHandler.this);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void download(String name) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            String filename = name;
            int filenameLength = filename.length();
            byte[] filenameBytes = filename.getBytes();
            long fileLength = Files.size(Paths.get("server_repository", name));
            System.out.println("file size " + fileLength);
            byte[] dataPackage = {15, (byte) filenameLength, 16};
            buf.putLong(0, fileLength);
            byte[] filesize = buf.array();

            BufferedInputStream fileIn = new BufferedInputStream(Files.newInputStream(Paths.get("server_repository", name), READ));
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
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendMassage(String str){
        byte[] bytes1 = str.getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = {17};
        byte[] c = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, c, 0, bytes1.length);
        System.arraycopy(bytes2, 0, c, bytes1.length, bytes2.length);
        try {
            out.write(c,0, c.length);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
