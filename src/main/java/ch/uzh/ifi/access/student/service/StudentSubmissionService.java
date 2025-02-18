package ch.uzh.ifi.access.student.service;

import ch.uzh.ifi.access.course.model.Assignment;
import ch.uzh.ifi.access.course.model.Exercise;
import ch.uzh.ifi.access.student.SubmissionProperties;
import ch.uzh.ifi.access.student.dao.StudentSubmissionRepository;
import ch.uzh.ifi.access.student.model.CodeSubmission;
import ch.uzh.ifi.access.student.model.StudentSubmission;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentSubmissionService {

    private final StudentSubmissionRepository studentSubmissionRepository;

    private final SubmissionProperties submissionProperties;

    public StudentSubmissionService(StudentSubmissionRepository studentSubmissionRepository, SubmissionProperties submissionProperties) {
        this.studentSubmissionRepository = studentSubmissionRepository;
        this.submissionProperties = submissionProperties;
    }

    public List<StudentSubmission> findAll() {
        return studentSubmissionRepository.findAll();
    }

    public Optional<StudentSubmission> findById(String submissionId) {
        Assert.notNull(submissionId, "Cannot get by id: id is null");
        return studentSubmissionRepository.findById(submissionId);
    }

    public <T extends StudentSubmission> List<T> findAllSubmissionsByExerciseAndUserAndIsGradedOrderedByVersionDesc(String exerciseId, String userId, boolean isGraded) {
        Assert.notNull(exerciseId, "exerciseId cannot be null");
        Assert.notNull(userId, "userId cannot be null");

        return studentSubmissionRepository.findAllByExerciseIdAndUserIdAndIsGradedOrderByVersionDesc(exerciseId, userId, isGraded);
    }

    public <T extends StudentSubmission> T saveSubmission(T answer) {
        Assert.notNull(answer, "answer cannot be null");
        Assert.notNull(answer.getExerciseId(), "exerciseId cannot be null");
        Assert.notNull(answer.getUserId(), "userId cannot be null");

        return studentSubmissionRepository.save(answer);
    }

    public <T extends StudentSubmission> T initSubmission(T answer) {
        Assert.notNull(answer, "answer cannot be null");
        Assert.notNull(answer.getExerciseId(), "exerciseId cannot be null");
        Assert.notNull(answer.getUserId(), "userId cannot be null");

        Optional<StudentSubmission> previousSubmissions = findLatestExerciseSubmission(answer.getExerciseId(), answer.getUserId());
        previousSubmissions.ifPresent(prev -> answer.setVersion(prev.getVersion() + 1));

        if (!(answer instanceof CodeSubmission)) {
            answer.setGraded(true);
        }

        return studentSubmissionRepository.save(answer);
    }

    public <T extends StudentSubmission> Optional<T> findLatestExerciseSubmission(String exerciseId, String userId) {
        Assert.notNull(exerciseId, "exerciseId cannot be null");
        Assert.notNull(userId, "userId cannot be null");

        return studentSubmissionRepository.findTopByExerciseIdAndUserIdOrderByVersionDesc(exerciseId, userId);
    }

    /**
     * For a given assignment and user returns the last graded submitted version of each exercise
     *
     * @return last submission of each exercise
     */
    public List<StudentSubmission> findLatestGradedSubmissionsByAssignment(Assignment assignment, String userId) {
        Assert.notNull(assignment, "assignment cannot be null");
        Assert.notNull(userId, "userId cannot be null");
        List<String> exerciseIds = assignment.getExercises().stream().map(Exercise::getId).collect(Collectors.toList());

        return studentSubmissionRepository.findByExerciseIdInAndUserIdAndIsGradedOrderByVersionDesc(exerciseIds, userId);
    }

    public List<StudentSubmission> findLatestGradedInvalidatedSubmissionsByAssignment(Assignment assignment, String userId) {
        return findLatestGradedSubmissionsByAssignment(assignment, userId).stream().filter(StudentSubmission::isInvalid).collect(Collectors.toList());
    }

    public void invalidateSubmissionsByExerciseIdIn(List<String> exerciseIds) {
        if (exerciseIds != null) {
            exerciseIds.forEach(this::invalidateSubmissionsByExerciseId);
        }
    }

    public void invalidateSubmissionsByExerciseId(String exerciseId) {
        studentSubmissionRepository.invalidateSubmissionsByExerciseId(exerciseId);
    }

    public int getSubmissionCountByExerciseAndUser(String exerciseId, String userId) {
        return studentSubmissionRepository.countByExerciseIdAndUserIdAndIsInvalidFalseAndIsGradedTrueAndIsTriggeredReSubmissionFalse(exerciseId, userId);
    }

    public boolean isUserRateLimited(String userId) {
        return submissionProperties.isUserRateLimit() && hasUserCurrentlyRunningSubmissions(userId);
    }

    public boolean hasUserCurrentlyRunningSubmissions(String userId) {
        return studentSubmissionRepository.existsByUserIdAndHasNoResultOrConsoleNotOlderThan10min(userId);
    }

}
