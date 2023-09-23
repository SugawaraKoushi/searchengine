package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.util.HibernateUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PageDao implements Dao<Page> {
    @Autowired
    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public Optional<Page> get(int id) {
        Session session = sessionFactory.openSession();
        Page page = session.get(Page.class, id);
        session.close();
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
        List<Page> pages;

        try {
            pages = session.createQuery("from " + Page.class.getSimpleName(), Page.class).getResultList();
        } catch (Exception e) {
            pages = new ArrayList<>();
        } finally {
            session.close();
        }

        return Optional.of(pages);
    }

    public Optional<List<Page>> getListByIndexes(Collection<Index> indexes) {
        Session session = sessionFactory.openSession();
        List<Integer> i = new ArrayList<>();
        indexes.forEach(index -> i.add(index.getPage().getId()));
        Object[] ids = i.toArray();

        Query<Page> query = session.createQuery("from Page where id in :ids");
        query.setParameterList("ids", ids);
        List<Page> pages = query.getResultList();

        return pages.isEmpty() ? Optional.empty() : Optional.of(pages);
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

        session.remove(page);
        transaction.commit();
        session.close();
    }
}
