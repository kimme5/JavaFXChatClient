package application;
	
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;


public class Main extends Application {
	
	Socket socket;
	TextArea textArea;
	
	public void startClient(String IP, int port) {
		Thread thread = new Thread() {
			public void run() {
				try {
					socket = new Socket(IP, port);
					receive();
				} catch(Exception e) {
					if(!socket.isClosed()) {
						stopClient();
						System.out.println("[서버 접속 실패]");
						Platform.exit();
					}
				}
				
			}
		};
		thread.start();
	}
	
	public void stopClient() {
		try {
			if(socket != null && !socket.isClosed()) {
				socket.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void receive() {
		while(true) {
			try {
				InputStream in = socket.getInputStream();
				byte[] buffer = new byte[512];
				int length = in.read(buffer);
				if(length == -1) throw new IOException();
				String message = new String(buffer, 0, length, "UTF-8");
				Platform.runLater(() -> {
					textArea.appendText(message);
				});
			} catch(Exception e) {
				stopClient();
				break;
			}
		}
	}
	
	public void send(String message) {
		Thread thread = new Thread() {
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer);
					out.flush();
				} catch(Exception e) {
					stopClient();
				}
			}
		};
		thread.start();
	}
	
//	실제 프로그램을 동작시키는 메소드
	@Override
	public void start(Stage primaryStage) {

		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		HBox hbox = new HBox();
		hbox.setSpacing(5);
		
		TextField userName = new TextField();
		userName.setPrefWidth(150);
		userName.setPromptText("닉네임을 입력하세요");
		HBox.setHgrow(userName, Priority.ALWAYS);
//		hbox.setHgrow(userName, Priority.ALWAYS);
		
		TextField IPText = new TextField("127.0.0.1");
		TextField portText = new TextField("9876");
		portText.setPrefWidth(80);
		
		hbox.getChildren().addAll(userName, IPText, portText);
		root.setTop(hbox);

		// 서버에서 보내진 메세지를 출력하는 부분
		textArea = new TextArea();
		textArea.setEditable(false);
		root.setCenter(textArea);
		
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		// 접속전까지는 어떠한 메세지도 출력하지 않음
		input.setDisable(true);
		// input 창에서의 Enter키 이벤트
		input.setOnAction(event -> {
			send(userName.getText() + ": " + input.getText() + "\n");
		});
		
		Button sendButton = new Button("보내기");
		sendButton.setDisable(true);
		sendButton.setOnAction(event -> {
			send(userName.getText() + ": " + input.getText() + "\n");
		});
		
		Button connectionButton = new Button("접속하기");
		connectionButton.setOnAction(event -> {
			if(connectionButton.getText().equals("접속하기")) {
				int port = 9876;
				try {
					port = Integer.parseInt(portText.getText());
				} catch(Exception e) {
					e.printStackTrace();
				}
				startClient(IPText.getText(), port);
				Platform.runLater(() -> {
					textArea.appendText(" [ 채팅방 접속 ] \n");
				});
				connectionButton.setText("종료하기");
				input.setDisable(false);
				sendButton.setDisable(false);
				input.requestFocus();
			} else {
//				send(userName.getText() + " 님이 퇴장하셨습니다.\n");
				stopClient();
				Platform.runLater(() -> {
					textArea.appendText("[ 채팅방 퇴장 ]\n");
				});
				connectionButton.setText("접속하기");
				input.setDisable(true);
				sendButton.setDisable(true);
			}
		});
		
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton);
		pane.setCenter(input);
		pane.setRight(sendButton);
		
		root.setBottom(pane);
		
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[ JavaFX Chat - Client ]");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event -> stopClient());
		primaryStage.show();
		
		// 채팅클라이언트창이 기본 포커스를 커넥션버튼으로 설정
		connectionButton.requestFocus();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
