package org.acme.employeescheduling.solver;

import org.acme.employeescheduling.domain.Shift;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

public class UserSchedulingConstraintProvider implements ConstraintProvider  {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                doNotAssignAmy(constraintFactory),
        };
    }

    Constraint doNotAssignAmy(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> shift.getEmployee().getName().equals("Amy King"))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Don't assign Amy");
    }

}
