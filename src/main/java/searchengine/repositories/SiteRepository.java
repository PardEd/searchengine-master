package searchengine.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

    @Query(value = "SELECT * FROM site WHERE name LIKE :siteName", nativeQuery = true)
    List<SiteEntity> findSiteByName(String siteName);

    @Query(value = "SELECT * FROM site WHERE url LIKE :siteUrl", nativeQuery = true)
    SiteEntity findSiteByUrl(String siteUrl);

    @Query(value = "SELECT * FROM site", nativeQuery = true)
    List<SiteEntity> getAllSite();
}

