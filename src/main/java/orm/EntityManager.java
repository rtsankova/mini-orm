package orm;

import annotation.Column;
import annotation.Entity;
import annotation.Id;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class EntityManager<E> implements DbContext<E> {
    private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field primary = this.getId(entity.getClass());
        primary.setAccessible(true);
        Object value = primary.get(entity);
        if (value == null || (int) value <= 0) {
            return this.doInsert(entity, primary);
        }
        return this.doUpdate(entity, primary);
    }

    private boolean doUpdate(E entity, Field primary) throws SQLException, IllegalAccessException {
        String query = "UPDATE " + this.getTableName(entity.getClass()) + " SET ";
        String columnAndValue = "";
        String where = "";

        Field[] fields = entity.getClass().getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);

            Object value = field.get(entity);
            if (field.isAnnotationPresent(Id.class)) {
                where += " where " + this.getColumnName(field) + " = " + value;
            } else {


                if (value instanceof Date) {
                    columnAndValue += this.getColumnName(field) + " = "
                            + "'" + new SimpleDateFormat("yyyy-MM-dd").format(value) + "'";
                } else if(value instanceof Integer){
                    columnAndValue +=   this.getColumnName(field) + " = " + value;
                }
                else {
                    columnAndValue +=   this.getColumnName(field) + " = '" + value + "'";
                }

                if (i < fields.length - 1) {
                    columnAndValue += ", ";

                }
            }
        }

        query += columnAndValue + where;
        return connection.prepareStatement(query).execute();
    }

    private boolean doInsert(E entity, Field primary) throws SQLException, IllegalAccessException {
        String query = "INSERT INTO " + this.getTableName(entity.getClass()) + " (";
        String columns = "";
        String values = "";

        Field[] fields = entity.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            if (!field.getName().equals(primary.getName())) {
                columns += '`' + this.getColumnName(field) + '`';
                Object value = field.get(entity);
                if (value instanceof Date) {
                    values += "'" + new SimpleDateFormat("yyyy-MM-dd").format(value) + "'";
                } else {
                    values += "'" + value + "'";
                }

                if (i < fields.length - 1) {
                    columns += ", ";
                    values += ", ";
                }
            }
        }
        query += columns + ") " + "VALUES(" + values + ")";
        return connection.prepareStatement(query).execute();
    }

    private String getColumnName(Field field) {
        String columnName = "";

        columnName = field.getAnnotation(Column.class).name();

        if (columnName.isEmpty()) {
            columnName = field.getName();
        }

        return columnName;
    }

    private String getTableName(Class entity) {
        String tableName = "";
        tableName = ((Entity) entity.getAnnotation(Entity.class)).name();

        if (tableName.isEmpty()) {
            tableName = entity.getSimpleName();
        }

        return tableName;
    }

    private Field getId(Class entity) {
        return Arrays.stream(entity.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst().orElseThrow(() -> new UnsupportedOperationException("Entity doesn't have primary key"));
    }

    @Override
    public Iterable<E> find(Class<E> table) {
        // TODO
        return null;
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) {
        // TODO
        return null;
    }

    @Override
    public E findFirst(Class<E> table) {
        // TODO
        return null;
    }

    @Override
    public E findFirst(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException {
        Statement stmt = connection.createStatement();
        String	query = "Select * FROM" + this.getTableName(table) +
                "WHERE 1 " +  (where != null ? "AND" + where : "") + "LIMIT 1";
        ResultSet rs = stmt.executeQuery(query);
        E entity = table.newInstance();
        rs.next();
        this.fillEntity(table, rs, entity);
        return entity;
    }

    private void fillEntity(Class<E> table, ResultSet rs, E entity) throws SQLException, IllegalAccessException {
        Field[] fields = table.getFields();
        for (Field field : fields) {
            field.setAccessible(true);
            this.fillField(field, entity, rs, field.getAnnotation(Column.class).name());
        }
    }

    private void fillField(Field field, E entity, ResultSet rs, String fieldName) throws IllegalArgumentException, IllegalAccessException, SQLException {
        field.setAccessible(true);
        if (field.getType() == int.class || field.getType() == Integer.class) {
            field.set(entity, rs.getInt(fieldName));
        } else if (field.getType() == long.class || field.getType() == Long.class) {
            field.set(entity, rs.getLong(fieldName));
        } else if (field.getType() == String.class) {
            field.set(entity, rs.getString(fieldName));
        } else if (field.getType() == Date.class) {
            field.set(entity, rs.getDate(fieldName));
        }
    }
}
