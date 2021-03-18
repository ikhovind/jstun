

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

public class Stun extends Thread {
    private ServerSocket serverSocket;
    private DatagramSocket clientSocket;
    private BufferedReader in;
    private byte header[] = new byte[20];
    private final int errorClass = 0b00000100010000;
    private final int bindingMethod = 0b00000000000001;
    private final int successClass = 0b00000100000000;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private int magicCookie = 0x2112A442;

    public Stun() throws SocketException {
        socket = new DatagramSocket(3478);
    }

    //TODO is temporarily a string
    public String formulateHeader(boolean success, byte[] transactionID) {

        //legger på 0 helt til lengden er delelig på 16
        String response = String.format("%16s", Integer.toBinaryString(
                (success ? successClass : errorClass) | bindingMethod
        ));

        //placeholder for message length ?
        response += String.format("%16s", Integer.toBinaryString(0b0));

        response += String.format("%32s", Integer.toBinaryString(magicCookie));

        //TODO Get transaction ID from client
        for (Byte b : transactionID) {
            response += String.format("%8s", Integer.toBinaryString(Byte.toUnsignedInt(b)));
        }
        return response.replace(" ", "0");
    }

    public String formulateMappedAddress(DatagramPacket packet) {
        boolean ipv6 = false;
        byte[] ip = packet.getAddress().getAddress();
        if (ip.length > 4) {
            ipv6 = true;
        }

        String res = String.format("%16s", Integer.toBinaryString(0x0001)); // 0x0001 is the attribute type for mapped address
        if (ipv6) res += String.format("%16s", Integer.toBinaryString(0x014));
        else res += String.format("%16s", Integer.toBinaryString(0x0008));       // Hard-coded length of an IPv4 address

        res += String.format("%8s", Integer.toBinaryString(0x0)); //first byte must be all zeroes
        if (ipv6) res += String.format("%8s", Integer.toBinaryString(0x02)); //0x02 is ipv6 family
        else res += String.format("%8s", Integer.toBinaryString(0x01)); //0x01 is ipv4 family

        res += String.format("%16s", Integer.toBinaryString(packet.getPort())); //16 bit port where message was recieved from

        for (Byte b : ip) {
            res += String.format("%8s", Integer.toBinaryString((b & 0xff)));
        }
        System.out.println("Mapped Address: " + res.replace(" ", "0"));
        return res.replace(" ", "0");
    }

    public String formulateXORMappedAddress(DatagramPacket packet) {
        byte[] ip = packet.getAddress().getAddress();
        Boolean ipv6 = false;
        int length = 0x0008;
        int family = 0x01;
        System.out.println(ip.length + " for ip " + Arrays.toString(ip));
        if (ip.length == 16) {
            length = 0x0014;
            family = 0x02;
            ipv6 = true;
            System.out.println("IPv6 address found");
        } else if (ip.length > 16) {
            System.out.println("what in tarnation is this: " + Arrays.toString(ip));
            return "ERROR";
        }

        String res = String.format("%16s", Integer.toBinaryString(0x0020));    // 0x0020 is the attribute type for XOR mapped address
        res += String.format("%16s", Integer.toBinaryString(length));            // Attribute Length. 0x0008 for IPv4, 0x0014 for IPv6

        res += String.format("%8s", Integer.toBinaryString(0x0));             // 1 byte used for alignment purposes, these are ignored
        res += String.format("%8s", Integer.toBinaryString(family));            // Attribute Family. 0x01 for IPv4, 0x02 for IPv6
        int binCookie = Integer.parseInt(
                String.format("%32s", Integer.toBinaryString(magicCookie))
                        .replace(" ", "0")
                        .substring(0, 16), 2);
        int binPort = Integer.parseInt(Integer.toBinaryString(packet.getPort()), 2);
        int xport = binPort ^ binCookie;
        res += String.format("%16s",
                Integer.toBinaryString(xport));

        int[] xorbytes = new int[]{0x21, 0x12, 0xA4, 0x42, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        if (ipv6) {
            int i = 4;
            for (Byte b : getTransactionID(packet)) {
                xorbytes[i++] = b;
            }

            i = 0;

            for (Byte b : ip) {
                int temp = (b & 0xff) ^ xorbytes[i++];
                res += String.format("%8s", Integer.toBinaryString(temp));
            }
        } else {
            int i = 0;
            for (Byte b : ip) {
                int temp = (b & 0xff) ^ xorbytes[i++];
                res += String.format("%8s", Integer.toBinaryString(temp));
            }
        }

        return res.replace(" ", "0");
    }

    private boolean verifyMessage(byte[] message) {
        byte[] magic = ByteBuffer.allocate(4).putInt(magicCookie).array();
        //first two bits zero
        if (message[0] >= 64) return false;
        //verify magic cookie
        for (int i = 32; i < 63; i++) {
            if (message[i] != magic[i - 32]) return false;
        }
        //check that message class is allowed for particular method
        //check that message class is request
        //is message request
        if ((message[0] & 1) != 0 || (message[1] & 16) != 0) return false;

        return true;
    }

    private byte[] binaryStringToByteArray(String str) {
        byte[] bytes = new byte[str.length() / 8];
        int start = 0, end = 8;
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(String.format("%8s", Integer.parseInt(str.substring(start, end))).replace(" ", "0"), 2);
            start += 8;
            end += 8;
        }

        return bytes;
    }

    private byte[] getTransactionID(DatagramPacket packet) {
        byte[] ans = packet.getData();

        byte[] transactionID = new byte[12];

        for (int i = 0; i < 12; i++) {
            transactionID[i] = ans[8 + i];
        }
        return transactionID;
    }

    private void debugPacket(DatagramPacket packet) {
        System.out.println(packet.getSocketAddress());
        System.out.printf("received: ");
        String print = "";
        for (Byte b :
                packet.getData()) {
            print += b & 0xff;
        }
        System.out.println(print);

        System.out.println("PACKET SOCKETADDRESS: " + packet.getSocketAddress());
        System.out.println("PACKET ADDRES + PORT: " + packet.getAddress() + ":" + packet.getPort());
    }

    public void run() {
        boolean running = true;

        while (running) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

                //  Begins building the response by getting transaction ID from the client,
                //  and uses when creating the response header
            byte[] transactionID = getTransactionID(packet);
            String response = "";
            response += formulateHeader(true, transactionID);

                //  Adds attributes to the response
            response += formulateXORMappedAddress(packet);
            response += formulateMappedAddress(packet);

                //  Overwrites the header's placeholder for length.
                //  NB! THIS SHOULD BE DONE LAST
            int byteLength = (response.length() / 8) - 20;
            String binLength =
                    String.format("%16s", Integer.toBinaryString(byteLength))
                            .replace(" ", "0");

            String newResponse = response.substring(0, 16) + binLength + response.substring(32);

                //  Prepares the response as a package to be sent
            byte[] responseArr = binaryStringToByteArray(newResponse);
            DatagramPacket send = new DatagramPacket(responseArr, responseArr.length);
            send.setAddress(packet.getAddress());
            send.setPort(packet.getPort());

            try {
                System.out.println("sent packet with address: " + send.getSocketAddress() + "\nwith source address: " + packet.getSocketAddress());
                socket.send(send);
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
