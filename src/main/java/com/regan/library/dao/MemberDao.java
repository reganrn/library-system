package com.regan.library.dao;

import com.regan.library.config.Db;
import com.regan.library.exception.LibraryException;
import com.regan.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read/write access to the MEMBERS table (plain JDBC). */
public class MemberDao {

    private static final String BASE_SELECT =
        "SELECT member_id, full_name, email, joined_date, active FROM members";

    public List<Member> findAll() {
        List<Member> members = new ArrayList<>();
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(BASE_SELECT + " ORDER BY full_name")) {
            while (rs.next()) members.add(map(rs));
        } catch (SQLException e) {
            throw new LibraryException("Failed to list members", e);
        }
        return members;
    }

    public Optional<Member> findById(long id) {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE_SELECT + " WHERE member_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to load member " + id, e);
        }
    }

    /** Registers a member; returns the generated MEMBER_ID. */
    public long insert(String fullName, String email) {
        String sql = "INSERT INTO members (full_name, email) VALUES (?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"MEMBER_ID"})) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to register member (duplicate email?)", e);
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        return new Member(
                rs.getLong("member_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getDate("joined_date").toLocalDate(),
                "Y".equals(rs.getString("active")));
    }
}
