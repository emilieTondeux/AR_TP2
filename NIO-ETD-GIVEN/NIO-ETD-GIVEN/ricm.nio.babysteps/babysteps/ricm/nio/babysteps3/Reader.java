package ricm.nio.babysteps3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Reader {
	int state;
	public static int NEUTRAL = 0;
	public static int READING = 1;

	byte[] msg;
	ByteBuffer buffer_len;
	ByteBuffer buffer_msg;
	int msg_len;
	SocketChannel sc;
	SelectionKey key;
	
	public Reader(SocketChannel socChan, SelectionKey k) {
		sc = socChan;
		key = k;
		state = NEUTRAL;
		msg_len = 0;
		buffer_len = ByteBuffer.allocate(4);
	}
	
	public void handleRead() throws IOException {
		if (state == NEUTRAL) {
			sc.read(buffer_len);
			if (!buffer_len.hasRemaining()) {//Il ne reste plus rien
				state = READING;
				buffer_len.rewind();
				msg_len = buffer_len.getInt();//récupère la longueur de msg
				buffer_len.rewind();
				buffer_msg= ByteBuffer.allocate(msg_len);//permet de caper le buffer_msg à la longueur du message voulu
			}
		}else {
			sc.read(buffer_msg);
			if (!buffer_msg.hasRemaining()) {//Si on a lu tout le message
				byte[] message = new byte[msg_len];
				buffer_len.rewind();
				message = buffer_msg.array();
				buffer_msg.rewind();
				processByte(message);
				state = NEUTRAL;
				key.interestOps(SelectionKey.OP_WRITE);	
			}
			
		}
	}
	
	public void processByte(byte[] msg) {

		// Let's print the received message, assuming it is a UTF-8 string
		// since it is the format of the first message sent to the server.
		String message = new String(msg, Charset.forName("UTF-8"));
		System.out.println("NioClient received msg: " + msg.length);
		System.out.println(message);
		
		// send back the received message doubled
		Writer wr = new Writer(sc,key);
		key.attach(wr);
		wr.sendMsg(msg);
	}
	
}
