

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

    private boolean verifyMessage(byte[] message) throws BadRequestException {
        byte[] magic = ByteBuffer.allocate(4).putInt(magicCookie).array();
        //first two bits zero
        if (message[0] >= 64) throw new BadRequestException("First two bits not zero");
        System.out.println("check 1 passed");
        //is message request, responses are invalid and indications warrant no response
        if ((message[0] & 1) != 0) throw new BadRequestException("illegal class");

        System.out.println("check 2 passed");
        //message method is binding
        if(((message[0] & bindMethod[0]) != 0 || (message[1] & bindMethod[1]) != 1)
                //add other methods here
            ) {
            throw new BadRequestException("invalid method");
        }

        System.out.println("check 3 passed");
        //check that message length is sensible - client should only send header
        if((((message[2] & 0xff << 4)) | message[3] & 0xff) < 20) throw new BadRequestException("Illegally short header");
        //todo check how long messages clients can send
        if((((message[2] & 0xff) << 4) | message[3] & 0xff) > 9999) throw new BadRequestException("Illegally long message");


        System.out.println("check 4 passed");
        //verify magic cookie
        for (int i = 32; i < 63; i++) {
            if (message[i] != magic[i - 32]) throw new BadRequestException("invalid magic cookie");
        }
        System.out.println("check 5 passed - packet OK!");
        //if the message is an indication it warrants no response
        return (message[1] & 16) == 0;
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
            try {
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
            } catch (BadRequestException e) {
                //the message is invalid
                Response response = new Response(false, packet.getData());
                //Bad Request
                response.insertErrorCodeAttribute(400, "Bad Request: " + e.getMessage());
                try {
                    socket.send(response.getDataGramPacket());
                }
                catch (IOException ioException){
                    ioException.printStackTrace();
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
