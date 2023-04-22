
import java.net.*;
import java.io.*;
import java.util.*;

// The client that can be run as a console

public class client {

    // notification
    private String notif = " *** ";

    // for I/O
    private ObjectInputStream sInput; // to read from the socket
    private ObjectOutputStream sOutput; // to write on the socket
    private Socket socket; // socket object

    private String server, username; // server and username
    private int port;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    /*
     * Constructor to set things
     * server: the server address
     * port: the port number
     * username: the username
     */

    client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    /*
     * To start the chat
     */
    public boolean start() {
        // try to conn to server
        try {
            socket = new Socket(server, port);
        } catch (Exception ec) {
            display("Error connecting to server:" + ec);
            return false;
        }

        String msg = "Connection accepted" + socket.getInetAddress() + ":" + socket;
        display(msg);

        // creating both data streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the thread to listen from the server
        new ListenFromServer().start();
        // send out username to the server this is the only msg that we
        // will send as a string. All other messages will be chatmessage objects

        try {
            sOutput.writeObject(username);
        } catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // we inform the caller that it worked
        return true;
    }

    /* to send a message to the console */
    private void display(String msg) {
        System.out.println(msg);
    }

    /*
     * to send a message to the server
     */
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * when something goes wrong
     * close the Input/Output streams and disconnect
     */
    private void disconnect() {
        try {
            if (sInput != null)
                sInput.close();
        } catch (Exception e) {
        }
        try {
            if (sOutput != null)
                sOutput.close();
        } catch (Exception e) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
        }
    }

    /*
     * To start the client in console mode use one of the following commands
     * java Client
     * java Client username
     * java Client username portNumber serverAddress
     * at the console prompt
     * if the portNumber is not specified 1500 is used
     * if the serverAddress is not specified "localHost" is used
     * if the username is not specified "Anonymous" is used
     */

    public static void main(String[] args) {
        // default values if not entered
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        // different case according to the length of the arguments
        switch (args.length) {
            case 3:
                // for > java client username portNumber serverAddr
                serverAddress = args[2];
            case 2:
                // for > java Client username portNumber
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number. ");
                }
            case 1:
                // for > javac Client username
                userName = args[0];
            case 0:
                // for > java client
                break;
            // if number of arguments are invalid
            default:
                System.out.println("Usage is: > java Client [username] [portNumber]");
                return;

        }
        // create the Client object
        client client = new client(serverAddress, portNumber, userName);
        // try to conn to the server and return if not connected
        if (!client.start())
            return;

        System.out.println("\nHello.! Welcome to the chatroom.");
        System.out.println("Instructions");
        System.out.println("1. Simply type the message to send broadcast to all active users");
        System.out.println("2. Type '@username<space>your message' without quotes to send messages");
        System.out.println("3. Type 'WHOISIN' without quotes to see list of active clients");
        System.out.println("4. Type 'LOGOUT' without quotes to logout from server");

        // infinite loop to get the input from the user
        while (true) {
            System.out.println("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }
            // message to check who are present in chatroom
            else if (msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));

            }
        }
        // close resource
        scan.close();
        // client completed it's job. disconnect client.
        client.disconnect();

    }

    class ListenFromServer extends Thread {

        public void run() {
            while (true) {
                try {
                    // read the message from the input data stream
                    String msg = (String) sInput.readObject();
                    // print the message
                    System.out.println(msg);
                    System.out.println(">");
                } catch (IOException e) {
                    display(notif + "Server has closed the conn: " + e);
                    break;

                } catch (ClassNotFoundException e2) {
                }
            }
        }
    }
}
