package org.acme.employeescheduling.solver;

import static org.optaplanner.core.api.score.stream.Joiners.equal;
import static org.optaplanner.core.api.score.stream.Joiners.greaterThan;
import static org.optaplanner.core.api.score.stream.Joiners.lessThan;
import static org.acme.employeescheduling.domain.AvailabilityType.DESIRED;
import static org.acme.employeescheduling.domain.AvailabilityType.UNDESIRED;
import static org.acme.employeescheduling.domain.AvailabilityType.UNAVAILABLE;

import java.time.Duration;
import java.time.LocalDateTime;

import org.acme.employeescheduling.domain.Availability;
import org.acme.employeescheduling.domain.AvailabilityType;
import org.acme.employeescheduling.domain.Shift;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.bi.BiConstraintStream;

public class EmployeeSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                requiredSkill(constraintFactory),
                noOverlappingShifts(constraintFactory),
                atLeast10HoursBetweenTwoShifts(constraintFactory),
                oneShiftPerDay(constraintFactory),
                unavailableEmployee(constraintFactory),
                desiredDayForEmployee(constraintFactory),
                undesiredDayForEmployee(constraintFactory),
        };
    }
    private static int getMinuteOverlap(Shift shift1, Shift shift2) {
        // The overlap of two timeslot occurs in the range common to both timeslots.
        // Both timeslots are active after the higher of their two start times,
        // and before the lower of their two end times.
        LocalDateTime shift1Start = shift1.getStart();
        LocalDateTime shift1End = shift1.getEnd();
        LocalDateTime shift2Start = shift2.getStart();
        LocalDateTime shift2End = shift2.getEnd();
        return (int) Duration.between((shift1Start.compareTo(shift2Start) > 0) ? shift1Start : shift2Start,
                (shift1End.compareTo(shift2End) < 0) ? shift1End : shift2End).toMinutes();
    }

    private static int getShiftDurationInMinutes(Shift shift) {
        return (int) Duration.between(shift.getStart(), shift.getEnd()).toMinutes();
    }
    
	private static BiConstraintStream<Availability, Shift> getConstraintStreamWithAvailabilityIntersections(
			ConstraintFactory constraintFactory, AvailabilityType availabilityType) {
		return constraintFactory.forEach(Availability.class)
				.filter(employeeAvailability -> employeeAvailability.getAvailabilityType() == availabilityType)
				.join(Shift.class, equal(Availability::getEmployee, Shift::getEmployee),
						lessThan(Availability::getStartDateTime, Shift::getEnd),
						greaterThan(Availability::getEndDateTime, Shift::getStart));
	}

