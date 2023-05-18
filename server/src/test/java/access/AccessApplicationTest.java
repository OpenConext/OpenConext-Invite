package access;

import org.junit.jupiter.api.Test;

class AccessApplicationTest {

    @Test
    void main() {
        AccessServerApplication.main(new String[]{"--server.port=8088"});
    }
}