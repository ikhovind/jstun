

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Stun {
    private boolean running = true;
    private org.slf4j.Logger log = LoggerFactory.getLogger(Stun.class);
    private HashSet<Integer> knownAttributes = new HashSet<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(3);
    private final int requestClass = 1;
    private final int indication = 0b00000000010000;
    private final int magicCookie = 0x2112A442;
    private DatagramSocket socket;
    private byte[] buf = new byte[256];
    private byte[] bindMethod = new byte[]{62, -17};
    private int port = 3478;


    public Stun() throws SocketException {
        log.info("listening to port: " + port);
        socket = new DatagramSocket(port);
    }
    private void addKnownAttributes(){
        //TODO insert attributes we know here
        knownAttributes.addAll(Arrays.asList());
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
        //if the sent message is too large for the buffer
        if((((message[2] & 0xff) << 8) | message[3] & 0xff) > 256){
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
        while (running) {
            log.info("waiting for udp-packet");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            executor.execute(()->{
                    log.info("package recieved in updated app");
                    //  Begins building the response by getting transaction ID from the client,
                    //  and uses when creating the response header
                    Response response = formulateResponse(packet.getData(), packet.getAddress().getAddress(), packet.getPort());
                    if(response != null) {
                        //  Prepares the response as a package to be sent
                        DatagramPacket send = new DatagramPacket(response.getByteResponse(), response.getByteResponse().length);
                        send.setAddress(packet.getAddress());
                        send.setPort(packet.getPort());

                        try {
                            log.info("sent packet with address: " + send.getSocketAddress() + "\nwith source address: " + packet.getSocketAddress());
                            socket.send(send);
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }
                    } else{
                        log.info("package warrants no response");
                    }
                }
            );
        }
        socket.close();
    }

    public void listenTCP() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while(running){
                log.info("waiting for tcp-connection");
                Socket socket = serverSocket.accept();
                log.info("tcp connection accepted");
                executor.execute(()-> {
                    try {
                        handleTCPConnection(socket);
                    } catch (IOException ioException) {
                        log.error("IOexception when handling tcp connection " + ioException.getMessage());
                    }

                });
            }
        } catch (IOException e){
            log.error("IOException when opening socket: " + e.getMessage());
        }
    }

    private void handleTCPConnection(Socket socket) throws IOException {
        log.info("handling tcp-connection");
        byte[] buffer = new byte[256];
        log.info("creating inputstream");
        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        log.info("inputstream created");
        dataInputStream.read(buffer, 0, 4);

        int messageLength = ((buffer[2] & 0xff) << 8) | (buffer[3] & 0xff);
        if(messageLength <= 256 - 20){
            log.info("read buffer fully");
            //want to read for specified length plus header
            dataInputStream.readFully(buffer, 0, messageLength + 20);
        }
        Response response = formulateResponse(buffer, socket.getInetAddress().getAddress(), socket.getPort());
        if (response != null) {
            log.info("responding to tcp-connection");
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.write(response.getByteResponse());
        } else {
            log.info("tcp-connection warrants no response");
        }

    }

    //handles attributes, returns null if all are understood, array of not understood attributes if not
    private Integer[] comprehendAttributes(byte[] message){
        int messageLength = ((message[2] & 0xff) << 8) | (message[3] & 0xff);
        ArrayList<Integer> unknowns = new ArrayList<>();
        if(messageLength > 20){
            for(int i = 20; i < messageLength; i++){
                int attribute = ((message[20] & 0xff) << 8) | (message[21] & 0xff);
                //comprehension required
                if(attribute < 0x7FFF){
                    if(!knownAttributes.contains(attribute)){
                        unknowns.add(attribute);
                    }
                    else{
                        //handle attribute here lol
                    }
                }
                log.info("client used attribute: " + attribute);
            }
        }
        return unknowns.toArray(new Integer[0]);
    }

    private void respond(DatagramPacket packet) {
    }

    private Response formulateResponse(byte[] message, byte[] address, int port)  {
        try {
            if(verifyMessage(message)) {
                Response response;
                //if any attributes are unknown we return an error message
                Integer[] unknownAttributes = comprehendAttributes(message);
                if(unknownAttributes.length > 0){

                    response = new Response(false, message);
                    response.insertErrorCodeAttribute(420, "Unknown attribute");
                    response.insertUnknownAttributes(unknownAttributes);
                }
                else {
                    response = new Response(true, message);
                    //  Adds attributes to the response
                    response.insertMappedAddress(address, port);
                    try {
                        response.insertXorMappedAdress(address, message, port);
                    } catch (IllegalArgumentException e) {
                        log.error(e.getMessage());
                    }
                }
                return response;
            }
            else{
                //it is an indication
                log.info("package warrants no response");
                return null;
            }
        } catch (BadRequestException e) {
            log.error("Package could not be verified - sending error message");
            //the message is invalid
            Response response = new Response(false, message);
            //Bad Request
            response.insertErrorCodeAttribute(400, "Bad Request: " + e.getMessage());
            return response;
        }
    }


    public static void main(String[] args) throws IOException {
        Stun server = new Stun();
        executor.execute(server::listenUDP);
        executor.execute(server::listenTCP);
    }
}
