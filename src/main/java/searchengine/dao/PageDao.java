package searchengine.dao;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.util.HibernateUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PageDao implements Dao<Page> {
    private Logger logger = LoggerFactory.getLogger(PageDao.class);
    @Autowired
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Page> get(int id) {
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

    public void saveAll(Collection<Page> pages) {
        Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();
        for (Page page : pages) {
            logger.info("Persisting page: " + page.getSite().getUrl() + page.getPath());
            sessionFactory.getCurrentSession().persist(page);
        }
        transaction.commit();
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
