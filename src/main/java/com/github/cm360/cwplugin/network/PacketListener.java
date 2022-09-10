package com.github.cm360.cwplugin.network;

@FunctionalInterface
public interface PacketListener {

	public void packetReceived(String data);

}
