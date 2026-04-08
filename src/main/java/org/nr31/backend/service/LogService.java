package org.nr31.backend.service;

import org.springframework.core.io.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LogService {
    Page<String> listLogFiles(Pageable pageable);

    Resource getLogFile(String fileName, Long offsetFromEnd, Long limit);
}
