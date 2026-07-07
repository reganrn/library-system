package com.regan.library.model;

import java.time.LocalDate;

// A registered library member.
public class Member {

    private final long id;
    private final String fullName;
    private final String email;
    private final LocalDate joinedDate;
    private final boolean active;

    public Member(long id, String fullName, String email, LocalDate joinedDate, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.joinedDate = joinedDate;
        this.active = active;
    }

    public long getId()            { return id; }
    public String getFullName()    { return fullName; }
    public String getEmail()       { return email; }
    public LocalDate getJoinedDate(){ return joinedDate; }
    public boolean isActive()      { return active; }

    @Override
    public String toString() {
        return String.format("[%d] %s <%s> joined %s%s",
                id, fullName, email, joinedDate, active ? "" : " (INACTIVE)");
    }
}
