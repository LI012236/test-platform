package com.example.testplatform.dao;

import com.example.testplatform.model.TestCase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class CaseDao {

    private static final RowMapper<TestCase> MAPPER = (rs, i) -> new TestCase(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("created_at")
    );

    private static final String COLUMNS = "id, project_id, name, description, created_at";

    private final JdbcTemplate jdbc;

    public CaseDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TestCase> listByProject(long projectId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM test_case WHERE project_id = ? ORDER BY id DESC",
                MAPPER, projectId);
    }

    public TestCase findById(long id) {
        List<TestCase> list = jdbc.query("SELECT " + COLUMNS + " FROM test_case WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(TestCase c) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_case(project_id, name, description) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, c.projectId());
            ps.setString(2, c.name());
            ps.setString(3, c.description());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(long id, TestCase c) {
        return jdbc.update("UPDATE test_case SET name = ?, description = ? WHERE id = ?",
                c.name(), c.description(), id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM test_case WHERE id = ?", id);
    }
}
