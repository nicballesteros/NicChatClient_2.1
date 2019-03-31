package com.nicballesteros.message.client.messagesender;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MessageSenderManager {
    //Signals
    private final static byte ENDSIGNAL = (byte)75;
    private final static byte REGISTEREDSIGNAL = (byte)101;
    private final static byte AESENCRYPTSIGNAL = (byte)102;
    private final static byte FIRSTSIGNAL = (byte)107;
    private final static byte DISCONNECTSIGNAL = (byte)108;
    //byte
    private byte[] idAsByte;

    private int id;
    private SecretKeySpec AESkey;
    private InetAddress ipAddress;
    private int port;
    //Threads
    private Thread send;
    private Thread receive;

    private DatagramSocket socket;
    //Booleans
    private boolean running;

    private List<String> acquaintacesName;
    private List<Integer> acquaintacesID;

    private String recipient;

    private MessageSenderWindow window;

    public MessageSenderManager(int id, SecretKeySpec AESkey, InetAddress ipAddress, int port, MessageSenderWindow window){
        this.id = id;
        this.AESkey = AESkey;
        this.ipAddress = ipAddress;
        this.port = port;
        this.acquaintacesName = new ArrayList<>();
        idAsByte = ByteBuffer.allocate(4).putInt(id).array();
        this.acquaintacesID = new ArrayList<>();
        this.acquaintanceMap = new HashMap<>();
        this.window = window;
        //Booleans
        running = true; //TODO disconnect and set this to false

        recipient = "";

        try {
            this.socket = new DatagramSocket();
            receive();
            sendFirstSignal();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        sendRequestForAcquaintedClients();
    }

    private void receive(){
        receive = new Thread("Receive"){
            @Override
            public void run(){
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

    private void whichFormOfEncryption(byte[] data){
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


        if(encryption == (byte)101) { //AES encryption
            data = decryptByteAES(data);
            byte[] extraData = null;
           // System.out.println(data.length);

            if(data.length > 4){
                extraData = Arrays.copyOfRange(data, 4, data.length);
                data = Arrays.copyOfRange(data, 0, 4);
            }


            ByteBuffer wrapped = ByteBuffer.wrap(data);
            int conf = wrapped.getInt();

            if(conf == 6789) {
                System.out.println("ready to receive messages");
            }
            else if(conf == 9876){
                System.out.println("Disconnected User");
                socket.close();
            }
            else if(conf == 7777){ //receive id
                byte[] id = Arrays.copyOfRange(extraData, 0, 4);
                extraData = Arrays.copyOfRange(extraData, 4, extraData.length);
                ByteBuffer bb = ByteBuffer.wrap(id);
                int intID = bb.getInt();
                String name = new String(extraData);
                //System.out.println(name);
                acquaintacesName.add(name);
                acquaintacesID.add(intID);
                acquaintanceMap.put(name, intID);
            }
            else if(conf == 999){ //client exists
                byte[] id = Arrays.copyOfRange(extraData, 0, 4);
                extraData = Arrays.copyOfRange(extraData, 4, extraData.length);
                clientExists = true;
                ByteBuffer bb = ByteBuffer.wrap(id);
                int intID = bb.getInt();
                acquaintacesID.add(intID);
                tempID = intID;
                String name = new String(extraData);
                acquaintacesName.add(name);
                acquaintanceMap.put(name, intID);
            }
            else if(conf == 2565){
                clientExists = false;
                System.out.println("Client either already exists or doesn't exist");
            }
            else if(conf == 11111){
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try{
                    output.write(extraData);

                    window.console(new String(output.toByteArray()));
                    System.out.println(new String(output.toByteArray()));
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            else if(conf == 22222){ //name of recipient
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try{
                    output.write(extraData);


                    String thisName = new String(output.toByteArray());
                    System.out.println(thisName);
                    if(!thisName.equals(recipient)){
                        if(acquaintacesName.contains(thisName)){
                            System.out.println("recipient: " + thisName);
                            recipient = thisName;
                            sendNewRecipient(acquaintanceMap.get(recipient));
                            window.selectUser(thisName);
                            window.clearConsole();
                        }
                        else{
                            requestIfClientExists(thisName);
                            if(doesClientExist()){
                                System.out.println("hey");
                            }
                        }
                    }
                    else{
                        //window.selectUser(recipient);
                        //window.clearConsole();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
            else {
                System.out.println("Wasnt 12345");
                //quit = true;
                running = false;
            }
        }
        else if(encryption == (byte)25){
            running = false;
        }
        else {
            System.out.println("Woh woh woh error on which form");
            //quit = true;
        }
    }

    private int tempID = -1;

    private void send(byte[] data){
        send = new Thread("Send"){
            @Override
            public void run(){
                ByteArrayOutputStream bb = new ByteArrayOutputStream();

                try {
                    bb.write(data);
                    bb.write(ENDSIGNAL);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                byte[] out = bb.toByteArray();
                DatagramPacket packet = new DatagramPacket(out, out.length, ipAddress, port);

                try {
                    socket.send(packet);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        send.start();
    }

    public void sendFirstSignal() throws Exception{
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write(REGISTEREDSIGNAL);
        output.write(FIRSTSIGNAL);
        output.write((byte)0);

        byte[] data = encryptByteAES(output.toByteArray());

        output = new ByteArrayOutputStream();
        output.write(AESENCRYPTSIGNAL);
        output.write(idAsByte);
        output.write(data);

        send(output.toByteArray());
    }

    private void sendDisconnectSignal() throws Exception{
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write(REGISTEREDSIGNAL);
        output.write(DISCONNECTSIGNAL);
        output.write((byte)0);

        byte[] data = encryptByteAES(output.toByteArray());

        output = new ByteArrayOutputStream();
        output.write(AESENCRYPTSIGNAL);
        output.write(idAsByte);
        output.write(data);

        send(output.toByteArray());
    }

    private void sendRequestForAcquaintedClients(){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        try{
            finalOut.write(AESENCRYPTSIGNAL);
            finalOut.write(idAsByte);

            output.write(REGISTEREDSIGNAL);
            output.write((byte)106);
            output.write((byte)0);
            finalOut.write(encryptByteAES(output.toByteArray()));

            send(finalOut.toByteArray());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendNewRecipient(int nameID){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();

        byte[] nameIDasByte = ByteBuffer.allocate(4).putInt(nameID).array();
        try{
            finalOut.write(AESENCRYPTSIGNAL);
            finalOut.write(idAsByte);

            output.write(REGISTEREDSIGNAL);
            output.write((byte)103);
            output.write(nameIDasByte);
            finalOut.write(encryptByteAES(output.toByteArray()));

            send(finalOut.toByteArray());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendNewMessage(String msg){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();


        try{
            finalOut.write(AESENCRYPTSIGNAL);
            finalOut.write(idAsByte);

            output.write(REGISTEREDSIGNAL);
            output.write((byte)104);
            output.write(msg.getBytes());
            finalOut.write(encryptByteAES(output.toByteArray()));

            send(finalOut.toByteArray());
        }
        catch (Exception e){
            e.printStackTrace();
        }
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

    public void disconnect(){
        try {
            sendDisconnectSignal();
            running = false;
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private DefaultListModel listModel;

    public void updateList(DefaultListModel<String> listModel){
        this.listModel = listModel;
    }

    public void displayNewAcquaintance(String name){
        listModel.addElement(name);
    }

    public boolean doesAcquaintanceAlreadyExist(String name){
//        for(String Aname : acquaintacesName){
//            System.out.println(Aname);
//        }

        return acquaintacesName.contains(name);
    }

    public String[] getListData(){
        String[] output = new String[acquaintacesName.size()];
        output = acquaintacesName.toArray(output);

        return output;
    }
    private String tempName;
    public void requestIfClientExists(String name){
        clientExists = false;
        tempName = name;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        try{
            finalOut.write(AESENCRYPTSIGNAL);
            finalOut.write(idAsByte);

            output.write(REGISTEREDSIGNAL);
            output.write((byte)41);
            output.write(name.getBytes());
            finalOut.write(encryptByteAES(output.toByteArray()));

            send(finalOut.toByteArray());
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private boolean clientExists;
    private HashMap<String, Integer> acquaintanceMap;

    public boolean doesClientExist(){
        /**
         * A listener to see whether the server has responded to requestIfClientExists()
         */
        int time = 0;
        while(!clientExists && time < 100){ //1 second timeout
            try{
                Thread.sleep(10);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            time++;
        }
        if(time < 100){ //if didnt time out
            acquaintanceMap.put(tempName, tempID);
            acquaintacesName.add(tempName);
            tempID = -1;
            tempName = null;
            return true;
        }
        tempName = null;
        return false;
    }

    public void getID(int index){
        Object obj = listModel.get(index);
        String name = obj.toString();
        //System.out.println(name);

        int nameID = acquaintanceMap.get(name);
        System.out.println(nameID);
        if(nameID != -1 && !name.equals(recipient)){
            recipient = name;
            sendNewRecipient(nameID);
            window.clearConsole();
        }
        else{
            System.out.println("Error in getid");
        }
    }

    public int getIndexOfListModel(String name){
        Object n = name;
        return listModel.indexOf(n);
    }

}
