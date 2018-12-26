package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import static java.lang.System.out;

public class Client {

    private static Socket s;
    private static BufferedReader reader;
    private static PrintWriter writer;
    private static Scanner console;

    public static void main(String[] args){
        try{
            s = new Socket("localhost", 12345);
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
            console = new Scanner(System.in);
            if(reader.ready()){
                String serverMessage = reader.readLine();
                out.println(serverMessage);
                if(serverMessage == null) return;
            }
            for(; ; ){
                out.print("§ ");
                out.flush();
                String message = console.nextLine();
                writer.println(message);
                writer.flush();
                String serverMessage = reader.readLine();
                out.println(serverMessage);
                if(message.trim().equals("quit") || serverMessage == null){
                    break;
                }
            }
        }catch(IOException e){
            System.err.println(e.getMessage());
        }finally{
            if(console != null)
                console.close();
            if(writer != null)
                writer.close();
            try{
                if(reader != null)
                    reader.close();
                if(s != null)
                    s.close();
            }catch(IOException ignored){
            }
        }
    }
}
