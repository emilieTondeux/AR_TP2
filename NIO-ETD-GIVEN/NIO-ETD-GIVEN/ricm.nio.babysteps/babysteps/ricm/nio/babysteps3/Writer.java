package ricm.nio.babysteps3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Vector;

public class Writer {
	public int state;
	public static int NEUTRAL = 0;
	public static int WRITING_LEN =1;
	public static int WRITING_MSG = 2;
	
	SocketChannel sc;
	Vector<byte[]> list_messages;
	ByteBuffer buffer_msg;
	ByteBuffer buffer_len;
	SelectionKey key;
	
	
	public Writer(SocketChannel socChan, SelectionKey k) {
		sc = socChan;
		key = k;
		state = NEUTRAL;
		list_messages = new Vector<byte[]>();
	}
	
	public void handleWrite() throws IOException {
		if (state == WRITING_LEN) {
			sc.write(buffer_len);
			if (!buffer_len.hasRemaining()) {//Il ne reste plus rien
				state = WRITING_MSG;
				buffer_len.rewind();
			}
		}else if (state == WRITING_MSG){
			sc.write(buffer_msg);
			if (!buffer_msg.hasRemaining()) {//il ne reste plus rien
				buffer_msg.rewind();
				if (list_messages.isEmpty()) {//si il n'y a plus de messages à envoyer dans la liste de messages
					state = NEUTRAL;
					key.interestOps(SelectionKey.OP_READ);
				}else {
					init_message();
				}
			}
		}
	}
	
	public boolean isDone() {
		return state == NEUTRAL;
	}
	
	public void init_message() {
		buffer_len= ByteBuffer.allocate(4);
		byte[] msg = list_messages.get(0);
		buffer_msg = ByteBuffer.allocate(msg.length);
		buffer_len.putInt(msg.length);
		buffer_len.rewind();
		buffer_msg.put(msg);
		buffer_msg.rewind();
		list_messages.remove(0);
		state = WRITING_LEN;
	}
	
	public void sendMsg(byte[] message) {
		list_messages.add(message);
		if(state == NEUTRAL) {
			init_message();	
			state = WRITING_LEN;
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	
}
