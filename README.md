# hydrogen-java

Java based Hydrogen client

## Usage

~~~java
import com.hydrogen.Client;
import com.hydrogen.IHydrogen;

import java.nio.charset.Charset;


public class Main {

    public static void main(String [] args) {

        // Create
        Client client = new Client(hydrogenImplementor);

        try {
            // Connect
            client.connectToHost("localhost", 1337, true, false);

            // Write a thing
            final byte[] buffer = "ping".getBytes(Charset.forName("UTF-8"));

            // Wait a bit for a response
            Thread.sleep(1000);

            // Disconnect from host
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IHydrogen hydrogenImplementor = new IHydrogen() {
        public void onConnected() {
            System.out.println("onConnected");
        }

        public void onDisconnected() {
            System.out.println("onDisconnected");
        }

        public void onReconnectAttempt() {
            System.out.println("onReconnectAttempt");
        }

        public void onError(Exception e) {
            System.out.println("onError: " + e.getMessage());
            e.printStackTrace();
        }

        public void onDataReceived(byte[] buffer) {
            System.out.println("onDataReceived");
            System.out.println("Received: " + new String(buffer));
        }
    };
}
~~~

## Author

Nathan Sizemore, nathanrsizemore@gmail.com

## License

hydrogen-java is available under the MPL-2.0 license. See the LICENSE file for more info.
