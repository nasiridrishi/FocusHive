#!/usr/bin/env python3

import json
import os
from datetime import datetime
from pathlib import Path

def generate_markdown_report():
    """Generate a comprehensive markdown report from test results"""
    
    # Load results
    results_file = 'results/parsed_test_results.json'
    batch_execution_file = 'results/batch_execution_results.json'
    
    if not os.path.exists(results_file):
        print(f"Results file not found: {results_file}")
        return
    
    with open(results_file, 'r') as f:
        test_results = json.load(f)
    
    batch_execution_results = None
    if os.path.exists(batch_execution_file):
        with open(batch_execution_file, 'r') as f:
            batch_execution_results = json.load(f)
    
    # Generate report
    report_content = f"""# Test Failure Analysis Report

## Executive Summary

**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

This report documents the systematic analysis of test failures across {test_results['total_batches']} test batches in the Identity Service project.

### Key Metrics

| Metric | Value |
|--------|-------|
| Total Test Batches | {test_results['total_batches']} |
| Total Tests Executed | {test_results['total_tests']} |
| Tests Passed | {test_results['total_passed']} |
| Tests Failed | {test_results['total_failed']} |
| Tests with Errors | {test_results['total_errors']} |
| Tests Skipped | {test_results['total_skipped']} |
| Overall Pass Rate | {(test_results['total_passed'] / test_results['total_tests'] * 100) if test_results['total_tests'] > 0 else 0:.1f}% |

## Root Cause Analysis

### Primary Issues Identified

"""

    # Add error patterns
    if test_results['overall_error_patterns']:
        report_content += "### Error Pattern Distribution\n\n"
        for error_type, count in sorted(test_results['overall_error_patterns'].items(), key=lambda x: x[1], reverse=True):
            percentage = (count / test_results['total_tests'] * 100) if test_results['total_tests'] > 0 else 0
            report_content += f"- **{error_type}**: {count} occurrences ({percentage:.1f}%)\n"
        report_content += "\n"
    
    # Add top failure causes
    if test_results['top_failure_causes']:
        report_content += "### Top Failure Root Causes\n\n"
        for i, cause in enumerate(test_results['top_failure_causes'][:10], 1):
            report_content += f"{i}. **{cause['cause']}** ({cause['count']} occurrences)\n"
        report_content += "\n"
    
    # Critical Findings
    report_content += """## Critical Findings

### 🚨 Test Discovery Issue

**Issue**: All failing tests show `NoTestsDiscoveredException` errors, indicating that JUnit is not discovering the individual test classes properly.

**Root Cause**: The current test execution strategy is attempting to run JUnit 5 Test Suite classes (`NetworkFailureTestSuite`, `ResilienceTestSuite`) rather than individual test methods.

**Impact**: 
- Tests are organized in suite classes but the gradle `--tests` filter is not compatible with suite-based test organization
- Individual test classes within suites are not being executed
- False negative results - tests may actually be working but are not being discovered

### 📋 Recommendations

#### Immediate Actions (Priority 1)

1. **Fix Test Discovery Strategy**
   - Change the gradle test execution to run individual test classes instead of suite classes
   - Use class-based filtering: `./gradlew test --tests "*.LoginRequestUnitTest"`
   - Or use package-based filtering: `./gradlew test --tests "com.focushive.identity.dto.*"`

2. **Verify Test Class Structure**
   - Review test classes to ensure they follow proper JUnit 5 naming and annotation patterns
   - Ensure test methods are properly annotated with `@Test`

3. **Update Batch Execution Strategy**
   - Modify the batch execution script to target individual test classes
   - Remove suite classes from the test execution scope initially

#### Medium-term Actions (Priority 2)

1. **Improve Test Organization**
   - Review and potentially restructure test suites
   - Ensure suite classes are properly configured if they need to be maintained
   - Consider separating suite execution from individual test execution

2. **Enhance Test Filtering**
   - Implement more granular test filtering strategies
   - Add support for both individual and suite-based test execution

#### Long-term Actions (Priority 3)

1. **Test Infrastructure Improvements**
   - Implement parallel test execution where appropriate
   - Add comprehensive test reporting and monitoring
   - Set up continuous integration with proper test failure analysis

## Detailed Batch Results

"""

    # Add batch-by-batch analysis
    for batch in test_results['batch_results']:
        report_content += f"### Batch: {batch['batch_name']}\n\n"
        
        report_content += f"**Summary**: {batch['total_tests']} tests, "
        report_content += f"{batch['passed_tests']} passed, "
        report_content += f"{batch['failed_tests']} failed, "
        report_content += f"{batch['error_tests']} errors, "
        report_content += f"{batch['skipped_tests']} skipped\n\n"
        
        if batch['failure_summary']:
            report_content += "**Failures**:\n\n"
            for failure in batch['failure_summary']:
                report_content += f"- `{failure['test_name']}`\n"
                report_content += f"  - **Error**: {failure['failure_type']}\n"
                report_content += f"  - **Cause**: {failure['root_cause'][:200]}{'...' if len(failure['root_cause']) > 200 else ''}\n\n"
        
        report_content += "---\n\n"
    
    # Add execution timing if available
    if batch_execution_results:
        report_content += f"""## Execution Performance

### Timing Analysis

| Metric | Value |
|--------|-------|
| Total Execution Time | {batch_execution_results['total_duration_minutes']:.1f} minutes |
| Average Batch Time | {batch_execution_results['total_duration_minutes'] / batch_execution_results['total_batches']:.1f} minutes |
| Successful Batches | {batch_execution_results['successful_batches']}/{batch_execution_results['total_batches']} |

### Batch Performance

"""
        
        for batch_result in batch_execution_results['batch_results']:
            status_emoji = "✅" if batch_result['success'] else "❌"
            report_content += f"- {status_emoji} **{batch_result['batch_name']}**: {batch_result['duration_minutes']:.1f}m"
            if not batch_result['success']:
                report_content += f" (Exit Code: {batch_result['exit_code']})"
            report_content += "\n"
        
        report_content += "\n"
    
    # Next steps
    report_content += """## Next Steps

### Immediate Next Actions

1. **Fix the test discovery issue**:
   ```bash
   # Try running a single test to verify the approach
   ./gradlew test --tests "com.focushive.identity.dto.LoginRequestUnitTest"
   
   # Or try package-based execution
   ./gradlew test --tests "com.focushive.identity.dto.*Test"
   ```

2. **Update the batch execution script** to use proper test class names instead of suite names

3. **Re-run the first few batches** with the corrected approach

4. **Document working patterns** for future batch executions

### Investigation Tasks

- [ ] Review the structure of suite classes and their purpose
- [ ] Identify which tests are meant to be run individually vs as suites
- [ ] Verify test annotations and setup across all test classes
- [ ] Check for any Spring Boot test context issues

### Success Criteria

- [ ] At least 80% of individual unit tests (DTO, entity) should pass
- [ ] Integration tests should show meaningful failures (not discovery errors)
- [ ] Test execution time should be under 2 hours for full test suite

---

*This report was generated automatically by the test batch analysis system.*
"""
    
    # Write the report
    os.makedirs('reports', exist_ok=True)
    report_file = 'reports/test_failure_analysis_report.md'
    
    with open(report_file, 'w') as f:
        f.write(report_content)
    
    print(f"📊 Comprehensive test failure report generated: {report_file}")
    
    # Also generate a CSV summary for easy analysis
    generate_csv_summary(test_results)

def generate_csv_summary(test_results):
    """Generate a CSV summary of test results"""
    import csv
    
    csv_file = 'reports/test_results_summary.csv'
    
    with open(csv_file, 'w', newline='') as f:
        writer = csv.writer(f)
        
        # Write header
        writer.writerow(['batch_name', 'total_tests', 'passed', 'failed', 'errors', 'skipped', 'pass_rate', 'duration_seconds'])
        
        # Write batch results
        for batch in test_results['batch_results']:
            pass_rate = (batch['passed_tests'] / batch['total_tests'] * 100) if batch['total_tests'] > 0 else 0
            writer.writerow([
                batch['batch_name'],
                batch['total_tests'],
                batch['passed_tests'],
                batch['failed_tests'],
                batch['error_tests'],
                batch['skipped_tests'],
                f"{pass_rate:.1f}%",
                batch['total_duration_seconds']
            ])
    
    print(f"📈 CSV summary generated: {csv_file}")

def main():
    generate_markdown_report()

if __name__ == "__main__":
    main()