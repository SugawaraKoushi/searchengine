package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class SiteDao implements Dao<Site> {
    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Site> get(int id) {
        Session session = sessionFactory.openSession();
        Site site = session.get(Site.class, id);
        session.close();

        return site == null ? Optional.empty() : Optional.of(site);
    }

    public Optional<Site> get(Site site) {
        Session session = sessionFactory.openSession();
        String query = "from " + Site.class.getSimpleName() + " where url like '" + site.getUrl() + "'";
        Site s = session.createQuery(query, Site.class).getSingleResult();
        session.close();

        return s == null ? Optional.empty() : Optional.of(s);
    }

    @Override
    public Optional<List<Site>> getAll() {
        Session session = sessionFactory.openSession();
        List<Site> sites = session.createQuery("from", Site.class).list();
        session.close();
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
