

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Stun extends Thread {
    private final int requestClass = 1;
    private final int indication = 0b00000000010000;
    private final int magicCookie = 0x2112A442;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private byte[] bindMethod = new byte[]{62, -17};
    public Stun() throws SocketException {
        socket = new DatagramSocket(3478);
    }

    private boolean verifyMessage(byte[] message) {
        byte[] magic = ByteBuffer.allocate(4).putInt(magicCookie).array();
        //first two bits zero
        if (message[0] >= 64) return false;
        System.out.println("check 1 passed");
        //is message request, responses are invalid and indications warrant no response
        if ((message[0] & 1) != 0 || (message[1] & 16) != 0) return false;

        System.out.println("check 2 passed");
        //message method is binding
        if(((message[0] & bindMethod[0]) != 0 || (message[1] & bindMethod[1]) != 1)
                //add other methods here
            ) return false;

        System.out.println("check 3 passed");
        //check that message length is sensible - client should only send header
        //TODO can clients send longer messages?
        if((message[2] & 0xff + message[3] & 0xff) != 20) return false;

        System.out.println("check 4 passed");
        //verify magic cookie
        for (int i = 32; i < 63; i++) {
            if (message[i] != magic[i - 32]) return false;
        }
        System.out.println("check 5 passed - packet OK!");

        return true;
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
            if(verifyMessage(packet.getData())) {
                //  Begins building the response by getting transaction ID from the client,
                //  and uses when creating the response header
                Response response = new Response(true, packet.getData());
                //  Adds attributes to the response
                response.insertMappedAddress(packet.getAddress().getAddress(), packet.getPort());
                try {
                    response.insertXorMappedAdress(packet.getAddress().getAddress(), packet.getData(), packet.getPort());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

                //  Prepares the response as a package to be sent
                DatagramPacket send = response.getDataGramPacket();
                send.setAddress(packet.getAddress());
                send.setPort(packet.getPort());

                try {
                    System.out.println("sent packet with address: " + send.getSocketAddress() + "\nwith source address: " + packet.getSocketAddress());
                    socket.send(send);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            throw new IllegalArgumentException("error in packet");
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        Stun server = new Stun();
        server.start();
    }
}
