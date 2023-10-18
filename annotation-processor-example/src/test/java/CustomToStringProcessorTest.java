import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.otus.SimpleDtoMyToString;
import ru.otus.SimpleDtoWithToString;

public class CustomToStringProcessorTest {

    @Test
    public void SimpleDtoTest() {
        Assertions.assertEquals("SimpleDtoWithToString{y=10,z=100}", new SimpleDtoWithToString().toString());
    }

    @Test
    public void SimpleDtoMyToStringTest() {
        Assertions.assertEquals("SimpleDtoMyToString{}", new SimpleDtoMyToString().toString());
    }

}
