package com.github.cm360.cwplugin.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.md_5.bungee.api.plugin.Plugin;

public class DatagramEndpoint {

	private DatagramSocket socket;
	private InetAddress sendAddress;
	private int sendPort;
	private long guildId;
	private Map<String, PacketListener> packetListeners;
	
	public DatagramEndpoint(Plugin plugin, InetAddress bindAddress, int bindPort, InetAddress sendAddress, int sendPort, long guildId) throws Exception {
		packetListeners = new HashMap<String, PacketListener>();
		this.sendAddress = sendAddress;
		this.sendPort = sendPort;
		this.guildId = guildId;
		// Bind UDP socket
		socket = new DatagramSocket(bindPort, bindAddress);
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			while (!socket.isClosed()) {
				try {
					// Read UDP packet into buffer
					byte[] buffer = new byte[4096];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					// Decode with UTF-8 charset
					String data = new String(truncate(packet.getData(), packet.getLength()), StandardCharsets.UTF_8);
//					System.out.printf("Received '%s'\nLength: %d\n", data, packet.getLength());
					// Call appropriate listener
					String[] dataSplit = data.split("\0", 2);
					packetListeners.get(dataSplit[0]).packetReceived(dataSplit[1]);
				} catch (IOException e) {
					if (!socket.isClosed())
						e.printStackTrace();
				}
			}
		});
	}
	
	public void registerListener(String type, PacketListener listener) {
		packetListeners.put(type, listener);
	}
	
	public boolean send(String type, String data) {
		try {
			String payloadString = String.join("\0", Long.toString(guildId), type, data);
			byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
			DatagramPacket packet = new DatagramPacket(payload, payload.length);
			packet.setAddress(sendAddress);
			packet.setPort(sendPort);
			socket.send(packet);
//			System.out.printf("Sent '%s', Length: %d/%d\n", payloadString, payloadString.length(), payload.length);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	// Modified from http://www.java2s.com/example/java-utility-method/array-truncate/truncate-byte-array-int-newlength-63bb1.html
	public byte[] truncate(byte[] input, int length) {
		if (input.length < length) {
			return input;
		} else {
			byte[] truncated = new byte[length];
			System.arraycopy(input, 0, truncated, 0, length);
			return truncated;
		}
	}
	
	public void close() {
		socket.close();
	}
	
}
