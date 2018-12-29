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
            s = new Socket("localhost", 5000);
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), /*auto flush*/ true);
            console = new Scanner(System.in);
            for(; ; ){
                out.print("§ ");
                out.flush();
                String message = console.nextLine().trim();
                writer.println(message);
                String serverMessage;
                do{
                    serverMessage = reader.readLine();
                    if(serverMessage == null) return;
                    if(!serverMessage.isEmpty()) out.println(serverMessage);
                }while(reader.ready());
                if(message.equals("quit")) break;
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
