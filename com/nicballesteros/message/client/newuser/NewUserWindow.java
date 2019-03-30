package com.nicballesteros.message.client.newuser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.nicballesteros.message.client.login.LoginWindow;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class NewUserWindow extends JFrame {

	private JPanel contentPane;
	
	private static final long serialVersionUID = 1L;
	private JTextField userField;
	private JTextField ipField;
	private JTextField portField;
	private JPasswordField passfield;
	private JPasswordField confirmpassfield;

	private String username;
	private String password;
	private InetAddress address;
	private int port;
	
	
	
	private NewUser newUser;
	
	/**
	 * Create the frame.
	 */
	private void makeWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		setTitle("New User");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 300, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("New Username");
		lblNewLabel.setBounds(103, 11, 74, 14);
		contentPane.add(lblNewLabel);
		
		userField = new JTextField();
		userField.setBounds(94, 36, 96, 20);
		contentPane.add(userField);
		userField.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("New Password");
		lblNewLabel_1.setBounds(107, 67, 70, 14);
		contentPane.add(lblNewLabel_1);
		
		passfield = new JPasswordField();
		passfield.setBounds(94, 92, 96, 20);
		contentPane.add(passfield);
		
		JLabel lblConfirmPassword = new JLabel("Confirm Password");
		lblConfirmPassword.setBounds(98, 123, 88, 14);
		contentPane.add(lblConfirmPassword);
		
		confirmpassfield = new JPasswordField();
		confirmpassfield.setBounds(94, 148, 96, 20);
		contentPane.add(confirmpassfield);
		
		JLabel lblNewLabel_2 = new JLabel("IP Address");
		lblNewLabel_2.setBounds(113, 179, 64, 14);
		contentPane.add(lblNewLabel_2);
		
		ipField = new JTextField();
		ipField.setBounds(94, 204, 96, 20);
		contentPane.add(ipField);
		ipField.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("Port");
		lblNewLabel_3.setBounds(129, 235, 27, 14);
		contentPane.add(lblNewLabel_3);
		
		portField = new JTextField();
		portField.setBounds(94, 260, 96, 20);
		contentPane.add(portField);
		portField.setColumns(10);
		
		JButton btnRegister = new JButton("Register");
		btnRegister.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					register();
				}
			}
		});
		btnRegister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				register(); //register button
			}
		});
		btnRegister.setBounds(97, 313, 89, 23);
		contentPane.add(btnRegister);
		
		setVisible(true);
	}
	
	public NewUserWindow() {
		makeWindow();
	}



	private boolean registerNewUser() {
		/**
		 * Returns true if the user was made
		 * Returns false if the user was not able to be made
		 */
		
		username = userField.getText();
		this.password = new String(passfield.getPassword());
		port = Integer.parseInt(portField.getText());

		String confirmPass = new String(confirmpassfield.getPassword());
		
		try {
			address = InetAddress.getByName(ipField.getText());
		} 
		catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println(password);

		if(password.equals(confirmPass) && !username.equals("") && !password.equals("") && !address.equals("") && port > 0 && port < 10000 ) {
			newUser = new NewUser(username, password, address, port);

			if(newUser.makeSocket()) {
				newUser.receive();
				newUser.sendConnectionSignal();
				//TODO add a signal from the server to know that the connection signal made it successfully
				
				//wait for pub and id to be send
				while(!newUser.isServerReadyForEncryption()) {
					try {
						Thread.sleep((long)10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				newUser.sendAES();
				//wait for confirmation
				while(!newUser.isServerReadyForCreds()) {
					//System.out.println("not ready");
					try {
						Thread.sleep((long)10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				try {
					newUser.sendUsername();
					newUser.sendPassword();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		
		return false;
	}
	
	private void register() {
		if(registerNewUser()) {
			dispose();
			new LoginWindow();
		}
		else {
			newUser = null;
		}
	}
}
