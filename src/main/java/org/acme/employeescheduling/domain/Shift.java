package org.acme.employeescheduling.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Entity
@PlanningEntity(pinningFilter = ShiftPinningFilter.class)
public class Shift {
    @Id
    @PlanningId
    @GeneratedValue
    Long id;

    LocalDateTime start;
    @Column(name = "endDateTime") // "end" clashes with H2 syntax.
    LocalDateTime end;

    String location;
    String requiredSkill;

    @PlanningVariable(valueRangeProviderRefs = "employeeRange")
    @ManyToOne
    Employee employee;
    
    @Transient
    private final AtomicLong lengthInMinutes = new AtomicLong(-1);

    public Shift() {
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill) {
        this(start, end, location, requiredSkill, null);
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill, Employee employee) {
        this(null, start, end, location, requiredSkill, employee);
    }

    public Shift(Long id, LocalDateTime start, LocalDateTime end, String location, String requiredSkill, Employee employee) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.location = location;
        this.requiredSkill = requiredSkill;
        this.employee = employee;
    }

    public long getLengthInMinutes() { // Thread-safe cache.
        long currentLengthInMinutes = lengthInMinutes.get();
        if (currentLengthInMinutes >= 0) {
            return currentLengthInMinutes;
        }
        long newLengthInMinutes = start.until(end, ChronoUnit.MINUTES);
        lengthInMinutes.set(newLengthInMinutes);
        return newLengthInMinutes;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    @Override
    public String toString() {
        return location + " " + start + "-" + end;
    }
    
    
}
