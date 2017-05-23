/**
 * 
 */
package application.windows;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import application.Main;
import application.tools.ActionTool;
import application.tools.InfoTool;
import application.tools.NotificationType;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * @author GOXR3PLUS
 *
 */
public class UpdateWindow extends StackPane {
	
	//--------------------------------------------------------------
	
	@FXML
	private GridPane centerGridPane;
	
	@FXML
	private Label updatesInformationLabel;
	
	@FXML
	private Label comingSoonLabel;
	
	@FXML
	private Label topLabel;
	
	@FXML
	private Button download;
	
	@FXML
	private Button gitHubButton;
	
	@FXML
	private Button automaticUpdate;
	
	@FXML
	private Button closeWindow;
	
	// -------------------------------------------------------------
	
	/** The logger. */
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	/** Window **/
	private Stage window = new Stage();
	
	private final InlineCssTextArea textArea1 = new InlineCssTextArea();
	private final InlineCssTextArea textArea2 = new InlineCssTextArea();
	private final VirtualizedScrollPane<InlineCssTextArea> vsPane1 = new VirtualizedScrollPane<>(textArea1);
	private final VirtualizedScrollPane<InlineCssTextArea> vsPane2 = new VirtualizedScrollPane<>(textArea2);
	
	private int update;
	
	/**
	 * The Thread which is responsible for the update check
	 */
	private static Thread updaterThread;
	
