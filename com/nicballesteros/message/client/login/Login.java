package com.nicballesteros.message.client.login;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Login {
	private String username;
	private String password;
	private InetAddress address;
	private int port;

	private int id;

	private DatagramSocket socket;
	
	private Thread send;
	private Thread receive;
	
	private boolean connected;
	private boolean running = true;
	private boolean continueToSender = false;
	private boolean quit = false;
	private boolean readyToSendCreds;
	private boolean publicKeyIsReceived;
	private boolean stopPepper = false;


	private PublicKey serverPublicKey;
	
	private SecretKeySpec AESkey;

	private byte[] hashedPassword;

	public Login(String username, String password, String address, int port) {
		this.username = username;
		this.password = password;
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

	public void disconnect(){
		socket.close();
		running = false;
	}


	public boolean failed(){
		return quit;
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

			if(conf == 24242) {
				readyToSendCreds = true;
			}
			else if(conf == 54321) {
				System.out.println("All is good!");
				//the user was registered in the database
				disconnect();
			}
			else if(conf == 34251){
				stopPepper = true;
				System.out.println("password accepted");
			}
			else if(conf == 55555){
				continueToSender = true;
				System.out.println("Client connected!!!");
				Thread.sleep(1000); //for networking
				closeSocket();
			}
			else {
				System.out.println("Wasnt 12345");
				quit = true;
				running = false;
			}
		}
		else if(encryption == (byte)25){
			quit = true;
			running = false;
		}
		else {
			System.out.println("Woh woh woh error on which form");
			quit = true;
		}
	}

	public void sendConnectionSignal() {
		byte[] out = { (byte)100, (byte)101};

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		try{
			output.write(out);
			output.write(username.getBytes());
		}
		catch (IOException i){
			i.printStackTrace();
		}

		sendPacket(output.toByteArray());
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

	public int getID(){
		return this.id;
	}

	public InetAddress getAddress(){
		return address;
	}

	public int getPort(){
		return port;
	}

	public SecretKeySpec getAESkey() {
		return AESkey;
	}

	private byte[] hashPassword(int index) {
		StringBuilder sb = new StringBuilder();
		sb.append("~p|`#");
		sb.append(password);
		sb.append((char)index);
		String precompiledPass = sb.toString();
		//System.out.println((char)index + " " + index +" : " + precompiledPass);
		//System.out.print(index + ": ");
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			//System.out.println(Base64.encodeBase64String(digest.digest(precompiledPass.getBytes())));
			//System.out.println(precompiledPass);
			return digest.digest(precompiledPass.getBytes());
		}
		catch (NoSuchAlgorithmException e) {
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



	public boolean isServerReadyForCreds() {
		return readyToSendCreds;
	}

	public boolean isServerReadyForEncryption() {
		return publicKeyIsReceived;
	}

	public boolean passAccepted(){
		return stopPepper;
	}

	public boolean getContinueToSender(){
		return this.continueToSender;
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

	public void sendAES() {
		byte[] out;

		try {
			byte[] bb = ByteBuffer.allocate(4).putInt(id).array();

			ByteArrayOutputStream output = new ByteArrayOutputStream();

			output.write((byte)101);
			output.write((byte)102);
			output.write(AESkey.getEncoded());

			out = encryptByteRSA(output.toByteArray());

			output = null;
			output = new ByteArrayOutputStream();

			output.write((byte)101);
			output.write(bb);
			output.write(out);

			//printByteDataToConsole("output", output.toByteArray());

			sendPacket(output.toByteArray());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void sendUsername() throws Exception{
		byte encryptionIdentifier = (byte)102;
		byte[] idToBytes = ByteBuffer.allocate(4).putInt(id).array();
		byte registeredUserIdentifier = (byte)101;
		byte usernameIdentifier = (byte)100;
		byte[] usernameBytes = username.getBytes();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(registeredUserIdentifier);
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
		byte registeredUserIdentifier = (byte)101;
		byte passwordIdentifier = (byte)101;

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(registeredUserIdentifier);
		output.write(passwordIdentifier);

		byte[] out = output.toByteArray();

		int i = 33;

		while(i < 256 && !stopPepper){
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			outputStream.write(out);
			outputStream.write(hashPassword(i));
			//System.out.println((char)(i+33) + " : " + Base64.encodeBase64String(hashPassword(i)));
			//printByteDataToConsole("Pass Hash", output.toByteArray());

			byte[] send = encryptByteAES(outputStream.toByteArray());

			ByteArrayOutputStream os = new ByteArrayOutputStream();

			os.write(encryptionIdentifier);
			os.write(idToBytes);
			os.write(send);

			//printByteDataToConsole("Pass Hash pt 2", output.toByteArray());

			sendPacket(os.toByteArray());
			Thread.sleep(10);
			i++;
		}
	}

	private void printByteDataToConsole(String name, byte[] data) {
		System.out.print(name + ": {");
		for(byte dat : data) {
			int num = dat;
			System.out.print(num + ", ");
		}
		System.out.println("}");
	}

	public void closeSocket(){
		running = false;
		try {
			Thread.sleep(2000);                //TODO fixthis
		}
		catch(Exception e){
			e.printStackTrace();
		}
		socket.close();
	}
}
