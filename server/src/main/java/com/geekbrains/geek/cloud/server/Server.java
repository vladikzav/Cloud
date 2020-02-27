package com.geekbrains.geek.cloud.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;




    public Server() {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            AuthService.connection();

            //String str = AuthService.getNickByLoginAndPass("login1","pass1");
            //System.out.println(str);
            //


            server = new ServerSocket(8189);
            System.out.println("Сервер запущен");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }




    public boolean onlineCheck(String nick){
        boolean clientOnline = false;
        for(ClientHandler o: clients){
            if(nick.equals(o.getNickname())){
                clientOnline = true;
                break;
            }
        }
        return clientOnline;
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastFileList(client);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }

    public void broadcastFileList(ClientHandler client) {
        StringBuilder sb = new StringBuilder();
        sb.append("/filelistFILE_SPLITTER");
        try {
            Files.list(Paths.get("server_repository"))
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(obj -> sb.append(obj.getFileName() + "FILE_SPLITTER"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String out = sb.toString();

        client.sendMsg(out);
//        for (ClientHandler o : clients) {
//            o.sendMsg(out);
//        }

    }

}