	/**
	 * Constructor.
	 */
	public UpdateWindow() {
		
		// ------------------------------------FXMLLOADER ----------------------------------------
		FXMLLoader loader = new FXMLLoader(getClass().getResource(InfoTool.FXMLS + "UpdateWindow.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		
		try {
			loader.load();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "", ex);
		}
		
		window.setTitle("Update Window");
		window.initStyle(StageStyle.UTILITY);
		window.setResizable(false);
		window.setScene(new Scene(this));
		window.getScene().setOnKeyReleased(k -> {
			if (k.getCode() == KeyCode.ESCAPE)
				window.close();
		});
	}
	
	/**
	 * Called as soon as .fxml is initialized
	 */
	@FXML
	private void initialize() {
		
		// --
		textArea1.setEditable(false);
		textArea1.setFocusTraversable(false);
		//--
		vsPane1.setMinSize(500, 425);
		vsPane1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		
		//--
		textArea2.setEditable(false);
		textArea2.setFocusTraversable(false);
		textArea2.setWrapText(true);
		//--
		vsPane2.setMinSize(500, 425);
		vsPane2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		
		centerGridPane.addRow(1, vsPane1, vsPane2);
		
		// -- automaticUpdate
		automaticUpdate.setOnAction(a -> startXR3PlayerUpdater(update));
		
		// -- download
		download.setOnAction(a -> ActionTool.openWebSite("https://sourceforge.net/projects/xr3player/"));
		
		// -- GitHub
		gitHubButton.setOnAction(a -> ActionTool.openWebSite("https://github.com/goxr3plus/XR3Player"));
		
		// -- closeWindow
		closeWindow.setOnAction(a -> window.close());
		
	}
	
	/**
	 * This method is fetching data from github to check if the is a new update
	 * for XR3Player
	 * 
	 * @param showTheWindow
	 *        If not update is available then don't show the window
	 */
	public synchronized void searchForUpdates(boolean showTheWindow) {
		
		// Not already running
		if (updaterThread != null && updaterThread.isAlive())
			return;
		
		updaterThread = new Thread(() -> {
			if (showTheWindow)
				Platform.runLater(
						() -> ActionTool.showNotification("Searching for Updates", "Fetching informations from server...", Duration.millis(1000), NotificationType.INFORMATION));
			
			//Check if we have internet connection
			if (InfoTool.isReachableByPing("www.google.com"))
				searchForUpdatesPart2(showTheWindow);
			else
				Platform.runLater(() -> ActionTool.showNotification("Can't Connect",
						"Can't connect to the update site :\n1) Maybe there is not internet connection\n2)GitHub is down for maintenance", Duration.millis(2500),
						NotificationType.ERROR));
			
		}, "Application Update Thread");
		
		updaterThread.setDaemon(true);
		updaterThread.start();
		
	}
	
	private void searchForUpdatesPart2(boolean showTheWindow) {
		try {
			
			Document doc = Jsoup.connect("https://raw.githubusercontent.com/goxr3plus/XR3Player/master/XR3PlayerUpdatePage.html").get();
			
			//Document doc = Jsoup.parse(new File("XR3PlayerUpdatePage.html"), "UTF-8", "http://example.com/");
			
			Element lastArticle = doc.getElementsByTag("article").last();
			
			// Not disturb the user every time the application starts if there is not new update
			int currentVersion = (int) Main.applicationProperties.get("Version");
			if (Integer.valueOf(lastArticle.id()) <= currentVersion && !showTheWindow)
				return;
			
			// Update is available or not?
			Platform.runLater(() -> {
				
				//--TopLabel
				if (Integer.valueOf(lastArticle.id()) > currentVersion) {
					window.setTitle("New update is available!");
					topLabel.setText("New Update ->( " + lastArticle.id() + " )<- is available!");
				} else {
					window.setTitle("You have the latest update!");
					topLabel.setText("You have the latest update ->( " + currentVersion + " )<-");
				}
				updatesInformationLabel.setText("Your current update is: ->( " + currentVersion + " )<-");
				
				//Clear the textAreas
				textArea1.clear();
				textArea2.clear();
				
				// -- TextArea 
				String style = "-fx-font-weight:bold; -fx-font-size:14; -fx-fill:black;";
				doc.getElementsByTag("article").forEach(element -> {
					
					// Append the text to the textArea
					textArea1.appendText("\n\n-------------Start of Update (" + element.id() + ")-------------\n");
					
					// Information
					textArea1.appendText("->Information: ");
					textArea1.setStyle(textArea1.getLength() - 13, textArea1.getLength() - 1, style.replace("black", "#202020"));
					textArea1.appendText(element.getElementsByClass("about").text() + "\n");
					
					// Release Date
					textArea1.appendText("->Release Date: ");
					textArea1.setStyle(textArea1.getLength() - 14, textArea1.getLength() - 1, style.replace("black", "firebrick"));
					textArea1.appendText(element.getElementsByClass("releasedate").text() + "\n");
					
					// Minimum JRE
					textArea1.appendText("->Minimum Java Version: ");
					textArea1.setStyle(textArea1.getLength() - 22, textArea1.getLength() - 1, style.replace("black", "orange"));
					textArea1.appendText(element.getElementsByClass("minJavaVersion").text() + "\n");
					
					// ChangeLog
					textArea1.appendText("->ChangeLog:\n");
					textArea1.setStyle(textArea1.getLength() - 11, textArea1.getLength() - 1, style.replace("black", "green"));
					final AtomicInteger counter = new AtomicInteger(-1);
					Arrays.asList(element.getElementsByClass("changelog").text().split("\\*")).forEach(el -> {
						if (counter.addAndGet(1) >= 1) {
							String s = "\t" + counter + ")";
							textArea1.appendText(s);
							textArea1.setStyle(textArea1.getLength() - s.length(), textArea1.getLength() - 1, style);
							textArea1.appendText(el + "\n");
						}
					});
					
				});
				
				textArea1.moveTo(textArea1.getLength());
				textArea1.requestFollowCaret();
				
				// -- TextArea 2
				doc.getElementsByTag("section").forEach(section -> {
					
					// Append the text to the textArea
					textArea2.appendText("\n\n-------------Upcoming Features for XR3Player-------------\n\n");
					
					// Information
					textArea2.appendText("->Coming:\n");
					textArea2.setStyle(textArea2.getLength() - 8, textArea2.getLength() - 1, style.replace("black", "green"));
					final AtomicInteger counter = new AtomicInteger(-1);
					Arrays.asList(section.getElementById("info").text().split("\\*")).forEach(el -> {
						if (counter.addAndGet(1) >= 1) {
							String s = "\t" + counter + ")";
							textArea2.appendText(s);
							textArea2.setStyle(textArea2.getLength() - s.length(), textArea2.getLength() - 1, style);
							textArea2.appendText(el + "\n");
						}
					});
					
					//Last Updated
					textArea2.appendText("->Last Updated: ");
					textArea2.setStyle(textArea2.getLength() - 14, textArea2.getLength() - 1, style.replace("black", "firebrick"));
					textArea2.appendText(section.getElementById("lastUpdated").text());
				});
				
				textArea2.moveTo(textArea2.getLength());
				textArea2.requestFollowCaret();
			});
			
			//show?
			if (showTheWindow || Integer.valueOf(lastArticle.id()) > currentVersion) {
				download.setDisable(Integer.valueOf(lastArticle.id()) <= currentVersion);
				automaticUpdate.setDisable(download.isDisable());
				update = Integer.valueOf(lastArticle.id());
				show();
			}
			
		} catch (IOException ex) {
			Platform.runLater(() -> ActionTool.showNotification("Error", "Trying to fetch update information a problem occured", Duration.millis(2500), NotificationType.ERROR));
			logger.log(Level.WARNING, "", ex);
		}
	}
	
	/**
	 * Calling this method to start the main Application which is XR3Player
	 * 
	 */
	public void startXR3PlayerUpdater(int update) {
		String applicationName = "XR3PlayerUpdater";
		
		// Start XR3Player Updater
		new Thread(() -> {
			String path = InfoTool.getBasePathForClass(Main.class);
			String[] applicationPath = { new File(path + applicationName + ".jar").getAbsolutePath() };
			
			//Show message that application is restarting
			Platform.runLater(() -> ActionTool.showNotification("Starting " + applicationName,
					"Application Path:[ " + applicationPath[0] + " ]\n\tIf this takes more than 10 seconds either the computer is slow or it has failed....", Duration.seconds(25),
					NotificationType.INFORMATION));
			
			try {
				
				//----Auto Update Button
				Platform.runLater(() -> automaticUpdate.setDisable(true));
				
				//------------Export XR3PlayerUpdater
				ActionTool.copy(UpdateWindow.class.getResourceAsStream("/updater/" + applicationName + ".jar"), applicationPath[0]);
				
				//------------Wait until XR3Player is created
				File XR3Player = new File(applicationPath[0]);
				while (!XR3Player.exists()) {
					Thread.sleep(50);
					System.out.println("Waiting " + applicationName + " Jar to be created...");
				}
				
				System.out.println(applicationName + " Path is : " + applicationPath[0]);
				
				//Create a process builder
				ProcessBuilder builder = new ProcessBuilder("java", "-jar", applicationPath[0], String.valueOf(update));
				builder.redirectErrorStream(true);
				Process process = builder.start();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				
				// Wait n seconds
				PauseTransition pause = new PauseTransition(Duration.seconds(10));
				pause.setOnFinished(f -> Platform.runLater(() -> ActionTool.showNotification("Starting " + applicationName + " failed",
						"\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));
				pause.play();
				
				// Continuously Read Output to check if the main application started
				String line;
				while (process.isAlive())
					while ( ( line = bufferedReader.readLine() ) != null) {
						System.out.println(line);
						if (line.isEmpty())
							break;
						if (line.contains(applicationName + " Application Started"))
							System.exit(0);
					}
				
			} catch (IOException | InterruptedException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);
				
				// Show failed message
				Platform.runLater(() -> Platform.runLater(() -> ActionTool.showNotification("Starting " + applicationName + " failed",
						"\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));
				
			}
			
			//----Auto Update Button
			Platform.runLater(() -> automaticUpdate.setDisable(false));
			
		}, "Start XR3Application Thread").start();
	}
	
	/**
	 * Show the Window
	 */
	public void show() {
		Platform.runLater(() -> {
			if (!window.isShowing())
				window.show();
			else
				window.requestFocus();
		});
	}
	
	/**
	 * @return the window
	 */
	public Stage getWindow() {
		return window;
	}
	
}
