package quizGame;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class QuizServer {
	
	public static String quizList(int num) {
		String question = "";
		switch(num) {
		case 1:
			question = "Enter the result of 36+28 (20 points)";
			break;
		case 2:
			question = "Enter the result of 72-45 (20 points)";
			break;
		case 3:
			question = "Enter the result of 18*21 (20 points)";
			break;
		case 4:
			question = "Enter the result of 42/8 (20 points)";
			break;
		case 5:
			question = "Enter the result of 42%8 (20 points)";
			break;
		default:
			question = "No more question exists...";
		}
		return question;
	}
	// function for assignment quiz for each "case number"
	// quiz returns in type "String"
	
	public static int evaluate(int num, String userAnswer) {
		int evaluate=0;
		switch(num) {
			case 1: // for case Number "1"
				if (userAnswer.equals("64"))
					evaluate=1; // correct answer
				else
					evaluate=-1; // incorrect answer
				break;
			case 2:
				if (userAnswer.equals("27"))
					evaluate=1;
				else
					evaluate=-1;
				break;
			case 3:
				if (userAnswer.equals("378"))
					evaluate=1;
				else
					evaluate=-1;
				break;
			case 4:
				if (userAnswer.equals("5"))
					evaluate=1;
				else
					evaluate=-1;
				break;
			case 5:
				if (userAnswer.equals("2"))
					evaluate=1;
				else
					evaluate=-1;
				break;
			default: // else case number doesn't have question -> wrong approach
				evaluate=404;
			}
		return evaluate;
	}
	// function for evaluate user's answer for each "case number"
	// evaluate result returns in type "int"

	public static void main(String[] args) {
		BufferedReader in = null; // input stream variable
		BufferedWriter out = null; // output stream variable
		
		ServerSocket listener = null; // server-socket(welcome socket)
		Socket socket = null; // socket for data I/O
		
		try {
			listener = new ServerSocket(3535); // make server socket to monitoring connection request from client
			
			System.out.println("Start Server...");
			System.out.println("Waiting for clients");
			
			socket = listener.accept(); // if connection request exists, return(create) socket for data I/O
			System.out.println("One user connect this program!");
			
			in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // create inputStream
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())); // create outputStream
			
			int caseNumber=1; // to identify quiz number
			int totalScore=0; // store user's total score
			
			while(true) {
				if(caseNumber>5) {
					// maximum quiz number = 6
					out.write("COMMAND:FINAL\n"); // Protocol for "FINAL"
					out.write("SERVER_MESSAGE:You completed all! Total score: "+totalScore+"\n\n"); 
					// send user's total score
					// Protocol for server_message
					out.flush();
					System.out.println("End Quiz game."); // message for server console
					break;
				}
				
				String inputMessage; // string to store client's output
				String clientCommand=""; // store client's command protocol
				String clientAnswer=""; // store client's client_answer protocol
				
				String question=quizList(caseNumber); // get appropriate quiz for each case number
				out.write("COMMAND:QUIZ\n"); // Protocol for "QUIZ"
				out.write("SERVER_MESSAGE:QUIZ"+caseNumber+"> "+question+"\n\n");
				// send quiz to client
				// Protocol for server_message
				out.flush();
				
				while((inputMessage=in.readLine())!=null && !inputMessage.isEmpty()) {
					String[] parts=inputMessage.split(":", 2);
					if(parts[0].equals("COMMAND")) {
						clientCommand = parts[1];
					} else if(parts[0].equals("CLIENT_ANSWER")) {
						clientAnswer = parts[1];
					}
				} // read and store client's output protocol(command, client_answer)
				
				if("ANSWER".equalsIgnoreCase(clientCommand)) {
					// if client's command protocol is "answer"
					int feedback = evaluate(caseNumber, clientAnswer); // get evaluate result(correct/incorrect)
					out.write("COMMAND:EVALUATE\n"); // Protocol for "Evaluate"
					if(feedback==1) {
						totalScore+=20;
						out.write("SERVER_MESSAGE:Correct! Total Score: "+totalScore+"(Enter \"next\" or \"End Program\")"+"\n\n");
						// send evaluate result and score to client
						// Protocol for server_message
					} // case of user answer is 'correct'
					else {
						totalScore-=20;
						out.write("SERVER_MESSAGE:Incorrect. Total Score: "+totalScore+"(Enter \"next\" or \"End Program\")"+"\n\n");
						// send evaluate result and score to client
						// Protocol for server_message
					} // case of user answer is 'incorrect'
					out.flush();
					caseNumber++; // update for remain quiz number
				}
				
				while((inputMessage=in.readLine())!=null && !inputMessage.isEmpty()) {
					String[] parts=inputMessage.split(":", 2);
					if(parts[0].equals("COMMAND")) {
						clientCommand = parts[1];
					} else if(parts[0].equals("CLIENT_ANSWER")) {
						clientAnswer = parts[1];
					}
				} // read and store client's output protocol(command, client_answer)
				
				if("END".equalsIgnoreCase(clientCommand)) {
					// if client's command protocol is "end"
					out.write("COMMAND:END\n"); // Protocol for "End"
					out.write("SERVER_MESSAGE:Stop play! Total score: "+totalScore+"\n\n");
					// send score to client
					// Protocol for server_message
					out.flush();
					System.out.println("User stop playingT^T"); // message for server console
					break; // stop play the application
				}
				if("CONTINUE".equalsIgnoreCase(clientCommand)){
					// if client's command protocol is "continue"
					continue; // send next quiz to client
				}
			}
		} catch(IOException e) {
			System.out.println("Error: "+e.getMessage()); 
			// connection error of client-server
		} finally {
			try {
				socket.close(); // close socket for data I/O
				listener.close(); // close server-socket(welcome socket)
			} catch(IOException e) {
				System.out.println("Disconnected.");
			}
		}
	}
}