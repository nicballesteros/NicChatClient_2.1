package com.nicballesteros.message.client.messagesender.userLookup;

import com.nicballesteros.message.client.messagesender.MessageSenderManager;
import com.nicballesteros.message.client.messagesender.MessageSenderWindow;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIManager;

import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserLookupWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextField txtSearch;
	
	private JLabel lblErrorMsg1;
	private JLabel lblErrorMsg2;
	private JLabel lblEnterName;

	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					UserLookupWindow frame = new UserLookupWindow();
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	private MessageSenderManager manager;
	private MessageSenderWindow window;

	/**
	 * Create the frame.
	 */
	public UserLookupWindow(MessageSenderManager manager, MessageSenderWindow window) {
		this.manager = manager;
		this.window = window;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		setTitle("Start Conversation");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 275, 200);
		//setSize(230, 200);
		getContentPane().setLayout(null);

		lblEnterName = new JLabel("Enter Name:");
		lblEnterName.setBounds(99, 11, 61, 14);
		getContentPane().add(lblEnterName);
		
		txtSearch = new JTextField();
		txtSearch.setBounds(81, 36, 96, 20);
		getContentPane().add(txtSearch);
		txtSearch.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.setBounds(101, 67, 56, 23);
		getContentPane().add(btnSend);
		
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lblErrorMsg1.setVisible(false);
				lblErrorMsg2.setVisible(false);

				System.out.println("send...");
				String inputName = txtSearch.getText();
				manager.requestIfClientExists(inputName);
				if(manager.doesClientExist() && manager.doesAcquaintanceAlreadyExist(inputName)){
					System.out.println("added");
					manager.displayNewAcquaintance(inputName);
					window.enableWindow();
					dispose();
				}
				else{
					lblErrorMsg1.setVisible(true);
					lblErrorMsg2.setVisible(true);
				}
			}
		});
		
		lblErrorMsg1 = new JLabel("User either does not exist or ");
		lblErrorMsg1.setForeground(Color.RED);
		lblErrorMsg1.setBounds(59, 102, 141, 20);
		getContentPane().add(lblErrorMsg1);
		
		lblErrorMsg2 = new JLabel("is already in your inbox");
		lblErrorMsg2.setForeground(Color.RED);
		lblErrorMsg2.setBounds(70, 123, 118, 14);
		getContentPane().add(lblErrorMsg2);
		lblErrorMsg1.setVisible(false);
		lblErrorMsg2.setVisible(false);

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				window.enableWindow();
			}
		});

		setVisible(true);
	}

	
}
