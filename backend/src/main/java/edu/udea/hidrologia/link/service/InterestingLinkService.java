package edu.udea.hidrologia.link.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.link.dto.InterestingLinkResponse;
import edu.udea.hidrologia.link.entity.InterestingLink;
import edu.udea.hidrologia.link.repository.InterestingLinkRepository;

@Service
public class InterestingLinkService {

    private final InterestingLinkRepository interestingLinkRepository;

    public InterestingLinkService(InterestingLinkRepository interestingLinkRepository) {
        this.interestingLinkRepository = interestingLinkRepository;
    }

    @Transactional(readOnly = true)
    public List<InterestingLinkResponse> findActiveLinks() {
        return interestingLinkRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private InterestingLinkResponse toResponse(InterestingLink interestingLink) {
        return new InterestingLinkResponse(
                interestingLink.getId(),
                interestingLink.getTitle(),
                interestingLink.getDescription(),
                interestingLink.getUrl(),
                interestingLink.getDisplayOrder());
    }
}
