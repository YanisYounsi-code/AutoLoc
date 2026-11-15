package fr.ece.autoloc.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Une page de resultats renvoyee par l'API REST, sous une forme JSON simple et stable. */
public record PageResponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
