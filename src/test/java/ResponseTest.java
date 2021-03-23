import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

class Helper {
  static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }
}

public class ResponseTest {
  Response validResponse;
  Response errorResponse;
  // gotten from wireshark
  String bindingRequest1 = "000100002112a442717161572f414c4743556a4c";
  String bindingResponse1 =
      "010100182112a442717161572f414c47"
          + "43556a4c000100080001d7777f000001"
          + "002000080001f6655e12a443";
  int port1 = 55159;
  byte[] ip1 = new byte[] {127, 0, 0, 1};

  @Before
  public void setUP() {
    validResponse = new Response(true, Helper.hexStringToByteArray(bindingRequest1));
    validResponse = new Response(false, Helper.hexStringToByteArray(bindingRequest1));
  }

  @Test
  public void insertUnknownAttributes() {
    Integer[] unknownAttributes = new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    validResponse.insertUnknownAttributes(unknownAttributes);
    byte[] byteResponse = validResponse.getByteResponse();
    assertEquals(0xa, byteResponse[21]);

    assertEquals(
        24 + unknownAttributes.length * 2 + (unknownAttributes.length % 2 == 0 ? 0 : 2),
        byteResponse.length);

    int currentIndex = 24;
    for (Integer i : unknownAttributes) {
      int errorCode = (byteResponse[currentIndex] << 8) | byteResponse[currentIndex + 1];
      assertEquals(i.intValue(), errorCode);
      currentIndex += 2;
    }
  }

  @Test
  public void validByteResponse() {
    // example taken from wireshark
    validResponse = new Response(true, Helper.hexStringToByteArray(bindingRequest1));
    validResponse.insertMappedAddress(ip1, port1);
    validResponse.insertXorMappedAdress(ip1, Helper.hexStringToByteArray(bindingRequest1), port1);
    assertArrayEquals(
        Helper.hexStringToByteArray(bindingResponse1), validResponse.getByteResponse());
  }

  @Test
  public void invalidByteResponse() {
    String errorMessage = "not divisible by four bytes";
    errorResponse = new Response(false, Helper.hexStringToByteArray(bindingRequest1));
    assertEquals(20, errorResponse.getByteResponse().length);
    for (int i = 4; i < errorResponse.getByteResponse().length; i++) {
      assertEquals(
          Helper.hexStringToByteArray(bindingResponse1)[i], errorResponse.getByteResponse()[i]);
    }
    assertEquals(1, errorResponse.getByteResponse()[0]);
    // error code
    assertEquals(17, errorResponse.getByteResponse()[1]);
    assertEquals(0, errorResponse.getByteResponse()[2] + errorResponse.getByteResponse()[3]);

    errorResponse.insertErrorCodeAttribute(456, errorMessage);
    assertEquals(0, errorResponse.getByteResponse().length % 4);
    assertTrue(20 < errorResponse.getByteResponse().length);
    // attribute type error code
    assertEquals(9, errorResponse.getByteResponse()[21]);
    // error class
    assertEquals(4, errorResponse.getByteResponse()[26]);
    // error code
    assertEquals(56, errorResponse.getByteResponse()[27]);
    // three is max padding
    byte[] stringBytes = new byte[errorMessage.getBytes(StandardCharsets.UTF_8).length + 3];

    // src pos gotten from wireshark
    System.arraycopy(
        errorResponse.getByteResponse(),
        72 - 44,
        stringBytes,
        0,
        errorResponse.getByteResponse().length - (72 - 44));

    assertEquals(errorMessage, new String(stringBytes, StandardCharsets.UTF_8).trim());
  }
}
