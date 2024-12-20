#!/usr/bin/env python3

import os
import sys
import yaml
import subprocess
import json
import shutil
from datetime import datetime
import time

def load_batch_manifest():
    """Load the batch manifest"""
    with open('batches/batch_manifest.yaml', 'r') as f:
        return yaml.safe_load(f)['batches']

def execute_batch(batch, batch_index, total_batches):
    """Execute a single batch of tests"""
    batch_name = batch['name']
    class_patterns = batch['class_patterns']
    
    print(f"\n{'='*80}")
    print(f"BATCH {batch_index}/{total_batches}: {batch_name}")
    print(f"Description: {batch['description']}")
    print(f"Test count: {batch['test_count']}")
    print(f"Estimated runtime: {batch['estimated_runtime_minutes']} minutes")
    print(f"{'='*80}")
    
    # Create includes for gradle
    includes = []
    for pattern in class_patterns:
        # Convert class pattern to file pattern
        # com.focushive.identity.dto.LoginRequestUnitTest -> **/LoginRequestUnitTest.class
        class_name = pattern.split('.')[-1]
        includes.append(f"**/{class_name}.class")
    
    # Create gradle command
    gradle_cmd = ["./gradlew", "test"]
    
    # Add test filtering
    for include in includes:
        gradle_cmd.extend(["--tests", include.replace('.class', '')])
    
    # Configure output directories
    batch_reports_dir = f"build/reports/tests/batch-{batch_name}"
    batch_results_dir = f"build/test-results/batch-{batch_name}"
    
    # Set system properties for batch-specific output
    gradle_cmd.extend([
        f"-Dtest.reports.dir={batch_reports_dir}",
        f"-Dtest.results.dir={batch_results_dir}",
        "--continue",  # Continue on test failures
        "--no-build-cache"
    ])
    
    start_time = time.time()
    
    print(f"Running command: {' '.join(gradle_cmd)}")
    
    # Execute the gradle command
    try:
        result = subprocess.run(
            gradle_cmd, 
            capture_output=True, 
            text=True, 
            timeout=30*60  # 30 minute timeout
        )
        
        end_time = time.time()
        duration_minutes = (end_time - start_time) / 60
        
        # Store results
        batch_result = {
            'batch_name': batch_name,
            'batch_index': batch_index,
            'duration_minutes': round(duration_minutes, 2),
            'exit_code': result.returncode,
            'success': result.returncode == 0,
            'stdout': result.stdout,
            'stderr': result.stderr,
            'timestamp': datetime.now().isoformat()
        }
        
        # Print summary
        if result.returncode == 0:
            print(f"✅ BATCH {batch_name} PASSED in {duration_minutes:.1f} minutes")
        else:
            print(f"❌ BATCH {batch_name} FAILED in {duration_minutes:.1f} minutes")
            print(f"Exit code: {result.returncode}")
            if result.stderr:
                print("STDERR:", result.stderr[-1000:])  # Last 1000 chars
        
        # Copy test results to artifacts
        artifacts_dir = f"artifacts/batch-{batch_name}"
        os.makedirs(artifacts_dir, exist_ok=True)
        
        # Copy HTML reports if they exist
        if os.path.exists("build/reports/tests/test"):
            shutil.copytree("build/reports/tests/test", f"{artifacts_dir}/html-reports", dirs_exist_ok=True)
        
        # Copy XML results if they exist
        if os.path.exists("build/test-results/test"):
            shutil.copytree("build/test-results/test", f"{artifacts_dir}/xml-results", dirs_exist_ok=True)
        
        # Save batch result
        with open(f"{artifacts_dir}/batch_result.json", 'w') as f:
            json.dump(batch_result, f, indent=2)
        
        return batch_result
        
    except subprocess.TimeoutExpired:
        end_time = time.time()
        duration_minutes = (end_time - start_time) / 60
        
        print(f"⏰ BATCH {batch_name} TIMED OUT after {duration_minutes:.1f} minutes")
        
        return {
            'batch_name': batch_name,
            'batch_index': batch_index,
            'duration_minutes': round(duration_minutes, 2),
            'exit_code': -1,
            'success': False,
            'stdout': "",
            'stderr': "Test batch timed out after 30 minutes",
            'timestamp': datetime.now().isoformat()
        }
    
    except Exception as e:
        print(f"💥 BATCH {batch_name} ERROR: {str(e)}")
        
        return {
            'batch_name': batch_name,
            'batch_index': batch_index,
            'duration_minutes': 0,
            'exit_code': -2,
            'success': False,
            'stdout': "",
            'stderr': f"Execution error: {str(e)}",
            'timestamp': datetime.now().isoformat()
        }

def main():
    """Main execution function"""
    # Load manifest
    batches = load_batch_manifest()
    
    print(f"Starting test execution for {len(batches)} batches")
    print(f"Execution started at: {datetime.now()}")
    
    # Check if we should run specific batches
    if len(sys.argv) > 1:
        if sys.argv[1] == "--fast":
            # Run only fast batches first
            batches = [b for b in batches if 'fast' in b['name'] or b['estimated_runtime_minutes'] < 5]
            print(f"Fast mode: Running {len(batches)} fast batches")
        elif sys.argv[1] == "--sample":
            # Run first 3 batches as a sample
            batches = batches[:3]
            print(f"Sample mode: Running first {len(batches)} batches")
    
    # Execute all batches
    results = []
    total_batches = len(batches)
    
    for i, batch in enumerate(batches, 1):
        batch_result = execute_batch(batch, i, total_batches)
        results.append(batch_result)
        
        # Small pause between batches to avoid resource contention
        if i < total_batches:
            print("Waiting 10 seconds before next batch...")
            time.sleep(10)
    
    # Save overall results
    os.makedirs('results', exist_ok=True)
    overall_results = {
        'execution_timestamp': datetime.now().isoformat(),
        'total_batches': len(results),
        'successful_batches': len([r for r in results if r['success']]),
        'failed_batches': len([r for r in results if not r['success']]),
        'total_duration_minutes': sum(r['duration_minutes'] for r in results),
        'batch_results': results
    }
    
    with open('results/batch_execution_results.json', 'w') as f:
        json.dump(overall_results, f, indent=2)
    
    # Print final summary
    print(f"\n{'='*80}")
    print("FINAL SUMMARY")
    print(f"{'='*80}")
    print(f"Total batches: {overall_results['total_batches']}")
    print(f"Successful: {overall_results['successful_batches']}")
    print(f"Failed: {overall_results['failed_batches']}")
    print(f"Total runtime: {overall_results['total_duration_minutes']:.1f} minutes")
    
    # List failed batches
    failed = [r for r in results if not r['success']]
    if failed:
        print(f"\nFailed batches:")
        for result in failed:
            print(f"  - {result['batch_name']} (exit code: {result['exit_code']})")
    
    print(f"\nResults saved to: results/batch_execution_results.json")
    print(f"Individual batch artifacts saved to: artifacts/")

if __name__ == "__main__":
    main()