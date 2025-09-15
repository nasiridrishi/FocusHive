#!/usr/bin/env python3

import os
import json
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict
import re

def parse_junit_xml(xml_file):
    """Parse a single JUnit XML file and extract test results"""
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        
        # Handle different XML structures
        if root.tag == 'testsuite':
            testsuites = [root]
        elif root.tag == 'testsuites':
            testsuites = root.findall('testsuite')
        else:
            print(f"Warning: Unknown XML root tag '{root.tag}' in {xml_file}")
            return None
        
        results = []
        
        for testsuite in testsuites:
            suite_name = testsuite.get('name', 'Unknown')
            suite_tests = int(testsuite.get('tests', 0))
            suite_failures = int(testsuite.get('failures', 0))
            suite_errors = int(testsuite.get('errors', 0))
            suite_skipped = int(testsuite.get('skipped', 0))
            suite_time = float(testsuite.get('time', 0.0))
            
            # Process individual test cases
            for testcase in testsuite.findall('testcase'):
                test_name = testcase.get('name', 'Unknown')
                test_classname = testcase.get('classname', 'Unknown')
                test_time = float(testcase.get('time', 0.0))
                
                # Check for failures and errors
                failure = testcase.find('failure')
                error = testcase.find('error')
                skipped = testcase.find('skipped')
                
                status = 'passed'
                failure_message = None
                failure_type = None
                failure_stacktrace = None
                
                if failure is not None:
                    status = 'failed'
                    failure_message = failure.get('message', 'No message')
                    failure_type = failure.get('type', 'Unknown')
                    failure_stacktrace = failure.text or 'No stacktrace'
                elif error is not None:
                    status = 'error'
                    failure_message = error.get('message', 'No message')
                    failure_type = error.get('type', 'Unknown')
                    failure_stacktrace = error.text or 'No stacktrace'
                elif skipped is not None:
                    status = 'skipped'
                    failure_message = skipped.get('message', 'Skipped')
                
                test_result = {
                    'test_name': test_name,
                    'class_name': test_classname,
                    'full_name': f"{test_classname}.{test_name}",
                    'status': status,
                    'duration_seconds': test_time,
                    'failure_message': failure_message,
                    'failure_type': failure_type,
                    'failure_stacktrace': failure_stacktrace,
                    'suite_name': suite_name
                }
                
                results.append(test_result)
        
        return results
        
    except ET.ParseError as e:
        print(f"Error parsing XML file {xml_file}: {e}")
        return None
    except Exception as e:
        print(f"Unexpected error parsing {xml_file}: {e}")
        return None

def extract_root_cause(failure_message, failure_stacktrace):
    """Extract a concise root cause from failure message and stacktrace"""
    if not failure_message and not failure_stacktrace:
        return "Unknown error"
    
    # Use failure message if available
    if failure_message:
        # Clean up common patterns
        message = failure_message.strip()
        # Truncate very long messages
        if len(message) > 150:
            message = message[:150] + "..."
        return message
    
    # Extract from stacktrace
    if failure_stacktrace:
        lines = failure_stacktrace.strip().split('\n')
        # Look for the first meaningful line
        for line in lines:
            line = line.strip()
            if line and not line.startswith('at ') and not line.startswith('...'):
                if len(line) > 150:
                    line = line[:150] + "..."
                return line
    
    return "Error details unavailable"

def analyze_batch_results(batch_name, xml_results_dir):
    """Analyze test results for a single batch"""
    xml_dir = Path(xml_results_dir)
    if not xml_dir.exists():
        return None
    
    batch_results = {
        'batch_name': batch_name,
        'total_tests': 0,
        'passed_tests': 0,
        'failed_tests': 0,
        'error_tests': 0,
        'skipped_tests': 0,
        'total_duration_seconds': 0,
        'test_results': [],
        'failure_summary': [],
        'error_patterns': defaultdict(int)
    }
    
    # Find all XML files
    xml_files = list(xml_dir.glob('**/TEST-*.xml'))
    
    if not xml_files:
        print(f"No XML files found in {xml_dir}")
        return batch_results
    
    # Parse each XML file
    for xml_file in xml_files:
        results = parse_junit_xml(xml_file)
        if results:
            batch_results['test_results'].extend(results)
    
    # Analyze results
    for test in batch_results['test_results']:
        batch_results['total_tests'] += 1
        batch_results['total_duration_seconds'] += test['duration_seconds']
        
        if test['status'] == 'passed':
            batch_results['passed_tests'] += 1
        elif test['status'] == 'failed':
            batch_results['failed_tests'] += 1
            
            # Extract root cause
            root_cause = extract_root_cause(test['failure_message'], test['failure_stacktrace'])
            batch_results['failure_summary'].append({
                'test_name': test['full_name'],
                'root_cause': root_cause,
                'failure_type': test['failure_type']
            })
            
            # Track error patterns
            if test['failure_type']:
                batch_results['error_patterns'][test['failure_type']] += 1
            
        elif test['status'] == 'error':
            batch_results['error_tests'] += 1
            
            # Extract root cause
            root_cause = extract_root_cause(test['failure_message'], test['failure_stacktrace'])
            batch_results['failure_summary'].append({
                'test_name': test['full_name'],
                'root_cause': root_cause,
                'failure_type': test['failure_type']
            })
            
            # Track error patterns
            if test['failure_type']:
                batch_results['error_patterns'][test['failure_type']] += 1
                
        elif test['status'] == 'skipped':
            batch_results['skipped_tests'] += 1
    
    return batch_results

