package ricm.channels.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import ricm.channels.IBroker;
import ricm.channels.IBrokerListener;

public class Broker implements IBroker {

	private Selector m_selector;
	private SelectionKey m_skey;
	private ServerSocketChannel m_ssc;
	private SocketChannel m_sc;
	private IBrokerListener m_bl;

	public Broker() throws IOException {
		// create a new selector
		m_selector = SelectorProvider.provider().openSelector();
	}

	@Override
	public void setListener(IBrokerListener l) {
		m_bl = l;
	}

	@Override
	public boolean connect(String host, int port) {
		try {
			// create a non-blocking server socket channel
			m_sc = SocketChannel.open();

			m_sc.configureBlocking(false);

			// register a CONNECT interest for channel sc
			m_skey = m_sc.register(m_selector, SelectionKey.OP_CONNECT);

			// request to connect to the server
			InetAddress addr;
			addr = InetAddress.getByName(host);
			m_sc.connect(new InetSocketAddress(addr, port));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean accept(int port) {
		try {
			// create a new non-blocking server socket channel
			m_ssc = ServerSocketChannel.open();
			m_ssc.configureBlocking(false);
			// bind the server socket to the given address and port
			InetAddress hostAddress;
			hostAddress = InetAddress.getByName("localhost");
			InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
			m_ssc.socket().bind(isa);

			// register a ACCEPT interest for channel ssc
			m_skey = m_ssc.register(m_selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void run() throws IOException {
		while (true) {
			// wait for some events
			m_selector.select();

			// get the keys for which the events occurred
			Iterator<?> selectedKeys = m_selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = (SelectionKey) selectedKeys.next();
				selectedKeys.remove();

				// process the event
				if (key.isValid() && key.isAcceptable()) // accept event
					handleAccept(key);
				if (key.isValid() && key.isReadable()) // read event
					handleRead(key);
				if (key.isValid() && key.isWritable()) // write event
					handleWrite(key);
				if (key.isValid() && key.isConnectable()) // connect event
					handleConnect(key);
			}
		}
	}

	private void handleConnect(SelectionKey key) throws IOException {
		assert (m_skey == key);
		assert (m_sc == key.channel());

		m_sc.finishConnect();
		Channel ch = new Channel(m_sc, m_selector);
		
		m_bl.connected(ch);
	}

	private void handleWrite(SelectionKey key) throws IOException {
		assert (m_skey != key);
		assert (m_ssc != key.channel());

		//transmit task to channel
		Channel chan = (Channel) key.attachment();
		chan.handleWrite();
	}

	private void handleRead(SelectionKey key) throws IOException {
		assert (m_skey != key);
		assert (m_ssc != key.channel());

		//transmit task to channel
		Channel chan = (Channel) key.attachment();
		chan.handleRead();

	}

	private void handleAccept(SelectionKey key) throws IOException {
		assert(key == m_skey);
		assert(m_ssc == key.channel());
		
		SocketChannel sc;

		// do the actual accept on the server-socket channel
		// get a client channel as result
		sc = m_ssc.accept();
		sc.configureBlocking(false);

		Channel ch = new Channel(sc, m_selector);
		m_bl.accepted(ch);
		
	}

}

