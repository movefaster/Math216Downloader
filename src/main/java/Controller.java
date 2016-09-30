/*
 * Copyright [2016] [Morton Mo]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Controller implements Initializable {
//    private static final String DEFAULT_URL = "https://services.math.duke.edu/~cbray/216PreLecs/";
    private static final String[] presetStrings = {"Lecture Recordings", "Past Exams", "Other"};
    private static final String[] presetUrls = {"https://services.math.duke.edu/~cbray/216PreLecs/",
            "https://services.math.duke.edu/~cbray/OldExams/107/", ""};
    private static final String[] presetEndsWith = {"mp4", "pdf", ""};
    private static final String[] presetStartsWith = {"CBrayMath", "", ""};


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
    @FXML public TextField txtUrl;
    @FXML public Button btnReload;
    @FXML public ComboBox<String> comboPreset;
    @FXML public TextField txtStartsWith;
    @FXML public TextField txtEndsWith;

    private List<String> urls;
    private String url;
    private Stage primaryStage;
    private File dir;
    private ObservableList<String> available;
    private ObservableList<String> existing;
    private String startsWith;
    private String endsWith;
    private boolean overwrite;
    private DownloaderThread downloader;
    private Thread dlThread;
    private Pattern pattern;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtUrl.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue.length() > 0 ^ txtUrl.isFocused()) {
                reloadUrl();
            }
        }));
        txtUrl.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue) {
                reloadUrl();
            }
        }));

        txtStartsWith.textProperty().addListener(((observable, oldValue, newValue) -> startsWith = newValue));
        txtEndsWith.textProperty().addListener(((observable, oldValue, newValue) -> endsWith = newValue));


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

        lblExistingCount.setText("Existing: 0");

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

        comboPreset.setItems(FXCollections.observableArrayList(Arrays.asList(presetStrings)));
        comboPreset.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) ->{
            int index = newValue.intValue();
            updateParameters(index);
            if (index == presetStrings.length - 1) {  // If user selected other
                txtUrl.requestFocus();
            }
        }));

        progressBar.setProgress(0);

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

        btnReload.setOnAction((event) -> reloadUrl());
    }

    private void reloadUrl() {
        String newUrl = txtUrl.getText();
        UrlValidator validator = new UrlValidator(new String[]{"http", "https"});
        if (validator.isValid(newUrl)) {
            url = newUrl;
            updateAvailable(url);
        } else {
            AlertBox.display("Error", "The URL you put in is not valid. Please make sure you are\n" +
                    "using the correct URL to the lecture recordings / exams.");
            comboPreset.getSelectionModel().select(0);
        }
    }

    /**
     * Updates txtUrl, txtStartsWith, txtEndsWith when selected preset is changed
     * @param index the selected index of the comboxPreset
     */

    private void updateParameters(int index) {
        txtStartsWith.setText(presetStartsWith[index]);
        txtEndsWith.setText(presetEndsWith[index]);
        startsWith = presetStartsWith[index];
        endsWith = presetEndsWith[index];
        txtUrl.setText(presetUrls[index]);
        url = presetUrls[index];
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
    private void setListAvailable(ObservableList<String> files) {
        ObservableList<String> filenames = FXCollections.observableArrayList(files);
        listAvailable.setItems(FXCollections.observableArrayList(filenames.stream()
                .map(e -> Utils.getStringPart(e, "/", -1))
                .collect(Collectors.toList())));
    }

    private void updateAvailable(String url) {
        if (url.length() > 0) {
            try {
                Document doc = Jsoup.connect(url).get();
                Elements elements = doc.select("a");
                available.setAll(elements.stream()
                        .map(e -> e.attr("href"))
                        .filter(e -> e.startsWith(startsWith) && e.endsWith(endsWith))
                        .map(e -> url + e).collect(Collectors.toList()));
                setListAvailable(available);
            } catch (IOException e) {
                e.printStackTrace();
                AlertBox.display("Network Error", "Unable to retrieve recordings web page.\n");
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void setListExisting(ObservableList<String> files) {
        listExisting.setItems(files);
    }

    private void updateExisting(File dir) {
        if (dir != null) {
            File[] files = dir.listFiles(pathname -> {
                String filename = pathname.getAbsoluteFile().getName();
                return pathname.isFile() && filename.startsWith(startsWith) && filename.endsWith(endsWith);
            });
            if (files == null) files = new File[0];
            existing.setAll(Stream.of(files).map(File::getName).collect(Collectors.toList()));
            setListExisting(existing);
        } else {
            existing = null;
            setListExisting(FXCollections.observableArrayList());
        }
    }

}
