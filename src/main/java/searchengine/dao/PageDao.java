package searchengine.dao;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class PageDao implements Dao<Page> {
    @Autowired
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Page> get(long id) {
        Page page = sessionFactory.getCurrentSession().get(Page.class, id);
        return Optional.of(page);
    }

    @Override
    public Optional<List<Page>> getAll() {
        List<Page> pages = sessionFactory.getCurrentSession().createQuery("from", Page.class).list();
        return Optional.of(pages);
    }

    @Override
    public void save(Page page) {
        sessionFactory.getCurrentSession().persist(page);
    }

    @Override
    public void update(Page page) {
        sessionFactory.getCurrentSession().merge(page);
    }

    @Override
    public void delete(Page page) {
        sessionFactory.getCurrentSession().delete(page);
    }
}
