import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
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

    private String url;
    private Stage primaryStage;
    private File dir;
    private List<String> available;
    private List<String> existing;
    private Downloader downloader;

    private class Downloader {
        private String url;

        Downloader(String url) {
            this.url = url;
        }

        void download(File dir, List<String> existing) throws IOException {
            List<String> toBeDownloaded = getLinks().stream().filter(existing::contains).collect(Collectors.toList());

        }

        private List<String> getLinks() throws IOException {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.select("a");
            LinkedList<String> links = new LinkedList<>();
            elements.forEach(e -> {
                String href = e.attr("href");
                if (href.startsWith("CBrayMath") && href.endsWith("mp4")) {
                    links.add(href);
                }
            });
            return links;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtDirectory.setText("");
        progressBar.setProgress(0);
        url = "https://services.math.duke.edu/~cbray/216PreLecs/";
        downloader = new Downloader(url);
        try {
            available = downloader.getLinks();
        } catch (IOException e) {
            e.printStackTrace();
        }
        existing = Stream.of(dir.listFiles(pathname -> pathname.isFile()
                && pathname.toString().startsWith("CBaryMath")
                && pathname.toString().endsWith("mp4"))).map(File::getName).collect(Collectors.toList());
        listExisting.setItems(FXCollections.observableArrayList(existing));
        listAvailable.setItems(FXCollections.observableArrayList());
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
            dir = dirSelected;
            txtDirectory.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    protected void handleDownloadAction(ActionEvent event) {
        try {
            if (dir != null) {
                downloader.download(dir, existing);
            } else {
                AlertBox.display("Error", "You must select download directory first.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    void setListAvailable(List<String> files) {
        listAvailable.setItems(FXCollections.observableArrayList(files));
    }

}
