

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Stun {
    private org.slf4j.Logger log = LoggerFactory.getLogger(Stun.class);
    private HashSet<Integer> knownAttributes = new HashSet<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(2);
    private final int requestClass = 1;
    private final int indication = 0b00000000010000;
    private final int magicCookie = 0x2112A442;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private byte[] bindMethod = new byte[]{62, -17};


    public Stun() throws SocketException {
        log.info("listening to port");
        socket = new DatagramSocket(3478);
    }
    private void addKnownAttributes(){
        knownAttributes.addAll(Arrays.asList(0x0006, 0x0008, 0x000A, 0x0014, 0x0015, 0x0020));
    }

    private boolean verifyMessage(byte[] message) throws BadRequestException {
        byte[] magic = ByteBuffer.allocate(4).putInt(magicCookie).array();
        //first two bits zero
        if (message[0] >= 64) throw new BadRequestException("First two bits not zero");
        log.info("check 1 passed");
        //is message request, responses are invalid and indications warrant no response
        if ((message[0] & 1) != 0) throw new BadRequestException("illegal class");

        log.info("check 2 passed");
        //message method is binding
        if(((message[0] & bindMethod[0]) != 0 || (message[1] & bindMethod[1]) != 1)
                //add other methods here
            ) {
            throw new BadRequestException("invalid method");
        }

        log.info("check 3 passed");
        //check that message length is sensible - client should only send header
        if(message.length < 20){
            log.info("illegally short header");
            throw new BadRequestException("Illegally short header");
        }
        //todo check how long messages clients can send
        if((((message[2] & 0xff) << 8) | message[3] & 0xff) > 9999){
            log.error("illegally long message");
            throw new BadRequestException("Illegally long message");
        }


        log.info("check 4 passed");
        //verify magic cookie
        for (int i = 4; i < 8; i++) {
            if (message[i] != magic[i - 4]) throw new BadRequestException("invalid magic cookie");
        }
        log.info("check 5 passed - packet OK!");
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

    public void listenUDP() {
        boolean running = true;
        while (running) {
            log.info("waiting for packet");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            executor.execute(()->respond(packet));
        }
        socket.close();
    }

    //handles attributes, returns null if all are understood, array of not understood attributes if not
    private Integer[] comprehendAttributes(byte[] message){
        ArrayList<Integer> unknows = new ArrayList<>();
        if(message.length > 20){
            for(int i = 20; i < message.length; i++){
                int attribute = ((message[20] & 0xff) << 8) | (message[21] & 0xff);
                //comprehension required
                if(attribute < 0x7FFF){
                    if(!knownAttributes.contains(attribute)){
                        unknows.add(attribute);
                    }
                    else{
                        //handle attribute here lol
                    }
                }
                log.info("client used attribute: " + attribute);
            }
        }
        return unknows.toArray(new Integer[0]);
    }

    private void respond(DatagramPacket packet) {
        try {
            log.info("package recieved in updated app");
            if(verifyMessage(packet.getData())) {
                log.info("packet verified");
                //  Begins building the response by getting transaction ID from the client,
                //  and uses when creating the response header
                Response response;
                //if any attributes are unknown we return an error message
                Integer[] unknownAttributes = comprehendAttributes(packet.getData());
                if(unknownAttributes.length > 0){
                    response = new Response(false, packet.getData());
                    response.insertErrorCodeAttribute(420, "Unknown attribute");
                    response.insertUnknownAttributes(unknownAttributes);
                }
                else {
                    response = new Response(true, packet.getData());
                    //  Adds attributes to the response
                    response.insertMappedAddress(packet.getAddress().getAddress(), packet.getPort());
                    try {
                        response.insertXorMappedAdress(packet.getAddress().getAddress(), packet.getData(), packet.getPort());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                //  Prepares the response as a package to be sent
                DatagramPacket send = response.getDataGramPacket();
                send.setAddress(packet.getAddress());
                send.setPort(packet.getPort());

                try {
                    log.info("sent packet with address: " + send.getSocketAddress() + "\nwith source address: " + packet.getSocketAddress());
                    socket.send(send);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                //it is an indication
                log.info("package warrants no response");
            }
        } catch (BadRequestException e) {
            log.error("Package could not be verified");
            //the message is invalid
            Response response = new Response(false, packet.getData());
            //Bad Request
            response.insertErrorCodeAttribute(400, "Bad Request: " + e.getMessage());
            try {
                //  Prepares the response as a package to be sent
                DatagramPacket send = response.getDataGramPacket();
                send.setAddress(packet.getAddress());
                send.setPort(packet.getPort());
                socket.send(send);
            }
            catch (IOException ioException){
                log.error(ioException.getMessage());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Stun server = new Stun();
        executor.execute(server::listenUDP);
    }
}