def main():
    """Main function to parse all batch results"""
    artifacts_dir = Path('artifacts')
    
    if not artifacts_dir.exists():
        print("Artifacts directory not found. Please run test batches first.")
        return
    
    # Find all batch directories
    batch_dirs = [d for d in artifacts_dir.iterdir() if d.is_dir() and d.name.startswith('batch-')]
    
    if not batch_dirs:
        print("No batch result directories found in artifacts/")
        return
    
    print(f"Found {len(batch_dirs)} batch result directories")
    
    all_results = {
        'analysis_timestamp': '2025-09-19T21:55:07',  # Will be updated
        'total_batches': 0,
        'total_tests': 0,
        'total_passed': 0,
        'total_failed': 0,
        'total_errors': 0,
        'total_skipped': 0,
        'batch_results': [],
        'overall_error_patterns': defaultdict(int),
        'top_failure_causes': []
    }
    
    # Process each batch
    for batch_dir in sorted(batch_dirs):
        batch_name = batch_dir.name.replace('batch-', '')
        xml_results_dir = batch_dir / 'xml-results'
        
        print(f"Processing batch: {batch_name}")
        
        batch_results = analyze_batch_results(batch_name, xml_results_dir)
        
        if batch_results:
            all_results['batch_results'].append(batch_results)
            all_results['total_batches'] += 1
            all_results['total_tests'] += batch_results['total_tests']
            all_results['total_passed'] += batch_results['passed_tests']
            all_results['total_failed'] += batch_results['failed_tests']
            all_results['total_errors'] += batch_results['error_tests']
            all_results['total_skipped'] += batch_results['skipped_tests']
            
            # Aggregate error patterns
            for error_type, count in batch_results['error_patterns'].items():
                all_results['overall_error_patterns'][error_type] += count
    
    # Convert defaultdict to regular dict for JSON serialization
    all_results['overall_error_patterns'] = dict(all_results['overall_error_patterns'])
    
    # Create top failure causes
    failure_causes = defaultdict(int)
    for batch in all_results['batch_results']:
        for failure in batch['failure_summary']:
            cause = failure['root_cause'][:100]  # Truncate for grouping
            failure_causes[cause] += 1
    
    all_results['top_failure_causes'] = sorted(
        [{'cause': cause, 'count': count} for cause, count in failure_causes.items()],
        key=lambda x: x['count'],
        reverse=True
    )[:10]
    
    # Save results
    os.makedirs('results', exist_ok=True)
    with open('results/parsed_test_results.json', 'w') as f:
        json.dump(all_results, f, indent=2, default=str)
    
    # Print summary
    print(f"\n{'='*80}")
    print("TEST RESULTS ANALYSIS SUMMARY")
    print(f"{'='*80}")
    print(f"Total batches processed: {all_results['total_batches']}")
    print(f"Total tests: {all_results['total_tests']}")
    print(f"Passed: {all_results['total_passed']}")
    print(f"Failed: {all_results['total_failed']}")
    print(f"Errors: {all_results['total_errors']}")
    print(f"Skipped: {all_results['total_skipped']}")
    
    if all_results['total_tests'] > 0:
        pass_rate = (all_results['total_passed'] / all_results['total_tests']) * 100
        print(f"Pass rate: {pass_rate:.1f}%")
    
    print(f"\nTop Error Types:")
    for error_type, count in sorted(all_results['overall_error_patterns'].items(), key=lambda x: x[1], reverse=True)[:5]:
        print(f"  {error_type}: {count}")
    
    print(f"\nTop Failure Causes:")
    for cause in all_results['top_failure_causes'][:5]:
        print(f"  {cause['count']}x: {cause['cause']}")
    
    print(f"\nDetailed results saved to: results/parsed_test_results.json")

if __name__ == "__main__":
    main()