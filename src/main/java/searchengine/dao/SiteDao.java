package searchengine.dao;

import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public class SiteDao implements Dao<Site>{
    @Override
    public Optional<Site> get(long id) {
        return Optional.empty();
    }

    @Override
    public List<Optional<Site>> getAll() {
        return null;
    }

    @Override
    public void save(Site site) {

    }

    @Override
    public void update(Site site) {

    }

    @Override
    public void delete(Site site) {

    }
}
