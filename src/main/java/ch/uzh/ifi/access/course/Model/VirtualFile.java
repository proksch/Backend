package ch.uzh.ifi.access.course.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
public class VirtualFile {
    private static final List<String> MEDIA_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "mp3", "mp4");

    private final UUID id;

    private String path;
    private String name;
    private String extension;
    private String content;
    private Boolean isMediaType;

    @JsonIgnore
    private File file;

    public VirtualFile(String fullPath, String virtualPath) {
        this.id = UUID.randomUUID();
        this.file = new File(fullPath);
        this.path = virtualPath;

        if (file.exists() && !file.isDirectory()) {
            try {
                String tmp[] = file.getName().split("\\.");
                if (tmp.length > 1) {
                    name = tmp[0];
                    extension = tmp[1];

                    if (MEDIA_EXTENSIONS.contains(extension)) {
                        content = "";
                        isMediaType = true;
                    } else {
                        content = new String(Files.readAllBytes(Paths.get(fullPath)));
                        isMediaType = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

