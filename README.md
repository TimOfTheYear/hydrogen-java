# hydrogen-java

Java based Hydrogen client

## Usage

~~~java

public class Main
{
	public static void main(String [] args)	{
		Client client = new Client(hydrogenImplementor);

        // Connect to host
        try {
            client.connectToHost("127.0.0.1", 1337);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Some buffer
        byte[] buffer = new byte[4];
		buffer[0] = (byte)'p';
		buffer[1] = (byte)'i';
		buffer[2] = (byte)'n';
		buffer[3] = (byte)'g';

        // Send a thing
        client.write(buffer);

		while (true) {
			// Do all the things
		}

        // Disconnect from host
        client.disconnect();
	}

    public static IHydrogen hydrogenImplementor = new IHydrogen() {
        public void onConnected() {
            // Called when the client connects
        }

        public void onDisconnected() {
            // Called when the client has been disconnected
        }

        public void onError(Exception e) {
            // Called when an error has occured
        }

        public void onDataReceived(byte[] buffer) {
            // Called when data has been received from host
        }
    };
}
~~~

## Author

Nathan Sizemore, nathanrsizemore@gmail.com

## License

hydrogen-java is available under the MPL-2.0 license. See the LICENSE file for more info.
