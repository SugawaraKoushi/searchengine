package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class PageDao implements Dao<Page> {
    private final Logger logger = LoggerFactory.getLogger(PageDao.class);
    @Autowired
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public Optional<Page> get(int id) {
        Session session = sessionFactory.openSession();
        Page page = session.get(Page.class, id);
        return page == null ? Optional.empty() : Optional.of(page);
    }

    public Optional<Page> get(Page page) {
        Session session = sessionFactory.openSession();
        Page p;

        try {
            String query = "from " + Page.class.getSimpleName() + " where path = '" + page.getPath() +
                    "' and site_id = " + page.getSite().getId();
            p = session.createQuery(query, Page.class).getSingleResult();
        } catch (Exception e) {
            p = null;
        } finally {
            session.close();
        }

        return p == null ? Optional.empty() : Optional.of(p);
    }

    @Override
    public Optional<List<Page>> getAll() {
        Session session = sessionFactory.openSession();
        List<Page> pages = session.createQuery("from", Page.class).getResultList();
        session.close();

        return Optional.of(pages);
    }

    @Override
    public void save(Page page) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.persist(page);
        transaction.commit();
        session.close();
    }

    @Override
    public void update(Page page) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.merge(page);
        transaction.commit();
        session.close();
    }

    @Override
    public void delete(Page page) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.delete(page);
        transaction.commit();
        session.close();
    }
}
