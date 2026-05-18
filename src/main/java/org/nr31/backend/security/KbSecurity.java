package org.nr31.backend.security;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.repository.KbArticleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("kbSecurity")
@RequiredArgsConstructor
public class KbSecurity {

    private final KbArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public boolean isAuthor(Authentication authentication, Long articleId) {
        if (authentication == null || articleId == null) {
            return false;
        }
        return articleRepository.findById(articleId)
                .map(article -> article.getAuthor().getUsername().equals(authentication.getName()))
                .orElse(false);
    }
}
