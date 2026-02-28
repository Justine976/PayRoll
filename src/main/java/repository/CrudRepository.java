package repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {
    T save(T entity) throws SQLException;

    Optional<T> findById(ID id) throws SQLException;

    List<T> findAll(int limit, int offset) throws SQLException;

    boolean update(T entity) throws SQLException;

    boolean deleteById(ID id) throws SQLException;
}
