import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Response {
    private static final int magicCookie = 0x2112A442;
    private static final int errorClass = 0b00000100010000;
    private static final int bindingMethod = 0b00000000000001;
    private static final int successClass = 0b00000100000000;
    private String header;
    private String body;

    public Response(boolean validMessage, byte[] data){
        formulateHeader(validMessage, data);
        body = "";
    }
    //with byte for data and ip then this can be used for tcp-connections as well
    public void insertMappedAddress(byte ip[], int port){
        boolean ipv6 = false;
        if (ip.length > 4) {
            ipv6 = true;
        }

        String res = String.format("%16s", Integer.toBinaryString(0x0001)); // 0x0001 is the attribute type for mapped address
        if (ipv6) res += String.format("%16s", Integer.toBinaryString(0x014));
        else res += String.format("%16s", Integer.toBinaryString(0x0008));       // Hard-coded length of an IPv4 address

        res += String.format("%8s", Integer.toBinaryString(0x0)); //first byte must be all zeroes
        if (ipv6) res += String.format("%8s", Integer.toBinaryString(0x02)); //0x02 is ipv6 family
        else res += String.format("%8s", Integer.toBinaryString(0x01)); //0x01 is ipv4 family

        res += String.format("%16s", Integer.toBinaryString(port)); //16 bit port where message was recieved from

        for (Byte b : ip) {
            res += String.format("%8s", Integer.toBinaryString((b & 0xff)));
        }
        body += res.replace(" ", "0");
    }

    public void insertXorMappedAdress(byte[] ip, byte[] data, int port) throws IllegalArgumentException {
        byte[] transactionID = getTransactionID(data);
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
            throw new IllegalArgumentException("illegal ip-address");
        }

        String res = String.format("%16s", Integer.toBinaryString(0x0020));    // 0x0020 is the attribute type for XOR mapped address
        res += String.format("%16s", Integer.toBinaryString(length));            // Attribute Length. 0x0008 for IPv4, 0x0014 for IPv6

        res += String.format("%8s", Integer.toBinaryString(0x0));             // 1 byte used for alignment purposes, these are ignored
        res += String.format("%8s", Integer.toBinaryString(family));            // Attribute Family. 0x01 for IPv4, 0x02 for IPv6
        int binCookie = Integer.parseInt(
                String.format("%32s", Integer.toBinaryString(magicCookie))
                        .replace(" ", "0")
                        .substring(0, 16), 2);
        int binPort = Integer.parseInt(Integer.toBinaryString(port), 2);
        int xport = binPort ^ binCookie;
        res += String.format("%16s",
                Integer.toBinaryString(xport));

        int[] xorbytes = new int[]{0x21, 0x12, 0xA4, 0x42, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};
        if (ipv6) {
            int i = 4;
            for (Byte b : transactionID) {
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

        this.body += res.replace(" ", "0");

    }

    public void insertErrorCodeAttribute(int code, String errorMessage){
        body += String.format("%16s", Integer.toBinaryString(0x0009)); //type for error code
        int lengthBefore = body.length();
        if(errorMessage.length() > 127 || errorMessage.getBytes(StandardCharsets.UTF_8).length > 763){
            throw new IllegalArgumentException("Given error message exceeds maximum length of 127 characters or 763 bytes");
        }
        if(!(code > 299 && code < 700)) throw new IllegalArgumentException("Given code is not an error code");
        //20 bits for alignment purposes
        body +=  String.format("%20s", Integer.toBinaryString(0));
        //The Class represents the hundreds digit of the error code
        body += String.format("%3s", Integer.toBinaryString(code / 100));
        //The Number represents the error code modulo 100
        body += String.format("%3s", Integer.toBinaryString(code % 100));

        for(Byte b : errorMessage.getBytes(StandardCharsets.UTF_8)){
            body += Integer.toBinaryString(b & 0xff);
        }
        int length =  body.length() - lengthBefore;
        body = body.substring(0, lengthBefore) +
                String.format("%16s", Integer.toBinaryString(length / 8)) +
                    body.substring(lengthBefore);
        //TODO this pads after value, maybe you are supposed to do it before
        body += String.format("%" + body.length() % 32 + "s", 0).replace(" ", "0");
    }

    public void insertUnknownAttributes(Integer[] attributes){

        int lengthBefore = body.length();
        body += String.format("%16s", Integer.toBinaryString(0x000A)); // 0x000A is the attribute type for unknown attribute

        //attribute types we don't know
        for(Integer i : attributes){
            body += Integer.toBinaryString(i);
        }


        int length =  body.length() - lengthBefore;
        //insert length
        body = body.substring(0, lengthBefore) +
                String.format("%16s", Integer.toBinaryString(length/8)) +
                body.substring(lengthBefore);

        //TODO this pads after value, maybe you are supposed to do it before
        body += String.format("%" + body.length() % 32 + "s", 0).replace(" ", "0");
    }

    private static byte[] getTransactionID(byte[] data) {

        byte[] transactionID = new byte[12];

        for (int i = 0; i < 12; i++) {
            transactionID[i] = data[8 + i];
        }
        return transactionID;
    }

    //TODO probably more efficient to use byte array rather than string
    private void formulateHeader(boolean success, byte[] data) {

        //first two bits plus message type is the first 16 bits
        String response = String.format("%16s", Integer.toBinaryString(
                (success ? successClass : errorClass) | bindingMethod
        ));

        response += String.format("%16s", Integer.toBinaryString(0b0));

        response += String.format("%32s", Integer.toBinaryString(magicCookie));

        for (Byte b : getTransactionID(data)) {
            response += String.format("%8s", Integer.toBinaryString(Byte.toUnsignedInt(b)));
        }
        header = response.replace(" ", "0");
    }

    private void insertLength(){
        //  Overwrites the header's placeholder for length.
        //  NB! THIS SHOULD BE DONE LAST
        int byteLength = this.body.length() / 8;
        String binLength =
                String.format("%16s", Integer.toBinaryString(byteLength))
                        .replace(" ", "0");

        header = header.substring(0, 16) + binLength + header.substring(32);
    }

    private String getResponse(){
        insertLength();
        return header + body;
    }

    private byte[] binaryStringToByteArray(String str) {
        byte[] bytes = new byte[str.length() / 8];
        int start = 0, end = 8;
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(String.format("%8s", Integer.parseInt(str.substring(start, end).replace(" ", "0"))).replace(" ", "0"), 2);
            start += 8;
            end += 8;
        }

        return bytes;
    }

    public DatagramPacket getDataGramPacket(){
        byte[] responseArray = binaryStringToByteArray(getResponse());
        return new DatagramPacket(binaryStringToByteArray(getResponse()), responseArray.length);
    }
}
