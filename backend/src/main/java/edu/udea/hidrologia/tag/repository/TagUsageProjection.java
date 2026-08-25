package edu.udea.hidrologia.tag.repository;

public interface TagUsageProjection {

    Long getId();

    String getName();

    String getSlug();

    long getUsageCount();
}
