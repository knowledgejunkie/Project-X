package xinput.ftp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;

/*******************************************************************************
 * This is a support class for some of the example programs. It is a sample
 * implementation of the ProtocolCommandListener interface which just prints out
 * to a specified stream all command/reply traffic.
 * <p>
 ******************************************************************************/

class StringCommandListener implements ProtocolCommandListener {

	private StringWriter sWriter = new StringWriter();

	private PrintWriter pWriter = new PrintWriter(sWriter);

	public void protocolCommandSent(ProtocolCommandEvent event) {
		pWriter.print(event.getMessage());
		pWriter.flush();
	}

	public void protocolReplyReceived(ProtocolCommandEvent event) {
		pWriter.print(event.getMessage());
		pWriter.flush();
	}

	public String getMessages() {
		return sWriter.toString();
	}

	public void reset() {
		pWriter.close();
		try {
			sWriter.close();
		} catch (IOException e) {
			// do nothing
		}

		sWriter = new StringWriter();
		pWriter = new PrintWriter(sWriter);
	}
}