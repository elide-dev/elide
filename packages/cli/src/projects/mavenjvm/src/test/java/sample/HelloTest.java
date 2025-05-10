package sample;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloTest {
  @Test public void testHello() {
    String message = Hello.message();
    assertEquals("Hello, World!", message);
  }
}