//	@Override
//	public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
//		return new Constraint[] { 
//				noOverlappingShifts(constraintFactory), 
//				requiredSkill(constraintFactory),
//				assignEveryShift(constraintFactory),
//				doNotAssignAmy(constraintFactory),
//				unavailableEmployeeTimeSlot(constraintFactory), 
//				noMoreThanTwoConsecutiveShifts(constraintFactory),
//				atLeast10HoursBetweenTwoShifts(constraintFactory),
//				oneShiftPerDay(constraintFactory), 
//				unavailableEmployee(constraintFactory),
//				desiredDayForEmployee(constraintFactory), 
//				undesiredDayForEmployee(constraintFactory), 
//				undesiredEmployeeTimeSlot(constraintFactory),
//				desiredEmployeeTimeSlot(constraintFactory),
//				
//		};
//	}

	Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, Joiners.equal(Shift::getEmployee),
                        Joiners.overlapping(Shift::getStart, Shift::getEnd))
                .penalize(HardSoftScore.ONE_HARD,
                        EmployeeSchedulingConstraintProvider::getMinuteOverlap)
                .asConstraint("Overlapping shift");
    }
	
	 Constraint requiredSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> !shift.getEmployee().getSkillSet().contains(shift.getRequiredSkill()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Missing required skill");
    }
	
	 Constraint assignEveryShift(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachIncludingNullVars(Shift.class)
                .filter(shift -> shift.getEmployee() == null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Assign every shift");
    }

    Constraint oneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, Joiners.equal(Shift::getEmployee),
                        Joiners.equal(shift -> shift.getStart().toLocalDate()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Max one shift per day");
    }

	 Constraint doNotAssignAmy(ConstraintFactory constraintFactory) {
    	 return constraintFactory.forEach(Shift.class)
                 .filter(shift -> shift.getEmployee().getName().equals("Amy King"))
                 .penalize(HardSoftScore.ONE_SOFT)
                 .asConstraint("Don't assign Amy");
	}

	  Constraint assignEveryJob(ConstraintFactory constraintFactory) {
    	 return constraintFactory.forEach(Shift.class)
                 .filter(shift -> shift.getStart() == null && shift.getEnd() == null)
                 .penalize(HardSoftScore.ONE_SOFT)
                 .asConstraint("Assign every job");
	}

	
	 Constraint unavailableEmployeeTimeSlot(ConstraintFactory constraintFactory) {
		return constraintFactory.forEach(Shift.class).join(Availability.class)
				.filter((s1, a1) -> (s1.getStart().compareTo(a1.getStartDateTime()) < 0)
						&& (s1.getEnd().compareTo(a1.getEndDateTime()) > 0))
				.penalize(HardSoftScore.ONE_HARD, EmployeeSchedulingConstraintProvider::setLunchBreak)
				.asConstraint("Add lunch time break");
	}

	private static int setLunchBreak(Shift shift1, Availability availability2) {
		return 0;
	}

     Constraint noMoreThanTwoConsecutiveShifts(ConstraintFactory constraintFactory) {
    	 return constraintFactory.forEach(Shift.class)
                 .join(Shift.class,
                         equal(Shift::getEmployee),
                         equal(Shift::getEnd, Shift::getStart))
                 .join(Shift.class,
                         equal((s1, s2) -> s2.getEmployee(), Shift::getEmployee),
                         equal((s1, s2) -> s2.getEnd(), Shift::getStart))
                 .penalize(HardSoftScore.ONE_HARD, (s1, s2, s3) -> (int) s3.getLengthInMinutes())
                 .asConstraint("No more than 2 consecutive shifts");
    }
    
     Constraint atLeast10HoursBetweenTwoShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class,
                        Joiners.equal(Shift::getEmployee),
                        Joiners.lessThanOrEqual(Shift::getEnd, Shift::getStart))
                .filter((firstShift, secondShift) -> Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < 10)
                .penalize(HardSoftScore.ONE_HARD,
                        (firstShift, secondShift) -> {
                            int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
                            return (10 * 60) - breakLength;
                        })
                .asConstraint("At least 10 hours between 2 shifts");
    }

     Constraint unavailableEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.UNAVAILABLE)
                .penalize(HardSoftScore.ONE_HARD,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Unavailable employee");
    }

     Constraint desiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.DESIRED)
                .reward(HardSoftScore.ONE_SOFT,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Desired day for employee");
    }

     Constraint undesiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.UNDESIRED)
                .penalize(HardSoftScore.ONE_SOFT,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Undesired day for employee");
    }

	 Constraint undesiredEmployeeTimeSlot(ConstraintFactory constraintFactory) {
		return getConstraintStreamWithAvailabilityIntersections(constraintFactory, UNDESIRED)
				.penalize(HardSoftScore.ONE_HARD,
						(employeeAvailability, shift) -> (int) employeeAvailability.getDuration().toMinutes())
				.asConstraint("Undesired time slot for an employee");
	}

     Constraint desiredEmployeeTimeSlot(ConstraintFactory constraintFactory) {
    	return getConstraintStreamWithAvailabilityIntersections(constraintFactory, DESIRED)
				.penalize(HardSoftScore.ONE_SOFT,
						(employeeAvailability, shift) -> (int) employeeAvailability.getDuration().toMinutes())
				.asConstraint("Desired time slot for an employee");
    }


}
