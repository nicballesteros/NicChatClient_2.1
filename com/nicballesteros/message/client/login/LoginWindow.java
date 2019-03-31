package com.nicballesteros.message.client.login;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.nicballesteros.message.client.messagesender.MessageSenderWindow;
import com.nicballesteros.message.client.newuser.NewUserWindow;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

public class LoginWindow extends JFrame {
	//TODO include a hidden error message and show it when there is an error
	/**
	 * Everything for the login frame
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField userField;
	private JTextField ipField;
	private JTextField portField;
	private JPasswordField passwordField;
	
	private String password;
	private String username;
	private String ipAddress;
	private int port;

	private Thread send;
	
	private Login login;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LoginWindow frame = new LoginWindow();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}	
	
	private boolean sendDataToLogin() {
		username = userField.getText();
		password = new String(passwordField.getPassword());
		ipAddress = ipField.getText();
		port = Integer.parseInt(portField.getText());

		//TODO make a new thread that checks if the info provided was correct or not

		if(!username.equals("") && !password.equals("") && !ipAddress.equals("") && port > 0 && port < 10000){
			login = new Login(username, password, ipAddress, port);

			if(login.makeSocket()) {
				//TODO make this login.run(); and manage client and start listening
				login.receive();
				login.sendConnectionSignal();//

				//wait for pub and id to be send
				int time = 0;
				while(!login.isServerReadyForEncryption() && time < 100) { //waits for signal for one second
					try {
						Thread.sleep((long)10);
						//System.out.println("wait");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					time++;
				}
				if(time < 100){
					login.sendAES();
					//wait for confirmation
					time = 0;
					while(!login.isServerReadyForCreds() && time < 100) {
						//System.out.println("not ready");
						try {
							Thread.sleep((long)10);
							//System.out.println("wait");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						time++;
					}
					if(time < 100){
						try {
							login.sendUsername();
							login.sendPassword();
							//System.out.println("out of pass");
						}
						catch(Exception e) {
							e.printStackTrace();
						}

						time = 0;
						while(!login.getContinueToSender() && time < 100){
							try{
								Thread.sleep(10);
							}
							catch (Exception e){
								e.printStackTrace();
							}
						}

						if(login.getContinueToSender()){
							return true;
						}
					}
					else{
						System.out.println("Timed Out");
					}
				}
				else{
					System.out.println("Timed Out");
				}
			}
		}

		return false;
	}
	
	/**
	 * Create the frame.
	 */
	public LoginWindow() {
		//login = new Login();
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		setTitle("Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 300, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Username");
		lblNewLabel.setBounds(113, 39, 57, 14);
		contentPane.add(lblNewLabel);
		
		userField = new JTextField();
		userField.setBounds(94, 62, 96, 20);
		contentPane.add(userField);
		userField.setColumns(10);
		
		JLabel lblNewLabel_1 = new JLabel("Password");
		lblNewLabel_1.setBounds(110, 93, 64, 14);
		contentPane.add(lblNewLabel_1);
		
		JLabel lblNewLabel_2 = new JLabel("IP Address");
		lblNewLabel_2.setBounds(110, 149, 64, 14);
		contentPane.add(lblNewLabel_2);
		
		JLabel lblNewLabel_4 = new JLabel("New User?");
		lblNewLabel_4.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				newUserRun();
			}
		});
		lblNewLabel_4.setBounds(110, 320, 64, 14);
		contentPane.add(lblNewLabel_4);
		
		ipField = new JTextField();
		ipField.setBounds(94, 174, 96, 20);
		contentPane.add(ipField);
		ipField.setColumns(10);
		
		JLabel lblNewLabel_3 = new JLabel("Port");
		lblNewLabel_3.setBounds(128, 205, 27, 14);
		contentPane.add(lblNewLabel_3);
		
		portField = new JTextField();
		portField.setBounds(94, 230, 96, 20);
		contentPane.add(portField);
		portField.setColumns(10);
		
		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(sendDataToLogin()) {
					dispose();
					new MessageSenderWindow(login.getID(), login.getAESkey(), login.getAddress(), login.getPort());
					login.closeSocket();
				}
				else {
					clearText();
				}
			}
		});
		btnLogin.setBounds(97, 277, 89, 23);
		contentPane.add(btnLogin);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(94, 118, 96, 20);
		contentPane.add(passwordField);
		
		JLabel lblWelcome = new JLabel("Welcome");
		lblWelcome.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblWelcome.setBounds(110, 11, 64, 20);
		contentPane.add(lblWelcome);


		//TODO fix this
		userField.setText("test");
		passwordField.setText("1234");
		ipField.setText("192.168.2.89");
		portField.setText("9999");

		setVisible(true);
	}
	
	private void newUserRun() {
		dispose();
		new NewUserWindow();
		System.out.println("Press");
	}

	private void clearText(){
		this.userField.setText("");
		this.passwordField.setText("");
		this.ipField.setText("");
		this.portField.setText("");
	}
}
