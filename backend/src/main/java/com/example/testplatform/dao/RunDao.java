package com.example.testplatform.dao;

import com.example.testplatform.model.Run;
import com.example.testplatform.model.RunDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class RunDao {

    private static final RowMapper<Run> RUN_MAPPER = (rs, i) -> new Run(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("case_id"),
            rs.getLong("env_id"),
            rs.getString("status"),
            rs.getInt("total"),
            rs.getInt("passed"),
            rs.getInt("failed"),
            rs.getInt("duration_ms"),
            rs.getString("started_at"),
            rs.getString("finished_at"),
            rs.getString("error_msg")
    );

    private static final RowMapper<RunDetail> DETAIL_MAPPER = (rs, i) -> new RunDetail(
            rs.getLong("id"),
            rs.getLong("run_id"),
            rs.getLong("step_id"),
            rs.getInt("sort"),
            rs.getString("status"),
            rs.getString("request_snapshot"),
            rs.getString("response_snapshot"),
            rs.getString("assertions_result"),
            rs.getInt("duration_ms"),
            rs.getString("error_msg"),
            rs.getString("api_name"),
            rs.getString("method")
    );

    private static final String RUN_COLUMNS =
            "id, project_id, case_id, env_id, status, total, passed, failed, duration_ms,"
                    + " started_at, finished_at, error_msg";

    private final JdbcTemplate jdbc;

    public RunDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long insert(Run r) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO run(project_id, case_id, env_id, status, total, passed, failed,"
                            + " duration_ms, started_at, finished_at, error_msg)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, r.projectId());
            ps.setLong(2, r.caseId());
            ps.setLong(3, r.envId());
            ps.setString(4, r.status());
            ps.setInt(5, r.total());
            ps.setInt(6, r.passed());
            ps.setInt(7, r.failed());
            ps.setInt(8, r.durationMs());
            ps.setString(9, r.startedAt());
            ps.setString(10, r.finishedAt());
            ps.setString(11, r.errorMsg());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void insertDetail(RunDetail d) {
        jdbc.update(
                "INSERT INTO run_detail(run_id, step_id, sort, status, request_snapshot,"
                        + " response_snapshot, assertions_result, duration_ms, error_msg)"
                        + " VALUES (?,?,?,?,?,?,?,?,?)",
                d.runId(), d.stepId(), d.sort(), d.status(), d.requestSnapshot(),
                d.responseSnapshot(), d.assertionsResult(), d.durationMs(), d.errorMsg());
    }

    /** 执行中持续更新进度，前端轮询能看到「跑到第几步」 */
    public int updateProgress(long id, String status, int total, int passed, int failed,
                              int durationMs, String finishedAt, String errorMsg) {
        return jdbc.update(
                "UPDATE run SET status = ?, total = ?, passed = ?, failed = ?, duration_ms = ?,"
                        + " finished_at = ?, error_msg = ? WHERE id = ?",
                status, total, passed, failed, durationMs, finishedAt, errorMsg, id);
    }

    public List<Run> listByProject(long projectId, int limit) {
        return jdbc.query("SELECT " + RUN_COLUMNS + " FROM run WHERE project_id = ? ORDER BY id DESC LIMIT ?",
                RUN_MAPPER, projectId, limit);
    }

    public Run findById(long id) {
        List<Run> list = jdbc.query("SELECT " + RUN_COLUMNS + " FROM run WHERE id = ?", RUN_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<RunDetail> listDetails(long runId) {
        return jdbc.query(
                "SELECT rd.id, rd.run_id, rd.step_id, rd.sort, rd.status, rd.request_snapshot,"
                        + " rd.response_snapshot, rd.assertions_result, rd.duration_ms, rd.error_msg,"
                        + " a.name AS api_name, a.method AS method"
                        + " FROM run_detail rd"
                        + " LEFT JOIN test_step s ON s.id = rd.step_id"
                        + " LEFT JOIN api a ON a.id = s.api_id"
                        + " WHERE rd.run_id = ? ORDER BY rd.sort, rd.id",
                DETAIL_MAPPER, runId);
    }

    public List<Run> listByCase(long caseId, int limit) {
        return jdbc.query("SELECT " + RUN_COLUMNS + " FROM run WHERE case_id = ? ORDER BY id DESC LIMIT ?",
                RUN_MAPPER, caseId, limit);
    }
}
