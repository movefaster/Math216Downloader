import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Morton on 8/29/2016.
 */
public class Downloader {
    private String url;

    Downloader(String url) {
        this.url = url;
    }

    public void download(File dir) throws IOException {
        List<String> existing = new LinkedList<>();
        Stream.of(dir.listFiles(pathname -> pathname.isFile()
                && pathname.toString().startsWith("CBaryMath")
                && pathname.toString().endsWith("mp4"))).map(File::getName).forEach(existing::add);
        List<String> toBeDownloaded = getLinks().stream().filter(existing::contains).collect(Collectors.toList());
    }

    private List<String> getLinks() throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements elements = doc.select("a [href]");
        LinkedList<String> links = new LinkedList<String>();
        elements.forEach(e -> {
            String href = e.attr("href");
            if (href.startsWith("CBrayMath") && href.endsWith("mp4")) {
                links.add(href);
            }
        });
        return links;
    }
}
