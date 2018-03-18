import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by kevin on 3/15/2018.
 */
public class sender {
    private byte sequenceNo;
    private byte ID;
    private int checkSum;
    private String packetContents;

    private static final int sequenceNOPos = 0;

    private static String createPacket(byte ID , int checkSum, String word){
        byte sequenceNo;
        String msg = "";
        if (ID % 2 == 0){
            sequenceNo = 1;
        }
        else {
            sequenceNo = 0;
        }
        msg += sequenceNo;
        msg += ID;
        msg += checkSum;
        msg += word;
        return msg;
    }


    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 3) {
            System.err.println(
                    "Usage: java sender <host name> <port number> <file name>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String fileName = args[2];

        BufferedReader br = new BufferedReader(new FileReader(fileName));

        byte ID = 1;
        String text = br.readLine();
        Scanner s = new Scanner(text);
        String packetContent;
        String[] messages = new String[text.trim().split("\\s+").length];
        String[] words = new String[text.trim().split("\\s+").length];
        int checkSum; int packetsSent = 0; int packetsSentSuccessfully = 0;
        byte reply;

        //Thread.sleep(4000);

        while(s.hasNext()){
            packetContent = s.next();
            checkSum = 0;
            byte[] checkSumCalc = packetContent.getBytes();
            for (byte aCheckSumCalc : checkSumCalc) {
                checkSum += aCheckSumCalc;
                if (checkSum > 9999) {
                    checkSum /= 100;
                }
            }
            messages[ID-1] = createPacket(ID,checkSum,packetContent);
            words[ID-1] = packetContent;

            ID++;
        }

        try (
                Socket clientSocket = new Socket(hostName, portNumber);
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()))
        ) {
            //Thread.sleep(1000);
            ID = 1;

            byte sequenceNo = 0;

            while(packetsSentSuccessfully < messages.length){
                packetsSent++;
                out.writeByte(sequenceNo);
                out.writeByte(ID);
                checkSum = 0;
                byte[] checkSumCalc = words[ID-1].getBytes();
                for (byte aCheckSumCalc : checkSumCalc) {
                    checkSum += aCheckSumCalc;
                    if (checkSum > 9999) {
                        checkSum /= 100;
                    }
                }
                out.writeInt(checkSum);
                out.writeUTF(words[ID-1]);
                out.flush();
                //System.out.println("------------------------");
                //System.out.println("Sending packet:" + sequenceNo+ID+checkSum+words[ID-1]);
                //System.out.println("------------------------");
                //Thread.sleep(1000);

                reply = in.readByte();
                //System.out.println("Reply: ACK" + reply);
                //If ACK and corresponding ACKs i.e. ACK0 and ACK0
                //messages[ID-1].charAt(0) == ACK#
                if(reply == sequenceNo){
                    //Send new packet
                    ID++; packetsSentSuccessfully++;
                    if(packetsSentSuccessfully == messages.length){
                        //if EOM, send packet containing -1 and break out of loop
                        System.out.println("Waiting ACK"
                                + sequenceNo
                                + ", " + packetsSent
                                + ", ACK" + reply
                                + ", no more packets to send");
                        out.writeByte(-1);
                        out.flush();
                        break;
                    }
                    else {
                        System.out.println("Waiting ACK"
                                + sequenceNo
                                + ", " + packetsSent
                                + ", ACK" + reply
                                + ", send Packet"
                                + messages[ID - 1].charAt(sequenceNOPos));
                    }
                    sequenceNo = (byte)((sequenceNo + 1) % 2);
                }
                else{
                    //Send old packet
                    if(reply == 2){
                        System.out.println("Waiting ACK"
                                + messages[ID-1].charAt(sequenceNOPos)
                                + ", " + packetsSent
                                + ", DROP"
                                + ", resend Packet"
                                + messages[ID-1].charAt(sequenceNOPos));
                    }
                    else{
                        System.out.println("Waiting ACK"
                                + messages[ID-1].charAt(sequenceNOPos)
                                + ", " + packetsSent
                                + ", ACK" + reply
                                + ", resend Packet"
                                + messages[ID-1].charAt(sequenceNOPos));
                    }

                }
            }
            br.close();
            in.close();
            out.close();
            clientSocket.close();
            //Thread.sleep(1000);
            //System.out.println("exit");

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}

