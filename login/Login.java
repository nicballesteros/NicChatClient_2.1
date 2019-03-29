package com.nicballesteros.message.client.login;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.codec.binary.Base64;

public class Login {
	private String username;
	private String password;
	private InetAddress address;
	private int port;
	
	private DatagramSocket socket;
	
	private Thread send;
	private Thread receive;
	
	private boolean connected;
	
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	private PublicKey serverPublicKey;
	
	private Cipher cipher;
	
	public Login(String username, char[] password, String address, int port) {
		this.username = username;
		this.password = password.toString();
		this.port = port;
		
		try {
			this.address = InetAddress.getByName(address);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		generateKeys();
	}
	
	private void generateKeys() {
		KeyPairGenerator keyGen;
		
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			
			KeyPair pair = keyGen.generateKeyPair();
			this.publicKey = pair.getPublic();
			this.privateKey = pair.getPrivate();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendPacket(final byte[] data) {
		send = new Thread("Send") {
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		send.start();
	}
	
	private String encrypt(String msg) {
		String encrypted;
		
		try {
			this.cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
		} 
		catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		
		
		try {
			encrypted = Base64.encodeBase64String(cipher.doFinal(Base64.decodeBase64(msg)));
		} 
		catch (Exception e) {
			e.printStackTrace();
			encrypted = "error";
		} 
		
		return encrypted;
	}
	
	private String hashPass() {
		
		String generatedPassword = null;
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			md.update(this.password.getBytes());
			
			byte[] bytes = md.digest();
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			
			generatedPassword = sb.toString();
		}
		catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return generatedPassword;
	}
	
	private byte[] receive() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		try {
			socket.receive(packet);
		}
		catch (Exception e){
			e.printStackTrace();
		}
			
		return packet.getData();
	}
	
	public boolean openConnection() {
		try {
			socket = new DatagramSocket();
		} 
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void sendPublic() {
		String data = "~c|`" + this.username + "~p|`" + new String(this.privateKey.getEncoded());
		sendPacket(data.getBytes());
	}
	
	public void sendPassword() {
		String passwordHashed = hashPass();
		send = null;
		
		String string = "~n|`" + this.username + "~p|`" + passwordHashed;
		string = encrypt(string);
		sendPacket(string.getBytes());
	}

	private boolean validKey = false;
	
	public boolean getServerPublic() {
		receive = new Thread("Receive") {
			public void run() {
				byte[] data = receive();
				String string = new String(data);

				if(string.startsWith("~p|`")) {
					string = string.substring(4);
					validKey = true;
				}
			}
		};
		receive.start();
			
		
		int i = 0;
		while(receive.isAlive() && i < 10) {
			try {
				Thread.sleep((long) 100);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			i++;
		}

		if(receive.isAlive()) {
			receive.interrupt();
		}
		
		return validKey;
	}
	
	public boolean getConfimation() {
		receive = null;
		
		boolean state = false;
		
		byte[] data = receive();
		String string = new String(data);

		if(string.equals("TRUE")) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
