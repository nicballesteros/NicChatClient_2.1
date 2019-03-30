package com.nicballesteros.message.client.messagesender.userLookup;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagLayout;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import java.awt.Insets;
import javax.swing.JButton;
import java.awt.Font;
import java.awt.Color;

public class UserLookupWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextField txtSearch;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UserLookupWindow frame = new UserLookupWindow();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public UserLookupWindow() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		setTitle("Start Conversation");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 275, 200);
		//setSize(230, 200);
		getContentPane().setLayout(null);
		
		JLabel lblEnterName = new JLabel("Enter Name:");
		lblEnterName.setBounds(99, 11, 61, 14);
		getContentPane().add(lblEnterName);
		
		txtSearch = new JTextField();
		txtSearch.setBounds(81, 36, 96, 20);
		getContentPane().add(txtSearch);
		txtSearch.setColumns(10);
		
		JButton btnSend = new JButton("Send");
		btnSend.setBounds(101, 67, 56, 23);
		getContentPane().add(btnSend);
		
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
