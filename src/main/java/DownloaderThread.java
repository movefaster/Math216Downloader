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

import javafx.concurrent.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

/**
 * Created by Morton on 9/30/16.
 */
public class DownloaderThread extends Task<Void> {
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
            String filename = Utils.getStringPart(url, "/", -1);
            URL file = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(file.openStream());
            if (!new File(dir, filename).exists() || overwrite) {
                FileOutputStream output = new FileOutputStream(new File(dir, filename));
                output.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                updateMessage(String.format("Downloading %s (%d/%d)", filename, index + 1, total));
            } else {
                updateMessage(String.format("Skipped duplicate file %s (%d/%d)", filename, index + 1, total));
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
