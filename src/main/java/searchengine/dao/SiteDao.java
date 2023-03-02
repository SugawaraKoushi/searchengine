package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SiteDao implements Dao<Site>{
    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Site> get(int id) {
        Session session = sessionFactory.openSession();
        Site site = session.get(Site.class, id);
        session.close();

        return site == null ? Optional.empty() : Optional.of(site);
    }

    @Override
    public Optional<List<Site>> getAll() {
        Session session = sessionFactory.openSession();
        List<Site> sites = session.createQuery("from", Site.class).list();
        return Optional.of(sites);
    }

    @Override
    public void save(Site site) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.persist(site);
        transaction.commit();
        session.close();
    }

    @Override
    public int saveAll(Collection<Site> sites) {
        try {
            Session session = sessionFactory.openSession();
            Transaction transaction = session.beginTransaction();

            for (Site site : sites) {
                session.persist(site);
            }

            transaction.commit();
            session.close();
        } catch (Exception e) {
            return -1;
        }

    return 0;
    }

    @Override
    public void update(Site site) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.merge(site);
        transaction.commit();
        session.close();
    }

    @Override
    public void delete(Site site) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.delete(site);
        transaction.commit();
        session.close();
    }
}
