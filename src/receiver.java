import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kevin on 3/15/2018.
 */
public class receiver {

    public static boolean listening = true;

    public static void main(String[] args) throws IOException, InterruptedException {



        if (args.length != 2) {
            System.err.println(
                    "Usage: java receiver <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
                Socket clientSocket = new Socket(hostName, portNumber);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()))
        ) {
            byte sequenceNo;
            byte ID;
            int calcdCheckSum; int packetCheckSum;
            String packetContents;

            String message = "";
            int ACK = 0, currentID = 1, numPacketsReceived = 0;
            while (listening) {
                numPacketsReceived++;
                sequenceNo = in.readByte();
                //System.out.println("\n##############################\nSequenceNo: " + sequenceNo);
                if (sequenceNo == -1){
                    in.close();
                    out.close();
                    break;
                }
                ID = in.readByte();
                //System.out.println("ID: " + ID);
                packetCheckSum = in.readInt();
                //System.out.println("checkSum: " + packetCheckSum);
                packetContents = in.readUTF();
                //System.out.println("packetContents: " + packetContents);

                calcdCheckSum = 0;
                byte[] checkSumCalc = packetContents.getBytes();

                for (byte aCheckSumCalc : checkSumCalc) {
                    calcdCheckSum += aCheckSumCalc;
                    if (calcdCheckSum > 9999) {
                        calcdCheckSum /= 10;
                    }
                }

                if(packetCheckSum == calcdCheckSum
                        && currentID == ID
                        && ACK == sequenceNo){
                    //packet PASS
                    //System.out.println("\nPACKET PASS\n");
                    System.out.println("Waiting " + ACK
                            + ", " + numPacketsReceived
                            + ", " + packetContents
                            + ", ACK" + ACK);

                    message += packetContents + " ";
                    if(packetContents.contains(".")){
                        System.out.println("Message: " + message);
                    }
                    out.writeByte(ACK);
                    out.flush();
                    currentID++;
                    ACK = (ACK + 1) % 2;
                }
                else{
                    //packet CORRUPT
                    /*System.out.println("\nPACKET CORRUPT\n");
                    if(packetCheckSum != calcdCheckSum){
                        System.out.println("Checksum was messedup");
                    }
                    if(currentID != ID){
                        System.out.println("ID was messedup");
                    }
                    if(ACK != sequenceNo){
                        System.out.println("sequenceNo was messedup");
                    }*/
                    out.writeByte((ACK + 1) % 2);
                    out.flush();

                    System.out.println("Waiting " + ACK
                            + ", " + numPacketsReceived
                            + ", " + packetContents
                            + ", ACK" + (ACK + 1) % 2);
                }

            }
            //System.out.println("exit");
            in.close();
            out.close();
            clientSocket.close();
            Thread.sleep(3000);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}
