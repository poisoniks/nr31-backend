package org.nr31.backend.service;

import org.springframework.core.io.Resource;
import java.util.List;

public interface LogService {
    List<String> listLogFiles();
    Resource getLogFile(String fileName, Long offsetFromEnd, Long limit);
}
