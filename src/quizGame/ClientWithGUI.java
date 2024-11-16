package quizGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class ClientWithGUI {

    private JFrame frame; // make frame for view UI
    private JTextArea serverMessageArea; // make message field to show server message
    private JTextField answerField; // make answer field to get user answer
    private JButton submitButton;
    private JButton nextButton;
    private JButton endButton;
    // buttons to get user answer

    private Socket socket; // socket for data I/O
    private BufferedReader in; // input stream variable
    private BufferedWriter out; // output stream variable
    
    private volatile boolean isGameEnded = false;
    // isGameEnded stops processing
    // true:stop game / false:continue game
    // volatile -> 변수 수정과 동시에 모든 스레드는 변동사항 확인 가능

    public ClientWithGUI() {
        setupGUI();
        connectToServer();
    } // constructor for thread

    private void setupGUI() {
        frame = new JFrame("Quiz Game Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());
        // frame setting for view UI

        serverMessageArea = new JTextArea();
        serverMessageArea.setEditable(false);
        serverMessageArea.setLineWrap(true);
        serverMessageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(serverMessageArea);
        // message field setting for view server message(Quiz, score etc.)

        JPanel inputPanel = new JPanel(new BorderLayout());
        answerField = new JTextField();
        inputPanel.add(answerField, BorderLayout.CENTER);
        // answer field setting to get user answer

        JPanel buttonPanel = new JPanel(new FlowLayout());
        submitButton = new JButton("Submit Answer");
        nextButton = new JButton("Next");
        endButton = new JButton("End Program");
        buttonPanel.add(submitButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(endButton);
        // setting for each buttons to get user command

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        // set the position for each field(panel)

        frame.setVisible(true); // show each field

        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAnswer();
            }
        }); // action if user press the submit answer button

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendNextCommand();
            }
        }); // action if user press the next button

        endButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendEndCommand();
            }
        }); // action if user press the end program button
    }

    private void connectToServer() {
        String ip = "localhost";
        int port = 3535;

        try {
            File configFile = new File("server_info.txt");
            if (configFile.exists()) {
                Properties config = new Properties();
                config.load(new FileInputStream(configFile));
                ip = config.getProperty("ip", "localhost");
                port = Integer.parseInt(config.getProperty("port", "3535"));
            } // Load server IP and port num from server_info.txt

            socket = new Socket(ip, port); // connect to server's server-socket(welcome socket)
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // create input stream
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // create output stream

            listenToServer();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenToServer() {
    	// get new thread
        new Thread(() -> {
            try {
                String inputMessage; // string to store server's output
                String serverCommand = ""; // store server's command protocol
                String serverMessage = ""; // store server's server_message protocol
                
                while(true) {
                	while ((inputMessage = in.readLine()) != null && !inputMessage.isEmpty()) {
                        String[] parts = inputMessage.split(":", 2);
                        if (parts[0].equals("COMMAND")) {
                            serverCommand = parts[1];
                        } else if (parts[0].equals("SERVER_MESSAGE")) {
                            serverMessage = parts[1];
                        }
                    }  // read and store server's output protocol(command, server_message)

                    if ("FINAL".equalsIgnoreCase(serverCommand)) {
                    	// if server's command protocol is "final"
                        appendMessage(serverMessage);
                        break; // stop program(no more quiz play)
                    } else if ("QUIZ".equalsIgnoreCase(serverCommand)) {
                    	// if server's command protocol is "quiz"
                        appendMessage(serverMessage);
                        appendMessage("Enter your answer and press the button!");
                    } else if ("EVALUATE".equalsIgnoreCase(serverCommand)) {
                    	// if server's command protocol is "evaluate"
                        appendMessage(serverMessage);
                        appendMessage("Press the button!");
                    } else if("END".equalsIgnoreCase(serverCommand)) {
                    	// if server's command protocol is "end"
                    	appendMessage(serverMessage);
                    	break; // stop program(no more quiz play)
                    }	
                }
            } catch (IOException e) {
                appendMessage("Disconnected from server.");
            } finally {
            	try {
                    if (socket != null && !socket.isClosed()) socket.close();
                    appendMessage("Connection closed.");
                    // if socket is open, close the connection
                } catch (IOException e) {
                    appendMessage("Error while closing connection: " + e.getMessage());
                }
            }
        }).start();
    }

    private void sendAnswer() {
    	if (isGameEnded) return;
        try {
            String userAnswer = answerField.getText();
            // get user answer from answer field
            if (userAnswer.trim().isEmpty()) {
                appendMessage("Please enter an answer before submitting.");
                return;
            }
            appendMessage("Your answer: "+userAnswer);
            out.write("COMMAND:ANSWER\n"); // Protocol for "answer"
            out.write("CLIENT_ANSWER:" + userAnswer + "\n\n");
            // send user answer to server
			// Protocol for client_answer
            out.flush();
            answerField.setText("");
        } catch (IOException e) {
            appendMessage("Error sending answer to server.");
        }
    } // // if user press the "submit answer" button

    private void sendNextCommand() {
    	if (isGameEnded) return;
        try {
        	appendMessage("Your pressed next button.");
            out.write("COMMAND:CONTINUE\n"); // Protocol for "continue"
            out.write("CLIENT_ANSWER:continue to play!\n\n");
            // send message to server
			// Protocol for client_answer
            out.flush();
        } catch (IOException e) {
            appendMessage("Error sending next command to server.");
        }
    } // // if user press the "next" button

    private void sendEndCommand() {
    	if (isGameEnded) return;
        try {
        	appendMessage("Your pressed End Program button.");
            out.write("COMMAND:END\n"); // Protocol for "End"
            out.write("CLIENT_ANSWER:Client ended the program.\n\n");
            // send message to server
			// Protocol for client_answer
            out.flush();
            isGameEnded = true; // stop play game
        } catch (IOException e) {
            appendMessage("Error ending the program.");
        }
    } // if user press the "end program" button

    private void appendMessage(String message) {
        serverMessageArea.append(message + "\n");
        serverMessageArea.setCaretPosition(serverMessageArea.getDocument().getLength());
    } // function for view more message to server message field

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientWithGUI::new); // show UI with GUI swing
    }
}
