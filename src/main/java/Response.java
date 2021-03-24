import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Response {
  private org.slf4j.Logger log = LoggerFactory.getLogger(Stun.class);
  private static final int magicCookie = 0x2112A442;
  private static final int errorClass = 0b00000100010000;
  private static final int bindingMethod = 0b00000000000001;
  private static final int successClass = 0b00000100000000;
  private byte[] responseBytes;

  public Response(boolean validMessage, byte[] data) {
    log.info("formulating header");
    formulateHeader(validMessage, data);
  }
  // with byte for data and ip then this can be used for tcp-connections as well
  public void insertMappedAddress(byte ip[], int port) {
    log.info("inserting mapped address");
    boolean ipv6 = false;
    if (ip.length > 4) {
      log.info("using ipv6");
      ipv6 = true;
    }
    byte[] attribute = new byte[((64 + (ipv6 ? 128 : 32)) / 8)];
    // attribute type
    attribute[1] = 1;
    // length of mapped address is always less than 256 bytes, so attribute[2] is zero
    attribute[3] = (byte) ((32 + (ipv6 ? 128 : 32)) / 8);
    // family
    attribute[5] = (byte) (ipv6 ? 2 : 1);
    // port
    attribute[6] = (byte) (port >> 8);
    attribute[7] = (byte) (port & 0xff);
    for (int i = 0; i < ip.length; i++) {
      attribute[i + 8] = ip[i];
    }

    addToResponse(attribute);
  }

  public void insertXorMappedAddress(byte[] ip, byte[] data, int port)
      throws IllegalArgumentException {
    log.info("inserting xor-mapped address");
    byte[] transactionID = getTransactionID(data);
    boolean ipv6 = false;
    int length = 0x0008;
    if (ip.length == 16) {
      log.info("using ipv6");
      length = 0x0014;
      ipv6 = true;
    } else if (ip.length > 16) {
      log.error("Found invalid ip: " + new String(ip, StandardCharsets.UTF_8));
      throw new IllegalArgumentException("illegal ip-address");
    }
    byte[] attribute = new byte[32 / 8 + length];

    // type
    attribute[1] = 0x20;
    // length
    attribute[3] = (byte) length;
    // family
    attribute[5] = (byte) (ipv6 ? 0x2 : 0x1);

    int binCookie = magicCookie >> 16;

    int xport = port ^ binCookie;
    // x-port
    attribute[6] = (byte) (xport >> 8);
    attribute[7] = (byte) (xport & 0xff);

    int[] xorbytes =
        new int[] {
          0x21, 0x12, 0xA4, 0x42, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
        };
    if (ipv6) {
      int i = 4;
      for (Byte b : transactionID) {
        xorbytes[i++] = b;
      }

      i = 0;

      for (int j = 0; j < ip.length; j++) {
        int temp = (ip[j] & 0xff) ^ xorbytes[i++];
        attribute[j + 8] = (byte) temp;
      }
    } else {
      int i = 0;
      for (int j = 0; j < ip.length; j++) {
        int temp = (ip[j] & 0xff) ^ xorbytes[i++];
        attribute[j + 8] = (byte) temp;
      }
    }
    addToResponse(attribute);
  }

  public void insertErrorCodeAttribute(int code, String errorMessage) {
    log.info("inserting error code: " + code + " with error message " + errorMessage);
    // smallest possible value, will be expanded probably
    byte[] attribute = new byte[8];
    // attribute type
    attribute[1] = 9;
    if (errorMessage == null) throw new IllegalArgumentException("error message can't be null");
    if (errorMessage.length() > 127 || errorMessage.getBytes(StandardCharsets.UTF_8).length > 763) {
      log.error("error message too long");
      throw new IllegalArgumentException(
          "Given error message exceeds maximum length of 127 characters or 763 bytes");
    }
    if (!(code > 299 && code < 700)) {
      log.error("code: " + code + " is not a valid error code");
      throw new IllegalArgumentException("Given code is not an error code");
    }

    // class
    attribute[6] = (byte) (code / 100);
    // number
    attribute[7] = (byte) (code % 100);
    // reason phrase
    byte[] reasonBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
    // expand array
    int lengthBeforePadding = reasonBytes.length + attribute.length;

    // increases length to number divisible by 4
    byte[] newBytes = new byte[lengthBeforePadding + (3 - (lengthBeforePadding + 3) % 4)];
    System.arraycopy(attribute, 0, newBytes, 0, attribute.length);
    System.arraycopy(reasonBytes, 0, newBytes, attribute.length, reasonBytes.length);

    attribute = newBytes;

    attribute[2] = (byte) (lengthBeforePadding >> 8);
    // -4 because the type and length of stun header should not be included in length
    attribute[3] = (byte) ((lengthBeforePadding & 0xff) - 4);
    addToResponse(attribute);
  }

  public void insertUnknownAttributes(Integer[] unknownAttributes) {
    log.info("inserting unknown attributes: " + Arrays.toString(unknownAttributes));
    // pads length up to nearest 4th byte no padding if already divisible
    int attributeLength = (4 + unknownAttributes.length * 2 + 3) / 4 * 4;
    // (3 - (unknownAttributes.length * 2 + 3) % 4);
    byte[] attribute = new byte[attributeLength];

    // type
    attribute[1] = 0xA;
    // length
    // two bytes per attribute
    int valueLength = unknownAttributes.length * 2;
    attribute[2] = (byte) (valueLength >> 8);
    attribute[3] = (byte) (valueLength & 0xff);
    // attributes
    int attributeIndex = 0;
    for (Integer i : unknownAttributes) {
      attribute[4 + attributeIndex] = (byte) (i >> 8);
      attribute[5 + attributeIndex] = (byte) (i & 0xff);
      attributeIndex += 2;
    }

    addToResponse(attribute);
  }

  private byte[] getTransactionID(byte[] data) {
    log.info("getting transaction id");
    byte[] transactionID = new byte[12];

    for (int i = 0; i < 12; i++) {
      transactionID[i] = data[8 + i];
    }
    return transactionID;
  }

  private void addToResponse(byte[] toBeAdded) {
    log.info("concatenating onto response");
    byte[] newBytes = new byte[responseBytes.length + toBeAdded.length];
    System.arraycopy(responseBytes, 0, newBytes, 0, responseBytes.length);
    System.arraycopy(toBeAdded, 0, newBytes, responseBytes.length, toBeAdded.length);
    responseBytes = newBytes;
  }

  private void formulateHeader(boolean success, byte[] data) {
    log.info("formulating initial header with " + (success ? "success" : "failure"));
    byte[] headerBytes = new byte[20];
    int messageType = (success ? successClass : errorClass) | bindingMethod;

    headerBytes[0] = (byte) (messageType >> 8);
    headerBytes[1] = (byte) (messageType & 0xff);

    // magic cookie
    headerBytes[4] = magicCookie >> 24;
    headerBytes[5] = (byte) (magicCookie >> 16);
    headerBytes[6] = (byte) (magicCookie >> 8);
    headerBytes[7] = magicCookie & 0xff;

    // transaction-id
    for (int i = 0; i < getTransactionID(data).length; i++) {
      headerBytes[8 + i] = getTransactionID(data)[i];
    }

    responseBytes = headerBytes;
  }

  private void insertLength() {
    log.info("inserting length into header");
    int byteArrayLength = responseBytes.length - 20;
    responseBytes[2] = (byte) (byteArrayLength >> 8);
    responseBytes[3] = (byte) (byteArrayLength & 0xff);
  }

  public byte[] getByteResponse() {
    log.info("getting bytes of response");
    insertLength();
    return responseBytes;
  }
}
