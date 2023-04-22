import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.text.*;
import java.util.*;

//import static jdk.internal.net.http.common.Utils.close;
public class server {
    // unique id for each conn
    private static int uniqueid;
    // Arraylist to keep list of all clients
    private ArrayList<ClientThread> al;
    // to display time
    private SimpleDateFormat sdf;
    // port conn
    private int port;
    // check server running
    private boolean keepGoing;
    // notification
    private String notif = "***";

    // Constructor to receive the port to listen for conn param
    public server(int port) {
        // the port
        this.port = port;
        // display date
        sdf = new SimpleDateFormat("HH:mm:ss");
        // Arraylist for clients
        al = new ArrayList<ClientThread>();
    }

    // server starting
    public void start() {
        keepGoing = true; // server is live
        // create socket server & wait for conn request
        try {
            // socket used by server
            ServerSocket serverSocket = new ServerSocket(port);
            // infinite loop to wait for conn
            while (keepGoing) {
                display("Server waiting for clients on port " + port + ".");

                // accept conn if requested from client
                Socket socket = serverSocket.accept();
                // break if server stopped
                if (!keepGoing)
                    break;
                // if client is connected, create it's thread
                ClientThread t = new ClientThread(socket);
                // add this client to arraylist
                al.add(t);

                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        // close all data streams and sockets
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {

                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: ";
            display(msg);
        }
    }

    // to stop server
    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Display event console
    private void display(String msg) {
        String time = sdf.format(new Date()) + "" + msg;
        System.out.println(time);
    }

    // To broadcast message to all clients
    private synchronized boolean broadcast(String message) {
        // add time stamp
        String time = sdf.format(new Date());
        // to check if msg is private i.e C to C
        String[] w = message.split(" ", 3);
        boolean isPrivate = false;
        if (w[1].charAt(0) == '@') // @jon msg private msg
            isPrivate = true;
        // if private message, send message to mentioned username only
        if (isPrivate == true) {
            String tocheck = w[1].substring(1, w[1].length());

            message = w[0] + w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            // we loop in reverse order to find the mentioned username
            for (int y = al.size(); --y >= 0;) {
                ClientThread ct1 = al.get(y);
                String check = ct1.getUsername();
                if (check.equals(tocheck)) {
                    // try to write to the client if it fails remove it from the list
                    if (!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                        display("Disconnected Client " + ct1.username + "removed from chat");
                    }
                    // username found and delivered the message
                    found = true;
                    break;
                }
                // mentioned user not found, return false
                if (found != true) {
                    return false;
                }

            }
        }
        // if message is a broadcast message
        else {
            String messageLf = time + " " + message + "\n";
            // display message
            System.out.println(messageLf);

            // we loop in reverse order in case we would have to remove a client
            // because it has disconnected
            for (int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                // try to write to the client if it fails to remove it
                if (!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from chat");
                }
            }
        }
        return true;

    }

    // if client sent LOGOUT message to exit
    synchronized void remove(int id) {
        String disconnectedClient = " ";
        // scan the array list until we find the id
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // if found remove it
            if (ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat room." + notif);
    }
    /*
     * To run as a console app
     * java server
     * java server portNumber
     * if the port number is not specified 1500 is used
     */

    public static void main(String[] args) {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number. ");
                    System.out.println("Usage is: > java server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }
        // starting server obj
        server server = new server(portNumber);
        server.start();
    }

    // One instance of ths thread will run for each client
    class ClientThread extends Thread {
        // the socket to get msg
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;

        // unique id
        int id;
        // username
        String username;
        // msg object to receive msg
        ChatMessage cm;
        // time
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // id
            id = ++uniqueid;
            this.socket = socket;
            System.out.println("Thread trying to create object Input/Output");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room." + notif);
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: ");
                return;

            } catch (ClassNotFoundException e) {
                System.out.println(e);
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername() {
            this.username = username;
        }

        // infinite loop to read and forward message
        public void run() {
            // to loop until logout
            boolean keepGoing = true;
            while (keepGoing) {
                // read string
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;

                    // remove(id);
                    // close();
                }
                // get the message from the chatmessage object received
                String message = cm.getMessage();
                switch (cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + notif);
                        if (confirmation == false) {
                            String msg = notif + "Sorry, no such user exists";
                            writeMsg(msg);
                        }

                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT error");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for (int i = 0; i < al.size(); i++) {
                            ClientThread ct = al.get(i);
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            } // if out of the loop then disconnect
            remove(id);
            close();
        }

        // close everything
        private void close() {
            try {
                if (sOutput != null)
                    sOutput.close();

            } catch (Exception e) {
            }
            try {
                if (sInput != null)
                    sInput.close();

            } catch (Exception e) {
            }
            try {
                if (sInput != null)
                    sInput.close();
            } catch (Exception e) {
            }
            ;
            try {
                if (socket != null)
                    socket.close();
            } catch (Exception e) {
            }
        }

        private boolean writeMsg(String msg) {
            // if client is still conn, send msg to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform user
            catch (IOException e) {
                display(notif + "Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }

    }
}
