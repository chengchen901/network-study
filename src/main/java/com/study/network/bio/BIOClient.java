package com.study.network.bio;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author Hash
 * @date 2021年06月10日 13:33
 */
public class BIOClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080);) {
            OutputStream out = socket.getOutputStream();

            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入：");
            String text = scanner.nextLine() + "\r\n";
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            scanner.close();

            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String msg;
            while ((msg = reader.readLine()) != null) {
                if (msg.length() == 0) {
                    break;
                }
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
