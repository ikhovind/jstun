import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Random;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;
    private final int errorClass =    0b00000100010000;
    private final int bindingMethod = 0b00000000000001;
    private final int successClass =  0b00000100000000;

    private byte[] buf;

    public Client() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public String sendEcho(byte[] msg) throws IOException {
        buf = msg;
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        socket.send(packet);

        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(
                packet.getData(), 0, packet.getLength());
        return received;
    }

    public void close() {
        socket.close();
    }

    private static String generateTransactionID() {
        Random rand = new Random();
        String res = "";
        BigInteger num;
        for(int i = 0 ; i<6 ; i++){
            num = new BigInteger(16, rand);
            res += Integer.toBinaryString(Integer.parseInt(num.toString()));
        }
        return res;
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        //send request
        client.sendEcho(new byte[]{0x00, (byte) 0xEE});
    }
}