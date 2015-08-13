# hydrogen-java

Java based Hydrogen client

## Usage

~~~java

public class Main implements IHydrogen
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
        Vector<Byte> buffer = new Vector<>();
        buffer.add((byte)'p');
        buffer.add((byte)'i');
        buffer.add((byte)'n');
        buffer.add((byte)'g');

        // Send a thing
        client.write(buffer);

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

        public void onDataReceived(Vector<Byte> buffer) {
            // Called when data has been received from host
        }
    };
}
~~~

## Author

Nathan Sizemore, nathanrsizemore@gmail.com

## License

hydrogen-objc is available under the MPL-2.0 license. See the LICENSE file for more info.
