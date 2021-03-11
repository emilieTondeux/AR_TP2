package ricm.fileserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import ricm.channels.IBroker;
import ricm.channels.IBrokerListener;
import ricm.channels.IChannel;
import ricm.channels.IChannelListener;

/*
 * Basic FileServer implementation 
 * 	The file is entirely read in memory before sending it to client
 */

public class FileServerApplication implements IBrokerListener, IChannelListener {

	public static final int CHUNK_SIZE = 512;
	IBroker engine;
	String folder;
	int port;

	void panic(String msg, Exception ex) {
		ex.printStackTrace(System.err);
		System.err.println("PANIC: " + msg);
		System.exit(-1);
	}

	public FileServerApplication(IBroker engine, String folder, int port) throws Exception {
		this.port = port;
		this.engine = engine;
		this.folder = folder;
		if (!folder.endsWith(File.separator))
			this.folder = folder + File.separator;
		this.engine.setListener(this);
		if (!this.engine.accept(port)) {
			System.err.println("Refused accept on " + port);
			System.exit(-1);
		}
	}

	byte[] readFile(FileInputStream fis) throws IOException {
		byte[] bytes;
		int length = 0;
		int remaining = fis.available();
		if (remaining > CHUNK_SIZE) {
			length = CHUNK_SIZE;
		} else {
			length = remaining;
		}
		bytes = new byte[length];
		for (int nread = 0; nread < length;) {
			int r;
			try {
				r = fis.read(bytes, nread, length - nread);
				nread += r;
			} catch (IOException e) {
				return null;
			}
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(0);
		dos.write(bytes);
		bytes = os.toByteArray();
		os.close();
		return bytes;
	}

	File openFile(String filename) {
		File f = new File(folder + filename);
		if (!f.exists() || !f.isFile()) {
			return null;
		} else {
			return f;
		}
	}

	/**
	 * Callback invoked when a message has been received. The message is whole, all
	 * bytes have been accumulated.
	 * 
	 * Returns an error code if the request failed: -1: could not parse the request
	 * -2: file does not exist -3: unexpected error
	 * 
	 * @param channel
	 * @param bytes
	 */
	public void received(IChannel channel, byte[] request) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			try {
				InputStream is = new ByteArrayInputStream(request);
				DataInputStream dis = new DataInputStream(is);
				String filename;
				try {
					filename = dis.readUTF();
					System.out.println("FileServer - Receive request for downloading: " + filename);
				} catch (Exception ex) {
					dos.writeInt(-1); // could not parse the request
					return;
				}
				File f = openFile(filename);
				if (f == null) {
					dos.writeInt(-2); // requested file does not exist
				} else {
					FileInputStream fis = new FileInputStream(f);
					long cursor = 0;
					while (cursor < f.length()) {
						byte[] bytes = readFile(fis);
						channel.send(bytes);
						cursor += CHUNK_SIZE;
					}
					fis.close();
					dos.writeInt(-4); //All packets sent.
				}
			} catch (IOException ex) {
				ex.printStackTrace(System.err);
				dos.writeInt(-3);
			} finally {
				dos.close();
				byte[] bytes = os.toByteArray();
				channel.send(bytes);
			}
		} catch (Exception ex) {
			panic("unexpected exception", ex);
		}
	}

	@Override
	public void connected(IChannel c) {
		System.out.println("Unexpected connected");
		System.exit(-1);
	}

	@Override
	public void accepted(IChannel c) {
		System.out.println("Accepted");
		c.setListener(this);
	}

	@Override
	public void refused(String host, int port) {
		System.out.println("Refused " + host + ":" + port);
		System.exit(-1);
	}

	@Override
	public void closed(IChannel c, Exception e) {
		System.out.println("Client closed channel");
	}
}
