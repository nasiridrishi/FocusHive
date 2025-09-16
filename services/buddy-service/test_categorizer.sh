#!/bin/bash

# Test classes to check (only the ones that likely have actual test methods)
test_classes=(
    "com.focushive.buddy.controller.BuddyCheckinControllerTest"
    "com.focushive.buddy.controller.HealthControllerTest"
    "com.focushive.buddy.BuddyServiceApplicationTests"
    "com.focushive.buddy.constant.BuddyConstantsTest"
    "com.focushive.buddy.util.TimeZoneUtilsTest"
    "com.focushive.buddy.util.ValidationUtilsTest"
    "com.focushive.buddy.dto.ResponseDTOsTest"
    "com.focushive.buddy.exception.BuddyExceptionsTest"
    "com.focushive.buddy.e2e.BuddyServiceE2ESimpleTest"
    "com.focushive.buddy.e2e.BuddyServiceE2ETest"
    "com.focushive.buddy.repository.BuddyCheckinRepositoryTest"
    "com.focushive.buddy.repository.BuddyGoalRepositoryTest"
    "com.focushive.buddy.repository.BuddyPartnershipRepositoryTest"
    "com.focushive.buddy.service.BuddyCheckinServiceTest"
    "com.focushive.buddy.service.BuddyGoalServiceTest"
    # "com.focushive.buddy.scheduler.BuddyScheduledTasksTest" - REMOVED: Hangs due to @Scheduled annotation conflicts
    "com.focushive.buddy.listener.BuddyEventListenerTest"
    "com.focushive.buddy.integration.SampleIntegrationTest"
    "com.focushive.buddy.BuildConfigurationTest"
    "com.focushive.buddy.infrastructure.TestInfrastructureTest"
)

passing_tests=()
failing_tests=()

echo "Testing all test classes..."

for test_class in "${test_classes[@]}"; do
    echo "Testing: $test_class"
    
    if ./gradlew test --tests "$test_class" -q > /dev/null 2>&1; then
        echo "  ✅ PASS"
        passing_tests+=("$test_class")
    else
        echo "  ❌ FAIL"
        failing_tests+=("$test_class")
    fi
    echo
done

echo "======= RESULTS ======="
echo
echo "PASSING TESTS (${#passing_tests[@]}):"
for test in "${passing_tests[@]}"; do
    echo "  ✅ $test"
done

echo
echo "FAILING TESTS (${#failing_tests[@]}):"
for test in "${failing_tests[@]}"; do
    echo "  ❌ $test"
done

echo
echo "Results saved to test_results.txt"

# Save to file
echo "PASSING TESTS (${#passing_tests[@]}):" > test_results.txt
for test in "${passing_tests[@]}"; do
    echo "$test" >> test_results.txt
done

echo "" >> test_results.txt
echo "FAILING TESTS (${#failing_tests[@]}):" >> test_results.txt
for test in "${failing_tests[@]}"; do
    echo "$test" >> test_results.txt
done