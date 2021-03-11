package ricm.channels.nio;

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
	Channel channel;
	
	public Reader(SocketChannel socChan, SelectionKey k, Channel chan) {
		sc = socChan;
		key = k;
		channel = chan;
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
			}
			
		}
	}
	
	public void processByte(byte[] msg) {
		channel.m_chanList.received(channel, msg);
	}

}
