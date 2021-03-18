import java.net.DatagramPacket;
import java.util.Arrays;

public class Attribute {
    private static final int magicCookie = 0x2112A442;
    private static final int errorClass = 0b00000100010000;
    private static final int bindingMethod = 0b00000000000001;
    private static final int successClass = 0b00000100000000;
    private String header;
    private String body;

    public Attribute(boolean validMessage, byte[] data){
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
        System.out.println("Mapped Address: " + res.replace(" ", "0"));
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

    private static byte[] getTransactionID(byte[] data) {

        byte[] transactionID = new byte[12];

        for (int i = 0; i < 12; i++) {
            transactionID[i] = data[8 + i];
        }
        return transactionID;
    }

    //TODO probably more efficient to use byte array rather than string
    private void formulateHeader(boolean success, byte[] data) {

        //legger på 0 helt til lengden er delelig på 16
        String response = String.format("%16s", Integer.toBinaryString(
                (success ? successClass : errorClass) | bindingMethod
        ));

        //placeholder for message length ?
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

    public String getResponse(){
        insertLength();
        return header + body;
    }
}
