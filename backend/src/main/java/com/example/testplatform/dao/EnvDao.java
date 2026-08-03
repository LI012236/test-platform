package com.example.testplatform.dao;

import com.example.testplatform.model.Env;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class EnvDao {

    private static final RowMapper<Env> MAPPER = (rs, i) -> new Env(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getString("base_url"),
            rs.getString("headers_json"),
            rs.getString("created_at")
    );

    private static final String COLUMNS = "id, project_id, name, base_url, headers_json, created_at";

    private final JdbcTemplate jdbc;

    public EnvDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Env> listByProject(long projectId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM env WHERE project_id = ? ORDER BY id",
                MAPPER, projectId);
    }

    public Env findById(long id) {
        List<Env> list = jdbc.query("SELECT " + COLUMNS + " FROM env WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsName(long projectId, String name, Long excludeId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM env WHERE project_id = ? AND name = ? AND id <> ?",
                Long.class, projectId, name, excludeId == null ? -1L : excludeId);
        return count != null && count > 0;
    }

    public Long insert(Env e) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO env(project_id, name, base_url, headers_json) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, e.projectId());
            ps.setString(2, e.name());
            ps.setString(3, e.baseUrl());
            ps.setString(4, e.headersJson());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(long id, Env e) {
        return jdbc.update("UPDATE env SET name = ?, base_url = ?, headers_json = ? WHERE id = ?",
                e.name(), e.baseUrl(), e.headersJson(), id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM env WHERE id = ?", id);
    }
}
