package com.nicballesteros.message.client.messagesender.userLookup;

import com.nicballesteros.message.client.messagesender.MessageSenderManager;

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

	private JLabel lblErrorMsg;
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

	/**
	 * Create the frame.
	 */
	public UserLookupWindow(MessageSenderManager manager) {
		this.manager = manager;
		
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
				System.out.println("send...");
				String inputName = txtSearch.getText();
				manager.requestIfClientExists(inputName);
				if(manager.doesClientExist() && manager.doesAcquaintanceAlreadyExist(inputName)){
					System.out.println("added");
					manager.displayNewAcquaintance(inputName);
					dispose();
				}
				else{
					errorMsg.setVisible(true);
				}
			}
		});

		lblErrorMsg = new JLabel("User does not exist or is already in your ");
		lblEnterName.setBounds(99, 11, 61, 14);
		getContentPane().add(lblEnterName);
		lblErrorMsg.setVisible(false);

		int code = 4;
		String message = "";
		Color color = Color.BLACK;
		
		if(code == 0) {
			message = "User exists";
			color = Color.GREEN;
		}
		else if (code == 1){
			message = "User does not exist";
			color = Color.RED;
		}
		
		JLabel lblMessage = new JLabel(message);
		lblMessage.setForeground(color);
		lblMessage.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblMessage.setBounds(66, 101, 127, 33);
		getContentPane().add(lblMessage);
		
		setVisible(true);
	}
}
