

import java.io.*;
import java.net.*;

public class Stun extends Thread {
    private DatagramSocket socket;
    private byte[] buf = new byte[256];

    public Stun() throws SocketException {
        socket = new DatagramSocket(3478);
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
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        Stun server = new Stun();
        server.start();
    }
}
