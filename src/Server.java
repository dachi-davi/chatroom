import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("enter the port: ");
        int port = scanner.nextInt();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started");

            while (true) {
                Socket client = server.accept();
                ClientHandler clientHandler = new ClientHandler(client);
                addClient(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void addClient(ClientHandler clientHandler) {
        clients.put(clientHandler.id, clientHandler);
        messageAll("Client number " + clientHandler.getID() + " has joined the chatroom", clientHandler);

    }

    static synchronized void messageAll(String string, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (!sender.equals(client)) {
                client.sendMessage(string);
            }
        }
        sendMessageToServer(string);
    }

    static synchronized void sendMessageToServer(String string) {
        System.out.println(string);
    }

    static class ClientHandler extends Thread {
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private final Socket socket;
        private String id;
        private static int clientNumber = 0;
        private String nickname;

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.id = String.valueOf(clientNumber);
            clientNumber++;
            this.nickname = "anonymus";
        }

        @Override
        public void run() {
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                String message;
                while ((message = (String) input.readObject()) != null) {
                    if (message.equals("/nickname")) {
                        sendMessage("enter your nickname: ");
                        nickNameSetter((String) input.readObject());
                    } else if (message.equals("/pm")) {
                        sendPrivateMessage();
                    } else if (message.equals("/commands")) {
                        commandList();
                    } else if (message.equals("/leave")) {
                        leaveHandler();
                        break;
                    } else messageAll(this.nickname + ": " + message, this);
                }

            } catch (IOException  | ClassNotFoundException e) {
                System.out.println(getNickname() +" disconnected unexpectedly");
            }
        }

        void commandList(){
            String string = "/pm \n" +
                    "/nickname \n" +
                    "/leave";
            try {
                output.writeObject(string);
            }catch (IOException e){
                System.out.println("couldn't write commands to client");
            }

        }

        synchronized void leaveHandler(){
            clients.remove(id);
            messageAll(nickname + " has left the chatroom",this);

            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("couldn't close connections");
            }

        }

        void nickNameSetter(String name) {
            boolean checker = true;
            for (ClientHandler clientHandler : clients.values()) {
                if (clientHandler.getNickname().equals(name)) {
                    checker = false;
                    sendMessage("this name is already taken");
                }
            }
            if (checker)setNickname(name);
        }

        void sendPrivateMessage() {
            try {
                sendMessage("who do you want to message: ");
                String name = (String) input.readObject();
                for (ClientHandler clientHandler : clients.values()) {
                    if (clientHandler.getNickname().equals(name)) {
                        sendMessage("type your message to " + clientHandler.getNickname());
                        String message = (String) input.readObject();
                        clientHandler.sendMessage(this.nickname+": " + message);
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("couldn't send private message");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        void sendMessage(String string) {
            try {
                output.writeObject(string);
                output.flush();
            } catch (IOException e) {
                System.out.println("failed to send message");
            }

        }

        public String getID() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}