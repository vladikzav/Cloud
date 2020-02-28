package com.geekbrains.geek.cloud.server;

import java.sql.*;
import java.util.Vector;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;


    public static void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDB.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setNickname(String login, String newNick, ClientHandler ch) {
        String sql = String.format("SELECT nickname FROM main where nickname = '%s'", newNick);
        String sql1 = String.format("UPDATE main SET nickname = '%s' WHERE login = '%s'", newNick, login);
        try {
            ResultSet rs = stmt.executeQuery(sql);


            if(!rs.next()){
                int rs1 = stmt.executeUpdate(sql1);
                System.out.println(rs1);
                ch.sendMsg("Server: Ник изменится при следующем подключении");
            }else
                ch.sendMsg("Server: Такой ник уже существует");


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static String getNickByLoginAndPass(String login, String pass) {
        int hash = pass.hashCode();
        String sql = String.format("SELECT nickname FROM main where login = '%s' and password = '%s'", login, hash);

        try {
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                String str = rs.getString(1);
                return rs.getString(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean registerCheck(String login, String nick){
        String sql0 = String.format("SELECT nickname FROM main WHERE nickname = '%s'", nick);
        String sql1 = String.format("SELECT login FROM main WHERE login = '%s'", login);

        try {
            ResultSet rs0 = stmt.executeQuery(sql0);
            ResultSet rs1 = stmt.executeQuery(sql1);
            if(rs0.next()||rs1.next()){
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void registerNewClient(String login, String nick, String pass) {
        int hash = pass.hashCode();
        String sql1 = String.format("INSERT INTO main (login, nickname, password) values ('%s','%s','%s')", login, nick, hash);

        try {

                ResultSet rs1 = stmt.executeQuery(sql1);
                if(rs1.next()){

                }


        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
