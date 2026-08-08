package com.example.testplatform.dao;

import com.example.testplatform.model.Api;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ApiDao {

    private static final RowMapper<Api> MAPPER = (rs, i) -> new Api(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("method"),
            rs.getString("path"),
            rs.getString("headers_json"),
            rs.getString("body"),
            rs.getString("created_at")
    );

    private static final String COLUMNS = "id, project_id, name, method, path, headers_json, body, created_at";

    private final JdbcTemplate jdbc;

    public ApiDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Api> listByProject(long projectId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM api WHERE project_id = ? ORDER BY id DESC",
                MAPPER, projectId);
    }

    public Api findById(long id) {
        List<Api> list = jdbc.query("SELECT " + COLUMNS + " FROM api WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(Api a) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO api(project_id, name, method, path, headers_json, body) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.projectId());
            ps.setString(2, a.name());
            ps.setString(3, a.method());
            ps.setString(4, a.path());
            ps.setString(5, a.headersJson());
            ps.setString(6, a.body());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(long id, Api a) {
        return jdbc.update("UPDATE api SET name = ?, method = ?, path = ?, headers_json = ?, body = ? WHERE id = ?",
                a.name(), a.method(), a.path(), a.headersJson(), a.body(), id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM api WHERE id = ?", id);
    }

    /** 删除接口前检查是否被用例步骤引用 */
    public int countUsedByStep(long apiId) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM test_step WHERE api_id = ?", Integer.class, apiId);
        return c == null ? 0 : c;
    }
}
