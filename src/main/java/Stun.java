import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class Stun extends Thread{
    private ServerSocket serverSocket;
    private DatagramSocket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private byte header[] = new byte[20];
    private int error = 0x0110;
    private int errorClass =    0b00000100010000;
    private int bindingMethod = 0b00000000000001;
    private int successClass =  0b00000100000000;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private int magicCookie = 0x2112A442;

    public Stun() throws SocketException {
        socket = new DatagramSocket(4445);
    }

    //TODO is temorarily a string
    //TODO message length
    public String formulateHeader(boolean success){

        //legger på 0 helt til lengden er delelig på 16
        String response = String.format("%16s", Integer.toBinaryString(
                (success ? successClass : errorClass) | bindingMethod
                            )).replace(' ', '0');
        response += String.format("%16s", Integer.toBinaryString(magicCookie));
        return response;
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
            System.out.println(formulateHeader(true).length());
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
        Stun server=new Stun();
        server.start();
    }
}