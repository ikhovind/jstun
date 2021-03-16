import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Stun extends Thread{
    private ServerSocket serverSocket;
    private DatagramSocket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private byte header[] = new byte[20];
    private final int errorClass =    0b00000100010000;
    private final int bindingMethod = 0b00000000000001;
    private final int successClass =  0b00000100000000;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private int magicCookie = 0x2112A442;

    public Stun() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    //TODO is temorarily a string
    //TODO message length is size in bytes not including 20-byte stun header
    public String formulateHeader(boolean success, byte[] transactionID){

        //legger på 0 helt til lengden er delelig på 16
        String response = String.format("%16s", Integer.toBinaryString(
                (success ? successClass : errorClass) | bindingMethod
                            ));

        //placeholder for message length ?
        response += String.format("%16s", Integer.toBinaryString(0b0));

        response += String.format("%32s", Integer.toBinaryString(magicCookie));

        //TODO Get transaction ID from client
        for(Byte b : transactionID){
            response += String.format("%8s", Integer.toBinaryString(Byte.toUnsignedInt(b)));
        }

        return response.replace(" ", "0");
    }

    private boolean verifyMessage(byte[] message){
        byte[] magic = ByteBuffer.allocate(4).putInt(magicCookie).array();
        //first two bits zero
        if(message[0] >= 64) return false;
        //verify magic cookie
        for(int i = 32; i < 63; i++){
            if(message[i] != magic[i - 32]) return false;
        }
        //check that message class is allowed for particular method


        return true;
    }


    public void run() {
        running = true;

        while (running) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);

            String received = new String(packet.getData(), 0, packet.getLength());

            System.out.println(packet.getSocketAddress());
            System.out.println("recieved " + Arrays.toString(packet.getData()));

            byte[] ans = packet.getData();

            byte[] transactionID = new byte[12];

            for(int i = 0; i < 12; i++){
                transactionID[i] = ans[62 + i];
            }

            System.out.println(formulateHeader(true, transactionID));

            if (received.equals("end")) {
                running = false;
                continue;
            }
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        Stun server = new Stun();
        server.start();
    }
}