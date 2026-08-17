package edu.udea.hidrologia.section.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.udea.hidrologia.section.dto.SectionResponse;
import edu.udea.hidrologia.section.entity.Section;
import edu.udea.hidrologia.section.repository.SectionRepository;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findActiveSections() {
        return sectionRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private SectionResponse toResponse(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getType(),
                section.getName(),
                section.getSlug(),
                section.getDescription(),
                section.getDisplayOrder());
    }
}
