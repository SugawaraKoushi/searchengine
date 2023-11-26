package searchengine.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY = createSessionFactory();

    public static SessionFactory createSessionFactory() {
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .configure("hibernate.cfg.xml")
                .build();
        Metadata metadata = new MetadataSources(serviceRegistry).getMetadataBuilder().build();
        return metadata.getSessionFactoryBuilder().build();
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
