package edu.udea.hidrologia.section.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udea.hidrologia.section.entity.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByActiveTrueOrderByDisplayOrderAsc();
}
