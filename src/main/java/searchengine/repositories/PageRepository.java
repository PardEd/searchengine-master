package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {

    @Query(value = "SELECT id FROM page WHERE path LIKE :url AND site_id = :siteId", nativeQuery = true)
    Long findPageIdByUrlSiteId(String url, Long siteId);

    @Query(value = "SELECT COUNT(*) FROM page", nativeQuery = true)
    Integer getSizePages();

    @Query(value = "SELECT COUNT(*) FROM page WHERE site_id = :siteId", nativeQuery = true)
    Integer getSizePagesBySiteId(Long siteId);

}

