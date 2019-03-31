package com.nicballesteros.message.client.messagesender;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import com.nicballesteros.message.client.messagesender.userLookup.UserLookupWindow;

import java.awt.GridBagLayout;

import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.*;
import java.net.InetAddress;

public class MessageSenderWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtMessage;
	private JList list;
	private JButton btnNewMessage;
	private JTextArea txtrHistory;
	private DefaultCaret caret;
	private JLabel lblNewLabel;
	private JLabel lblUser;

	private int id;
	private SecretKeySpec AESkey;
	private InetAddress ipAddress;
	private int port;

	private MessageSenderManager manager;

	private DefaultListModel<String> listModel;


	/**
	 * Create the frame.
	 */
	public MessageSenderWindow(int id, SecretKeySpec AESkey, InetAddress ipAddress, int port) {
		//TODO when the client closes, set the user to not connected

		manager = new MessageSenderManager(id, AESkey, ipAddress, port, this);

		createWindow();
	}

	private void createWindow(){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Message");
		setLocationRelativeTo(null);
		//setBounds(100, 100, 820, 540);
		setSize(830, 550);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setSize(800,500);
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{110, 630, 60}; //800
		gbl_contentPane.rowHeights = new int[]{25, 450, 20}; //500
		gbl_contentPane.columnWeights = new double[]{1.0, 1.0};
		gbl_contentPane.rowWeights = new double[]{1.0, 1.0};
		contentPane.setLayout(gbl_contentPane);

		//String[] listData = {"one", "two", "three", "four", "four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four","four"};

		lblNewLabel = new JLabel("Messages");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 15));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPane.add(lblNewLabel, gbc_lblNewLabel);

		lblUser = new JLabel("User");
		lblUser.setFont(new Font("Tahoma", Font.PLAIN, 15));
		GridBagConstraints gbc_lblUser = new GridBagConstraints();
		gbc_lblUser.insets = new Insets(0, 0, 5, 5);
		gbc_lblUser.gridx = 1;
		gbc_lblUser.gridy = 0;
		contentPane.add(lblUser, gbc_lblUser);

		//for networking
		try {
			Thread.sleep(100);
		}
		catch (Exception e){
			e.printStackTrace();
		}

		listModel = new DefaultListModel<>();

		String[] arr = manager.getListData();

		for(String string : arr){
			listModel.addElement(string);
		}

		list = new JList(listModel);
		list.setFont(new Font("Tahoma", Font.PLAIN, 15)); //TODO request for data

		manager.updateList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//list.setSelectedIndex(2);
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				JList list = (JList)evt.getSource();
				if(evt.getClickCount() == 1){
					int index = list.locationToIndex(evt.getPoint());
					manager.getID(index);
				}
			}
		});

		JScrollPane scrollList = new JScrollPane(list);
		GridBagConstraints gbc_scrollListConstraints = new GridBagConstraints();
		gbc_scrollListConstraints.insets = new Insets(0, 0, 5, 5);
		gbc_scrollListConstraints.fill = GridBagConstraints.BOTH;
		gbc_scrollListConstraints.gridx = 0;
		gbc_scrollListConstraints.gridy = 1;
		contentPane.add(scrollList, gbc_scrollListConstraints);

		txtrHistory = new JTextArea();
		txtrHistory.setEditable(false);
		caret = (DefaultCaret)txtrHistory.getCaret();
		JScrollPane scrollHistory = new JScrollPane(txtrHistory);
		txtrHistory.setFont(new Font("Tahoma", Font.BOLD, 13));
		GridBagConstraints gbc_scrollConstraints = new GridBagConstraints();
		gbc_scrollConstraints.insets = new Insets(0, 0, 5, 0);
		gbc_scrollConstraints.fill = GridBagConstraints.BOTH;
		gbc_scrollConstraints.gridx = 1;
		gbc_scrollConstraints.gridy = 1;
		gbc_scrollConstraints.gridwidth = 2;
		contentPane.add(scrollHistory, gbc_scrollConstraints);

		btnNewMessage = new JButton("New Message");
		btnNewMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabled(false);
				makeDialog();
			}
		});
		GridBagConstraints gbc_btnNewMessage = new GridBagConstraints();
		gbc_btnNewMessage.insets = new Insets(0, 0, 0, 5);
		gbc_btnNewMessage.gridx = 0;
		gbc_btnNewMessage.gridy = 2;
		contentPane.add(btnNewMessage, gbc_btnNewMessage);

		txtMessage = new JTextField();
		txtMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					String msg = txtMessage.getText();
					send(msg);
				}
//				else if(e.getKeyCode() == KeyEvent.VK_N) {
//					console("ctrl n");
//				}
			}
		});
		GridBagConstraints gbc_txtMessage = new GridBagConstraints();
		gbc_txtMessage.insets = new Insets(0, 0, 0, 5);
		gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMessage.gridx = 1;
		gbc_txtMessage.gridy = 2;
		contentPane.add(txtMessage, gbc_txtMessage);
		txtMessage.setColumns(10);

		JButton btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String msg = txtMessage.getText();
				send(msg);
			}
		});
		GridBagConstraints gbc_btnSend = new GridBagConstraints();
		gbc_btnSend.gridx = 2;
		gbc_btnSend.gridy = 2;
		contentPane.add(btnSend, gbc_btnSend);

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				manager.disconnect();
			}
		});

		setVisible(true);

		txtMessage.requestFocusInWindow();
	}

	public void console(String text) {
		txtrHistory.append(text + "\n");
		txtrHistory.setCaretPosition(txtrHistory.getDocument().getLength());
	}
	
	private void send(String msg) {
		if(msg.equals("")) {
			txtMessage.setText("");
			txtMessage.requestFocusInWindow();
		}
		else {
			console(msg);//TODO edit this out maybe
			manager.sendNewMessage(msg);
		}
		txtMessage.setText("");
		txtMessage.requestFocusInWindow();
	}

	private void makeDialog(){
		new UserLookupWindow(manager, this);
	}

	public void enableWindow(){
		setEnabled(true);
	}

	public void selectUser(String name){ //TODO make this work
		int index = -1;
		for(int i = 0; i < listModel.size(); i++){
			if(listModel.get(i).equals(name)){
				index = i;

				break;
			}
		}
		System.out.println(index);
		if(index != -1){
			list.setSelectedIndex(index);
		}
	}

	public void clearConsole(){
		txtrHistory.setText(null);
	}

}
