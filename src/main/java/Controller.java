import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller implements Initializable {
    @FXML public TextField txtDirectory;
    @FXML public ListView listExisting;
    @FXML public ListView listAvailable;
    @FXML public ProgressBar progressBar;
    @FXML public Button btnDownload;
    @FXML public Button btnExit;
    @FXML public Button btnBrowse;
    @FXML public CheckBox chkOverwrite;
    @FXML public Label lblStatus;

    private List<String> urls;
    private String url;
    private Stage primaryStage;
    private File dir;
    private List<String> available;
    private List<String> existing;
    private boolean overwrite;
    private DownloaderThread downloader;
    private Thread dlThread;

    private class DownloaderThread extends Task<Void> {
        private List<String> urls;
        private File dir;
        private boolean overwrite;

        DownloaderThread(List<String> urls, File dir, boolean overwrite) {
            this.urls = urls;
            this.dir = dir;
            this.overwrite = overwrite;
        }

        private void download(String url) {
            try {
                String[] urlParts = url.split("/");
                String filename = urlParts[urlParts.length - 1];
                URL file = new URL(url);
                ReadableByteChannel rbc = Channels.newChannel(file.openStream());
                if (!new File(dir, filename).exists() || overwrite) {
                    FileOutputStream output = new FileOutputStream(new File(dir, filename));
                    output.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        protected Void call() {
            for (int i = 0; i < urls.size(); i++) {
                download(urls.get(i));
                updateMessage("Downloading " + urls.get(i));
                updateProgress(i, urls.size());
            }
            return null;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtDirectory.setEditable(false);
        txtDirectory.setText("");
        txtDirectory.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.length() > 0) {
                dir = new File(newValue);
                updateExisting(dir);
            }
            else {
                dir = null;
            }
        }));
        progressBar.setProgress(0);
        url = "https://services.math.duke.edu/~cbray/216PreLecs/";
        updateAvailable(url);
        chkOverwrite.selectedProperty().addListener(((observable, oldValue, newValue) ->{
            overwrite = newValue;
            System.out.println("Overwrite is set to " + overwrite);
        }));
    }

    void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    protected void handleBrowseAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setInitialDirectory(new File(System.getProperty("user.dir")));
        dc.setTitle("Choose a directory to save recordings...");
        File dirSelected = dc.showDialog(primaryStage);
        if (dirSelected != null) {
            txtDirectory.setText(dirSelected.getAbsolutePath());
        }
    }

    @FXML
    protected void handleDownloadAction(ActionEvent event) {
        if (dir == null) {
            AlertBox.display("Error", "You must select an output directory first.");
        } else {
            if (existing == null || available == null) {
                throw new RuntimeException("Cannot initiate download.");
            } else {
                downloader = new DownloaderThread(available, dir, overwrite);
                dlThread = new Thread(downloader);
                dlThread.start();
                progressBar.progressProperty().bind(downloader.progressProperty());
                lblStatus.textProperty().bind(downloader.messageProperty());
                downloader.messageProperty().addListener(((observable, oldValue, newValue) -> {
                    System.out.println(newValue);
                }));
                try {
                    dlThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateExisting(dir);  // trigger update of existing folder after download
                progressBar.progressProperty().unbind();
                lblStatus.textProperty().unbind();
            }
        }
    }

    @FXML
    protected void handleExitAction(ActionEvent event) {
        if (dlThread != null && dlThread.isAlive()) {
            boolean o = AlertBox.displayYesNo("Warning", "Your download is incomplete.\nDo you really want to quit?");
            if (o) System.exit(1);
        } else {
            System.exit(0);
        }
    }

    @SuppressWarnings("unchecked")
    void setListAvailable(List<String> files) {
        listAvailable.setItems(FXCollections.observableArrayList(files));
    }

    void updateAvailable(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.select("a");
            available = elements.stream()
                    .map(e -> e.attr("href"))
                    .filter(e -> e.startsWith("CBrayMath") && e.endsWith("mp4"))
                    .map(e -> url + e).collect(Collectors.toList());
            setListAvailable(available.stream().map(e -> {
                String[] urlParts = e.split("/");
                return urlParts[urlParts.length - 1];
            }).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
            AlertBox.display("Network Error", "Unable to retrieve recordings web page.\n");
        }

    }

    @SuppressWarnings("unchecked")
    private void setListExisting(List<String> files) {
        listExisting.setItems(FXCollections.observableArrayList(files));
    }

    void updateExisting(File dir) {
        if (dir != null) {
            File[] files = dir.listFiles(pathname -> {
                String filename = pathname.getAbsoluteFile().getName();
                return pathname.isFile() && filename.startsWith("CBrayMath") && filename.endsWith("mp4");
            });
            if (files == null) files = new File[0];
            existing = Stream.of(files).map(File::getName).collect(Collectors.toList());
            setListExisting(existing);
        } else {
            existing = null;
            setListExisting(new ArrayList<>());
        }
    }

}
