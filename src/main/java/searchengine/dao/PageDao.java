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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PageDao implements Dao<Page> {
    private Logger logger = LoggerFactory.getLogger(PageDao.class);
    @Autowired
    private SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Page> get(int id) {
        Session session = sessionFactory.openSession();
        Page page = session.get(Page.class, id);
        return Optional.of(page);
    }

    @Override
    public Optional<List<Page>> getAll() {
        Session session = sessionFactory.openSession();
        List<Page> pages = session.createQuery("from", Page.class).list();
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

    public int saveAll(Collection<Page> pages) {
        try {
            Session session = sessionFactory.openSession();
            Transaction transaction = session.beginTransaction();

            for (Page page : pages) {
                session.persist(page);
            }

            transaction.commit();
            session.close();
        } catch (Exception e) {
            return -1;
        }

        return 0;
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
