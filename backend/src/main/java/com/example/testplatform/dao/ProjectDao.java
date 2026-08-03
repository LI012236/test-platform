package com.example.testplatform.dao;

import com.example.testplatform.model.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProjectDao {

    private static final RowMapper<Project> MAPPER = (rs, i) -> new Project(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("owner_id"),
            rs.getString("created_at")
    );

    private static final String COLUMNS = "id, name, description, owner_id, created_at";

    private final JdbcTemplate jdbc;

    public ProjectDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Project> list(Long ownerId) {
        if (ownerId == null) {
            return jdbc.query("SELECT " + COLUMNS + " FROM project ORDER BY id DESC", MAPPER);
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM project WHERE owner_id = ? ORDER BY id DESC",
                MAPPER, ownerId);
    }

    public Project findById(long id) {
        List<Project> list = jdbc.query("SELECT " + COLUMNS + " FROM project WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(Project p) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO project(name, description, owner_id) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.name());
            ps.setString(2, p.description());
            ps.setLong(3, p.ownerId());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(long id, Project p) {
        return jdbc.update("UPDATE project SET name = ?, description = ? WHERE id = ?",
                p.name(), p.description(), id);
    }

    /** 级联删除项目下的所有数据（环境、接口、用例、步骤、执行记录） */
    public void deleteCascade(long projectId) {
        List<Object> args = new ArrayList<>();
        args.add(projectId);

        jdbc.update("DELETE FROM run_detail WHERE run_id IN (SELECT id FROM run WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM run WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM test_step WHERE case_id IN (SELECT id FROM test_case WHERE project_id = ?)", projectId);
        jdbc.update("DELETE FROM test_case WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM api WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM env WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM project WHERE id = ?", projectId);
    }
}
