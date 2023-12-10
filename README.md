# Search engine

Поисковый движок по одному или нескольким сайтам.

# Стэк используемых технологий

Языки:
- Java;
- HTML.

Фреймворки:
- Spring.

# Инструкция для запуска
1. Установител [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html).

1. Создайте в корне проекта файл `application.yaml` со следующим содержимым:
```yaml
server:
  port: 8080

spring:
  datasource:
    username: %username%
    password: %password%
    url: %url_to_db%
  jpa:
    properties:
      hibernate:
        dialect: %hibernate_db_dialect%
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
    hibernate:
      ddl-auto: %create || update%
    show-sql: false

indexing-settings:
  sites:
    - url: %site_url%
      name: %site_name%
```
В параметр `username` укажите пользователя БД, в `password` - его пароль, в `url` - адрес для подключения к БД.

В `dialect` укажите диалект для БД, которой вы будете использовать.

Для первого запуска укажите `create` для параметра `ddl-auto`. Для последующего использования используйте `update`.

2. Создайте в папке `./searchengine/src/main/resources/` файл `hibernate.cfg.xml` со следующим содержимым:
```xml
<!DOCTYPE hibernate-configuration PUBLIC
        "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
        "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
<hibernate-configuration>
    <session-factory>
        <!-- JDBC Database connection settings -->
        <property name="hibernate.jdbc.batch_size">20</property>
        <property name="connection.driver_class">%db_driver%</property>
        <property name="connection.url">%url_to_db%</property>
        <property name="connection.username">%username%</property>
        <property name="connection.password">%password%</property>
        <!-- JDBC connection pool settings ... using built-in test pool -->
        <property name="connection.pool_size">100</property>
        <!-- Select our SQL dialect -->
        <property name="dialect">%hibenrate_db_dialect%</property>
        <!-- Echo the SQL to stdout -->
        <property name="show_sql">false</property>
        <!-- Set the current session context -->
        <property name="current_session_context_class">thread</property>
        <!-- Drop and re-create the database schema on startup -->
        <property name="hbm2ddl.auto">%create || update%</property>

        <mapping class="searchengine.model.Site"/>
        <mapping class="searchengine.model.Page"/>
        <mapping class="searchengine.model.Lemma"/>
        <mapping class="searchengine.model.Index"/>
    </session-factory>
</hibernate-configuration>
```
В параметр `connection.driver_class` укажите драйвер БД, которой будете использовать.

В параметр `connection.username` укажите пользователя БД, в `connection.password` - его пароль, в `connection.url` - адрес для подключения к БД.

В `dialect` укажите диалект для БД, которой вы будете использовать.

Для первого запуска укажите `create` для параметра `hbm2ddl.auto`. Для последующего использования используйте `update`.

3. Перейдите по адресу http://localhots:8080/.
