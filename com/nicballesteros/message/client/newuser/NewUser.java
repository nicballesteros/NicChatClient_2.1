package com.nicballesteros.message.client.newuser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class NewUser {

	private DatagramSocket socket;
	
	private Thread send;
	private Thread receive;
	
	private PublicKey serverPublicKey;
	
	private String username;
	private String password;
	private InetAddress address;
	private int port;
	
	private byte[] hashedPassword;
	
	private int id;
	
	private SecretKeySpec AESkey;
	
	private boolean publicKeyIsReceived;
	private boolean running;
	private boolean readyToSendCreds;

	//TODO if the user is made correctly, take the user pass ip and port and enter into the login window automagically

	public NewUser(String username, String password, InetAddress address, int port) {
		this.username = username;
		this.password = password;
		this.address = address;
		this.port = port;
		this.running = true;
		this.readyToSendCreds = false;
		this.publicKeyIsReceived = false;
		hashPassword();
		generateKeys();
	}
	
	private char randomChar() {
		Random rand = new Random();
		//0-223
		int n = rand.nextInt(223);
		//33-256
		n += 33;
		System.out.println("char" + (char)n);
		System.out.println("num" + n);
		return (char)n;
	}
	
	private void hashPassword() {
		StringBuilder sb = new StringBuilder();
		sb.append("~p|`#");
		sb.append((password));
		sb.append(randomChar());
		String precompiledPass = sb.toString();
		System.out.println(precompiledPass);
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			hashedPassword = digest.digest(precompiledPass.getBytes());
		} 
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		System.out.println(Base64.encodeBase64String(hashedPassword));
	}
	
	public byte[] getAESkeyBytes() {
		return AESkey.getEncoded();
	}
	
	private byte[] encryptByteRSA(byte[] rsa) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
			return cipher.doFinal(rsa);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private byte[] encryptByteAES(byte[] msg) {
		try {
			Cipher aescipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			aescipher.init(Cipher.ENCRYPT_MODE, AESkey);
			return aescipher.doFinal(msg);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private byte[] decryptByteAES(byte[] data) {
		try {
			Cipher aescipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			aescipher.init(Cipher.DECRYPT_MODE, AESkey);
			return aescipher.doFinal(data);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void sendPacket(final byte[] data) {
		send = new Thread("Send") {
			public void run() {
				ByteArrayOutputStream bb = new ByteArrayOutputStream();
				try {
					bb.write(data);
					bb.write((byte)75);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				byte[] out = bb.toByteArray();
				DatagramPacket packet = new DatagramPacket(out, out.length, address, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		send.start();
	}
	
	public void sendConnectionSignal() {
		byte[] out = { (byte)100, (byte)100};
		sendPacket(out);
	}
	
	public boolean makeSocket() {
		try {
			socket = new DatagramSocket();
			return true;
		} 
		catch (SocketException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	private void generateKeys() {
		//AES
		//random 64 byte string
		byte[] key = new byte[16];
		new Random().nextBytes(key);
		
		MessageDigest sha = null;
		
		try {
			sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			AESkey = new SecretKeySpec(key, "AES");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void disconnect(){
		socket.close();
		running = false;
	}

	public boolean isServerReadyForCreds() {
		return readyToSendCreds;
	}
	
	public boolean isServerReadyForEncryption() {
		return publicKeyIsReceived;
	}

	private boolean quit = false;

	private void whichFormOfEncryption(byte[] data) throws Exception {
		byte encryption = data[0];
		data = Arrays.copyOfRange(data, 1, data.length);
		
		int cutoff = 0;
		for(int j = data.length - 1; j > 0; j--) {
			if(data[j] == (byte)75) {
				cutoff = j;
				break;
			}
		}
		
		data = Arrays.copyOfRange(data, 0, cutoff);
		
		if(encryption == (byte)100) { //no encryption
			//Get the id

			byte[] idBytes = Arrays.copyOfRange(data, 0, 4);
			ByteBuffer wrapped = ByteBuffer.wrap(idBytes);
			id = wrapped.getInt();

			data = Arrays.copyOfRange(data, 4, data.length);
			//Get the pub key
			serverPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data));

			publicKeyIsReceived = true;
		}
		else if(encryption == (byte)101) { //AES encryption
			data = decryptByteAES(data);
			ByteBuffer wrapped = ByteBuffer.wrap(data);
			int conf = wrapped.getInt();
			if(conf == 12345) {
				readyToSendCreds = true;
			}
			else if(conf == 54321) {
				System.out.println("All is good!");
				//the user was registered in the database
				disconnect();
			}
			else {
				System.out.println("Wasnt 12345");
			}
		}
		else {
			System.out.println("Woh woh woh error on which form");
		}
	}
	
	public void receive() {
		receive = new Thread("Receive") {
			public void run() {
				DatagramPacket packet;
				while(running) {
					byte[] data = new byte[1024];
					packet = new DatagramPacket(data, data.length);
					
					try {
						socket.receive(packet);
						
						whichFormOfEncryption(packet.getData());
					}
					catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		receive.start();
	}

	
	private void printByteDataToConsole(String name, byte[] data) {
		System.out.print(name + ": {");
		for(byte dat : data) {
			int num = dat;
			System.out.print(num + ", ");
		}
		System.out.println("}");
	}
	
	public void sendAES() {	
		byte[] out;
		
		try {
			byte[] bb = ByteBuffer.allocate(4).putInt(id).array();
			
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			
			output.write((byte)100);
			output.write((byte)102);
			output.write(AESkey.getEncoded());

			out = encryptByteRSA(output.toByteArray());

			output = null;
			output = new ByteArrayOutputStream();
			
			output.write((byte)101);
			output.write(bb);
			output.write(out);
			
			sendPacket(output.toByteArray());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendUsername() throws Exception{
		byte encryptionIdentifier = (byte)102;
		byte[] idToBytes = ByteBuffer.allocate(4).putInt(id).array();
		byte newUserIdentifier = (byte)100;
		byte usernameIdentifier = (byte)100;
		byte[] usernameBytes = username.getBytes();
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(newUserIdentifier);
		output.write(usernameIdentifier);
		output.write(usernameBytes);
		
		//printByteDataToConsole("Username", output.toByteArray());
		
		byte[] out = encryptByteAES(output.toByteArray());
		
		output = null;
		output = new ByteArrayOutputStream();
		output.write(encryptionIdentifier);
		output.write(idToBytes);
		output.write(out);
		
		//printByteDataToConsole("Username pt2", output.toByteArray());
		
		sendPacket(output.toByteArray());
	}
	
	public void sendPassword() throws Exception{
		byte encryptionIdentifier = (byte)102;
		byte[] idToBytes = ByteBuffer.allocate(4).putInt(id).array();
		byte newUserIdentifier = (byte)100;
		byte passwordIdentifier = (byte)101;
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(newUserIdentifier);
		output.write(passwordIdentifier);
		output.write(hashedPassword);
		
		//printByteDataToConsole("Pass Hash", output.toByteArray());
		
		byte[] out = encryptByteAES(output.toByteArray());
		
		output = null;
		output = new ByteArrayOutputStream();
		output.write(encryptionIdentifier);
		output.write(idToBytes);
		output.write(out);
		
		//printByteDataToConsole("Pass Hash pt 2", output.toByteArray());
		
		sendPacket(output.toByteArray());
	}
	
}
