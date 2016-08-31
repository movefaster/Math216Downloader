import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import java.util.function.Supplier;
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
    @FXML public Label lblExistingCount;
    @FXML public Label lblAvailableCount;

    private List<String> urls;
    private String url;
    private Stage primaryStage;
    private File dir;
    private ObservableList<String> available;
    private ObservableList<String> existing;
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

        private void download(String url, int index, int total) {
            try {
                String filename = getStringPart(url, "/", -1);
                URL file = new URL(url);
                ReadableByteChannel rbc = Channels.newChannel(file.openStream());
                if (!new File(dir, filename).exists() || overwrite) {
                    FileOutputStream output = new FileOutputStream(new File(dir, filename));
                    output.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    updateMessage(String.format("Downloading %s (%d/%d)", filename, index + 1, total));
                } else {
                    updateMessage("Skipped duplicate file " + filename);
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        protected Void call() {
            for (int i = 0; i < urls.size(); i++) {
                if (isCancelled()) break;
                download(urls.get(i), i, urls.size());
                updateProgress(i + 1, urls.size());
            }
            if (!isCancelled()) {
                updateMessage("Completed.");
            } else {
                updateMessage("Cancelled.");
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

        lblExistingCount.setText("Existing: No directory selected");

        existing = FXCollections.observableArrayList();
        available = FXCollections.observableArrayList();

        existing.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                lblExistingCount.setText("Existing: " + c.getList().size());
            }
        });

        available.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                lblAvailableCount.setText("Available: " + c.getList().size());
            }
        });

        progressBar.setProgress(0);
        url = "https://services.math.duke.edu/~cbray/216PreLecs/";
        updateAvailable(url);
        chkOverwrite.selectedProperty().addListener(((observable, oldValue, newValue) ->{
            if (newValue) {
                boolean option = AlertBox.displayYesNo("Warning", "This will erase all existing recordings and " +
                        "download them anew.\nAre you sure?");
                if (option) {
                    overwrite = true;
                } else {
                    chkOverwrite.setSelected(false);
                    overwrite = false;
                }
            } else {
                overwrite = newValue;
            }
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
        if (dlThread == null || !dlThread.isAlive()) {
            if (dir == null) {
                AlertBox.display("Error", "You must select an output directory first.");
            } else {
                if (existing == null || available == null) {
                    throw new RuntimeException("Cannot initiate download.");
                } else {
                    downloader = new DownloaderThread(available, dir, overwrite);
                    downloader.setOnSucceeded(value -> {
                        AlertBox.display("Success", "Download completed.");
                        updateExisting(dir);
                        btnDownload.setText("Download");
                    });
                    downloader.setOnCancelled(value -> updateExisting(dir));
                    dlThread = new Thread(downloader);
                    dlThread.setDaemon(true);
                    dlThread.start();
                    progressBar.progressProperty().bind(downloader.progressProperty());
                    lblStatus.textProperty().bind(downloader.messageProperty());
                    downloader.messageProperty().addListener(((observable, oldValue, newValue) -> {
                        System.out.println(newValue);
                    }));
                    btnDownload.setText("Cancel");
                }
            }
        } else {
            downloader.cancel();
            btnDownload.setText("Download");
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
    void setListAvailable(ObservableList<String> files) {
        ObservableList<String> filenames = FXCollections.observableArrayList(files);
        listAvailable.setItems(FXCollections.observableArrayList(filenames.stream()
                .map(e -> getStringPart(e, "/", -1))
                .collect(Collectors.toList())));
    }

    void updateAvailable(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.select("a");
            available.setAll(elements.stream()
                    .map(e -> e.attr("href"))
                    .filter(e -> e.startsWith("CBrayMath") && e.endsWith("mp4"))
                    .map(e -> url + e).collect(Collectors.toList()));
            setListAvailable(available);
        } catch (IOException e) {
            e.printStackTrace();
            AlertBox.display("Network Error", "Unable to retrieve recordings web page.\n");
        }

    }

    @SuppressWarnings("unchecked")
    private void setListExisting(ObservableList<String> files) {
        listExisting.setItems(files);
    }

    void updateExisting(File dir) {
        if (dir != null) {
            File[] files = dir.listFiles(pathname -> {
                String filename = pathname.getAbsoluteFile().getName();
                return pathname.isFile() && filename.startsWith("CBrayMath") && filename.endsWith("mp4");
            });
            if (files == null) files = new File[0];
            existing.setAll(Stream.of(files).map(File::getName).collect(Collectors.toList()));
            setListExisting(existing);
        } else {
            existing = null;
            setListExisting(FXCollections.observableArrayList());
        }
    }

    /**
     * Retrieves an arbitrary part after splitting a string using the given regex.
     * If n is negative, it will retrieve the last n-th part of the string
     * (similar to python syntax array[-n]).
     * @param s the string to be split
     * @param regex the regular expression to split the string
     * @param n the n-th part of the string to be retrieved. If it's negative, the
     *          last n-th part
     * @return the n-th part, or last n-th part, of a given string split by regex
     */

    private static String getStringPart(String s, String regex, int n) {
        String[] parts = s.split(regex);
        if (Math.abs(n) >= parts.length) throw new IllegalArgumentException(String.format("N cannot be larger" +
                " than the length (%d) of the split string.", parts.length));
        if (n >= 0) return parts[n];
        else return parts[parts.length + n];
    }

}
