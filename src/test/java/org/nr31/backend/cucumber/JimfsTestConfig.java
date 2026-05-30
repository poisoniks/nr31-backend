package org.nr31.backend.cucumber;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.file.FileSystem;

@TestConfiguration
public class JimfsTestConfig {

    @Bean
    public FileSystem fileSystem() {
        return Jimfs.newFileSystem(Configuration.unix());
    }
}
