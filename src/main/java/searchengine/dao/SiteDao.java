package searchengine.dao;

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
    public Optional<Site> get(long id) {
        Site site = sessionFactory.getCurrentSession().get(Site.class, id);
        return Optional.of(site);
    }

    @Override
    public Optional<List<Site>> getAll() {
        List<Site> sites = sessionFactory.getCurrentSession().createQuery("from", Site.class).list();
        return Optional.of(sites);
    }

    @Override
    public void save(Site site) {
        Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();
        sessionFactory.getCurrentSession().persist(site);
        transaction.commit();
    }

    @Override
    public void saveAll(Collection<Site> sites) {
        Transaction transaction = sessionFactory.getCurrentSession().beginTransaction();
        for (Site site : sites) {
            sessionFactory.getCurrentSession().persist(site);
        }
        transaction.commit();
    }

    @Override
    public void update(Site site) {
        sessionFactory.getCurrentSession().merge(site);
    }

    @Override
    public void delete(Site site) {
        sessionFactory.getCurrentSession().delete(site);
    }
}
