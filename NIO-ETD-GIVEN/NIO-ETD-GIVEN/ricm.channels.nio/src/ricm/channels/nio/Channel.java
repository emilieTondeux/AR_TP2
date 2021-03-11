package ricm.channels.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import ricm.channels.IChannel;
import ricm.channels.IChannelListener;

public class Channel implements IChannel {
	SocketChannel m_sc;
	SelectionKey m_skey;

	Reader m_reader;
	Writer m_writer;

	IChannelListener m_chanList;

	public Channel(SocketChannel sc, Selector select) throws ClosedChannelException {
		m_sc = sc;
		m_skey = m_sc.register(select, SelectionKey.OP_READ);
		m_skey.attach(this);

		m_reader = new Reader(m_sc, m_skey, this);
		m_writer = new Writer(m_sc, m_skey, this);

	}

	@Override
	public void setListener(IChannelListener l) {
		m_chanList = l;

	}

	@Override
	public void send(byte[] bytes, int offset, int count) {
		byte newBytes[] = new byte[count];
		for (int i = 0; i < count; i++) {
			newBytes[i] = bytes[offset + i];
		}
		m_writer.sendMsg(bytes);

	}

	@Override
	public void send(byte[] bytes) {
		m_writer.sendMsg(bytes);
	}

	/* closes the channel, the other side will know when he will attempt tu use the channel.
	 * */
	
	@Override
	public void close() {
		try {
			m_sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean closed() {
		return !m_sc.isOpen();
	}

	public void handleRead(){
		try {
			m_reader.handleRead();
		} catch (IOException e) { //Communication is closed
			m_chanList.closed(this, e);
			m_skey.interestOps(0);
		}
	}

	public void handleWrite(){
		try {
			m_writer.handleWrite();
		} catch (IOException e) { //Communication is closed
			m_chanList.closed(this, e);
			m_skey.interestOps(0);
		}
	}

}
