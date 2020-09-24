import entities.User;
import orm.Connector;
import orm.EntityManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

public class Main {
    public static void main(String[] args) throws SQLException, IllegalAccessException, IOException {

        Connector.createConnection("root", "1234", "orm");

        EntityManager<User>entityManager = new EntityManager<>(Connector.getConnection());

        User user = new User(4,"petio", "4575", 34, new Date());
        entityManager.persist(user);

    }
}
