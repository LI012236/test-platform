package com.example.testplatform.dao;

import com.example.testplatform.model.TestStep;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class StepDao {

    private static final RowMapper<TestStep> MAPPER = (rs, i) -> new TestStep(
            rs.getLong("id"),
            rs.getLong("case_id"),
            rs.getInt("sort"),
            rs.getLong("api_id"),
            rs.getString("api_name"),
            rs.getString("method"),
            rs.getString("path"),
            rs.getString("override_headers"),
            rs.getString("override_body"),
            rs.getString("extractors_json"),
            rs.getString("assertions_json"),
            rs.getString("created_at")
    );

    private static final String SELECT_BASE =
            "SELECT s.id, s.case_id, s.sort, s.api_id, a.name AS api_name, a.method, a.path,"
                    + " s.override_headers, s.override_body, s.extractors_json, s.assertions_json, s.created_at"
                    + " FROM test_step s JOIN api a ON a.id = s.api_id";

    private final JdbcTemplate jdbc;

    public StepDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TestStep> listByCase(long caseId) {
        return jdbc.query(SELECT_BASE + " WHERE s.case_id = ? ORDER BY s.sort, s.id", MAPPER, caseId);
    }

    public TestStep findById(long id) {
        List<TestStep> list = jdbc.query(SELECT_BASE + " WHERE s.id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(TestStep s) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO test_step(case_id, sort, api_id, override_headers, override_body,"
                            + " extractors_json, assertions_json) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, s.caseId());
            ps.setInt(2, s.sort() == null ? 1 : s.sort());
            ps.setLong(3, s.apiId());
            ps.setString(4, s.overrideHeaders());
            ps.setString(5, s.overrideBody());
            ps.setString(6, s.extractorsJson());
            ps.setString(7, s.assertionsJson());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(long id, TestStep s) {
        return jdbc.update(
                "UPDATE test_step SET sort = ?, api_id = ?, override_headers = ?, override_body = ?,"
                        + " extractors_json = ?, assertions_json = ? WHERE id = ?",
                s.sort(), s.apiId(), s.overrideHeaders(), s.overrideBody(),
                s.extractorsJson(), s.assertionsJson(), id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM test_step WHERE id = ?", id);
    }

    public int deleteByCase(long caseId) {
        return jdbc.update("DELETE FROM test_step WHERE case_id = ?", caseId);
    }

    public int maxSort(long caseId) {
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(sort), 0) FROM test_step WHERE case_id = ?",
                Integer.class, caseId);
        return max == null ? 0 : max;
    }
}
