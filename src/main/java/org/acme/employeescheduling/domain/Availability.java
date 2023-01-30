package org.acme.employeescheduling.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Availability {

    @PlanningId
    @Id
    @GeneratedValue
    Long id;

    @ManyToOne
    Employee employee;

    LocalDate date;

    AvailabilityType availabilityType;
    
    LocalDateTime startDateTime;
    @Column(name = "endDateTime") // "end" clashes with H2 syntax.
    LocalDateTime endDateTime;


    public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	
	@JsonIgnore
    public Duration getDuration() {
        return Duration.between(startDateTime, endDateTime);
    }
	
	public Availability() {
    }

    public Availability(Employee employee, LocalDate date, AvailabilityType availabilityType) {
        this.employee = employee;
        this.date = date;
        this.availabilityType = availabilityType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate localDate) {
        this.date = localDate;
    }

    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(AvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    @Override
    public String toString() {
        return availabilityType + "(" + employee + ", " + date + ")";
    }
}
