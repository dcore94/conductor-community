package com.netflix.conductor.validations;

import com.netflix.conductor.common.metadata.workflow.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;

/**
 * This constraint class validates following things.
 * 1. TaskDef for given name exists or not.
 * 2. Correct parameters are set depending on task type.
 */
@Documented
@Constraint(validatedBy = WorkflowTaskConstraint.WorkflowTaskValidator.class)
@Target({TYPE,  ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkflowTaskConstraint {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class WorkflowTaskValidator implements ConstraintValidator<WorkflowTaskConstraint, WorkflowTask> {

        final String PARAM_REQUIRED_STRING_FORMAT = "%s field is required for taskType: %s taskName: %s";

        @Override
        public void initialize(WorkflowTaskConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();

            boolean valid = true;

            if (ValidationContext.getMetadataDAO().getTaskDef(workflowTask.getName()) == null){
                String message = String.format("taskDef %s by name does not exists", workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }

            // depending on task type check if required parameters are set or not
            switch (workflowTask.getType()) {
                case TaskType.TASK_TYPE_EVENT:
                    try {
                        valid = isEventTaskValid(workflowTask, context);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    break;
                case TaskType.TASK_TYPE_DECISION:
                    valid = isDecisionTaskValid(workflowTask, context);
                    break;
                case TaskType.TASK_TYPE_DYNAMIC:
                    valid = isDynamicTaskValid(workflowTask, context);
                    break;
                case TaskType.TASK_TYPE_FORK_JOIN_DYNAMIC:
                    valid = isDynamicForkJoinValid(workflowTask, context);
                    break;
                case TaskType.TASK_TYPE_HTTP:
                    valid = isHttpTaskValid(workflowTask, context);
                    break;
                case TaskType.TASK_TYPE_FORK_JOIN:
                    valid = isForkJoinTaskValid(workflowTask, context);
                    break;
                case TaskType.TASK_TYPE_JOIN:
                    valid = isJoinTaskValid(workflowTask, context);
                    break;
            }

            return valid;
        }

        private boolean isEventTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) throws NoSuchFieldException {
            boolean valid = true;
            if (workflowTask.getSink() == null){
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "sink", TaskType.TASK_TYPE_EVENT, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            return valid;
        }

        private boolean isDecisionTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;
            if (workflowTask.getCaseValueParam() == null){
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "caseValueParam", TaskType.DECISION, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            if (workflowTask.getDecisionCases() == null) {
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "decisionCases", TaskType.DECISION, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            else if (workflowTask.getDecisionCases() != null && (workflowTask.getDecisionCases().size() == 0)){
                String message = String.format("decisionCases should have atleast one task for taskType: %s taskName: %s", TaskType.DECISION, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            return valid;
        }

        private boolean isDynamicTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;
            if (workflowTask.getDynamicTaskNameParam() == null){
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "dynamicTaskNameParam", TaskType.DYNAMIC, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }

            return valid;
        }

        private boolean isDynamicForkJoinValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;

            if (workflowTask.getDynamicForkTasksParam() == null) {
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "dynamicForkTasksParam", TaskType.FORK_JOIN_DYNAMIC, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            if (workflowTask.getDynamicForkTasksInputParamName() == null) {
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "dynamicForkTasksInputParamName", TaskType.FORK_JOIN_DYNAMIC, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            return valid;
        }

        private boolean isHttpTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;

            if (workflowTask.getInputParameters() == null || !workflowTask.getInputParameters().containsKey("http_request")) {
                String message = String.format(PARAM_REQUIRED_STRING_FORMAT, "inputParameters.http_request", TaskType.HTTP, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            return valid;
        }

        private boolean isForkJoinTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;

             if (workflowTask.getForkTasks() != null && (workflowTask.getForkTasks().size() == 0)){
                String message = String.format("forkTasks should have atleast one task for taskType: %s taskName: %s", TaskType.FORK_JOIN, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }

            return valid;
        }

        private boolean isJoinTaskValid(WorkflowTask workflowTask, ConstraintValidatorContext context) {
            boolean valid = true;
            if (workflowTask.getJoinOn() != null && (workflowTask.getJoinOn().size() == 0)){
                String message = String.format("joinOn should have atleast one value for taskType: %s taskName: %s", TaskType.JOIN, workflowTask.getName());
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                valid = false;
            }
            return valid;
        }
    }
}