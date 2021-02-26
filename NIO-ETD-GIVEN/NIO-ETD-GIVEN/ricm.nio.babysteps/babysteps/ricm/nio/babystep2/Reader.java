package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

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
				msg_len = buffer_len.getInt();//récupère la longueur de msg
				buffer_msg= ByteBuffer.allocate(msg_len);//permet de caper le buffer_msg à la longueur du message voulu
			}
		}else {
			sc.read(buffer_msg);
			if (!buffer_msg.hasRemaining()) {//Si on a lu tout le message
				byte[] message = new byte[msg_len];
				message = buffer_msg.array();
				processByte(message);
				state = NEUTRAL;
				key.interestOps(SelectionKey.OP_WRITE);	
			}
			
		}
	}
	
	public void processByte(byte[] msg) {
		System.out.println(msg);
	}
	
}
