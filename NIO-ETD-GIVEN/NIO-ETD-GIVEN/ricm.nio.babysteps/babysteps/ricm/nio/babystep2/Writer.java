package ricm.nio.babystep2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Writer {
	int state;
	public static int NEUTRAL = 0;
	public static int SENDING = 1;
	
	SocketChannel sc;
	ByteBuffer buffer_msg;
	ByteBuffer buffer_len;
	SelectionKey key;
	
	
	public Writer(SocketChannel socChan, SelectionKey k) {
		sc = socChan;
		key = k;
		state = NEUTRAL;
	}
	
	public void handleWrite() throws IOException {
		if (state == NEUTRAL) {
			sc.write(buffer_len);
			if (!buffer_len.hasRemaining()) {//Il ne reste plus rien
				state = SENDING;
			}
		}else {
			sc.write(buffer_msg);
			if (!buffer_msg.hasRemaining()) {//il ne reste plus rien
				state = NEUTRAL;
				key.interestOps(SelectionKey.OP_READ);
			}
			
		}
	}
	
	public void sendMsg(byte[] message) {
		buffer_len= ByteBuffer.allocate(4);
		buffer_msg = ByteBuffer.allocate(message.length);
		buffer_len.putInt(message.length);
		buffer_msg.put(message);
		state = NEUTRAL;
		key.interestOps(SelectionKey.OP_WRITE);
	}

	
}
