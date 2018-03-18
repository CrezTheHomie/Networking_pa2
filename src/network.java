import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by kevin on 3/15/2018.
 */
public class network {

    public static boolean listening = true;
    public static int clientCount = 0;

    public static int networkOperation = -1;
    public static final int pass = -2;
    public static final int drop = -3;
    public static final int corrupt = -4;

    public static ServerSocket serverSocket = null;

    private static final clientThread[] threads = new clientThread[3];

    public static int chooseOperation(){
        Random ran = new Random();
        double x = ThreadLocalRandom.current().nextDouble(0, 1);
        //System.out.println("x: " + x);
        int operation = 0;
        if(x <= .5){
            operation = pass;
        }
        else if(x > .5 && x <= .75){
            operation = drop;
        }
        else{
            operation = corrupt;
        }
        return operation;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 1) {
            System.err.println("Usage: java network <port number>");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        int i = 0;

        try {
            serverSocket = new ServerSocket(portNumber);
            while (listening) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (threads[i] == null && i < 2) {
                        System.out.println("------------------------");
                        System.out.println("New Client Thread Started");
                        System.out.println("------------------------");
                        (threads[i] = new clientThread(clientSocket, threads)).start();
                        i++; clientCount++;
                    }
                    else{
                        PrintStream os = new PrintStream(clientSocket.getOutputStream());
                        os.println("Network already has a sender and receiver");
                        os.close();
                        clientSocket.close();
                    }
                }
                catch (IOException e) {
                    //System.out.println("Oops IOException");
                }
            }
            //System.out.println("closing");
            serverSocket.close();

        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }

    }
}

class clientThread extends Thread {

    private DataInputStream in = null;
    private DataOutputStream out = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;

    private byte sequenceNo;
    private byte ID;
    private int checkSum;
    private String packetContents;


    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
    }

    public void run() {
        clientThread[] threads = this.threads;

        try {
            in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
            while (network.listening) {
                sequenceNo = in.readByte();
                //System.out.println("Sequence Number: " + sequenceNo);

                if(sequenceNo == -1){
                    threads[0].out.writeByte(sequenceNo);
                    threads[0].out.flush();
                    for (int i = 0; i < 2; i++) {
                        threads[i].in.close();
                        threads[i].out.close();
                        threads[i].clientSocket.close();
                        threads[i].interrupt();
                    }
                    break;
                }
                if(!(in.available() > 0)){
                    //receive ACK from receiver
                    network.networkOperation = network.chooseOperation();
                    //System.out.println("Network chose: " + network.networkOperation);
                    switch (network.networkOperation) {
                        case network.pass:
                            //send packet as is;
                            System.out.println("Received: ACK" + sequenceNo + ", PASS");
                            threads[1].out.writeByte(sequenceNo);
                            threads[1].out.flush();
                            break;
                        case network.drop:
                            //drop packet
                            System.out.println("Received: ACK" + sequenceNo + ", DROP");
                            sequenceNo = (byte)2;
                            threads[1].out.writeByte(sequenceNo);
                            threads[1].out.flush();
                            break;
                        case network.corrupt:
                            //corrupt ACK. No point in corrupting checksum as sender doesn't care about it.
                            System.out.println("Received: ACK" + sequenceNo + ", CORRUPT");
                            //sequenceNo = (byte)((sequenceNo + 1) % 2);
                            threads[1].out.writeByte(sequenceNo);
                            threads[1].out.flush();
                            break;
                    }
                }
                else {
                    //receive packet
                    ID = in.readByte();
                    //System.out.println("ID: " + ID);
                    checkSum = in.readInt();
                    //System.out.println("checkSum: " + checkSum);
                    packetContents = in.readUTF();
                    //System.out.println("packetContents: " + packetContents);

                    network.networkOperation = network.chooseOperation();
                    //System.out.println("Network chose: " + network.networkOperation);
                    switch (network.networkOperation) {
                        case network.pass:
                            //send packet as is;
                            System.out.println("Received: Packet" + sequenceNo + ", " + ID + ", PASS");
                            threads[0].out.writeByte(sequenceNo);
                            threads[0].out.writeByte(ID);
                            threads[0].out.writeInt(checkSum);
                            threads[0].out.writeUTF(packetContents);
                            threads[0].out.flush();
                            break;
                        case network.drop:
                            //drop packet
                            System.out.println("Received: Packet" + sequenceNo + ", " + ID + ", DROP");
                            sequenceNo = 2;
                            threads[1].out.writeByte(sequenceNo);
                            threads[1].out.flush();
                            break;
                        case network.corrupt:
                            //change checksum by + 1
                            System.out.println("Received: Packet" + sequenceNo + ", " + ID + ", CORRUPT");
                            if(checkSum + 1 > 10000){
                                checkSum=0;
                            }
                            else{
                                checkSum++;
                            }
                            threads[0].out.writeByte(sequenceNo);
                            threads[0].out.writeByte(ID);
                            threads[0].out.writeInt(checkSum);
                            threads[0].out.writeUTF(packetContents);
                            threads[0].out.flush();
                            break;
                    }

                }
            }
			/*
			* Close the output stream, close the input stream, close the
			* socket.
			*/
            System.out.println("We must be closing since a -1 was found");

            System.out.println("I'm not listening to you~");


        } catch (IOException e) {
            network.listening = false;
            System.out.println("IO Exception in client(s)");
            System.exit(-1);
        }
    }
}