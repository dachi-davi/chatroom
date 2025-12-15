import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true){
            try {
                System.out.println("to connect to the server enter the server ip: ");
                String ip = scanner.next();
                System.out.println("enter the port: ");
                int port = scanner.nextInt();

                try (Socket socket = new Socket(ip,port)){
                    System.out.println("connected to the chatRoom");

                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                    Thread listener = createListener(socket);
                    listener.start();

                    String input;
                    while (!Objects.equals(input = scanner.next(), null)){
                        output.writeObject(input);
                        output.flush();
                        if (input.equals("/leave"))break;
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }catch (Exception e){
                System.out.println("something went wrong, please try again");
            }
        }
    }

    private static Thread createListener(Socket socket){
        return new Thread(){
            @Override
            public void run() {
                try {
                    ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    String message;
                    while ((message = (String) input.readObject())!=null){
                        System.out.println(message);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("disconnected");
                }
            }
        };
    }
}
