#!/usr/bin/env python3

import os
import re
import csv
from pathlib import Path

def categorize_test(filepath):
    """Categorize test files based on their package path"""
    # Extract package path from file path
    path_parts = filepath.split('/')
    
    # Find package parts after 'java' directory
    try:
        java_index = path_parts.index('java')
        package_parts = path_parts[java_index + 1:-1]  # Exclude filename
    except ValueError:
        package_parts = []
    
    if len(package_parts) < 4:  # Not enough package depth
        return 'other'
    
    # Get the category after com.focushive.identity.*
    if len(package_parts) >= 4 and package_parts[3] in ['dto', 'entity', 'service', 'controller', 'integration', 'security', 'performance', 'config', 'cache', 'compliance', 'concurrency', 'diagnosis', 'exception', 'interceptor', 'monitoring', 'network', 'repository', 'resilience', 'startup', 'unit', 'util', 'validation']:
        return package_parts[3]
    elif len(package_parts) >= 4:
        return package_parts[3]  # Any other direct subfolder
    else:
        return 'other'

def estimate_runtime(filepath):
    """Estimate test runtime based on category and naming patterns"""
    category = categorize_test(filepath)
    filename = os.path.basename(filepath)
    
    # Integration and performance tests typically take longer
    if 'integration' in category.lower() or 'Integration' in filename:
        return 'long'  # 30+ seconds
    elif 'performance' in category.lower() or 'Performance' in filename or 'Load' in filename:
        return 'very-long'  # 60+ seconds
    elif 'Test' in filename and ('Unit' in filename or category in ['dto', 'entity', 'util']):
        return 'short'  # < 5 seconds
    else:
        return 'medium'  # 5-30 seconds

def main():
    # Find all test files
    test_files = []
    src_test_dir = Path('src/test/java')
    
    if not src_test_dir.exists():
        print("Test directory not found!")
        return
    
    for test_file in src_test_dir.rglob('*.java'):
        if 'Test' in test_file.name:
            relative_path = str(test_file.relative_to(src_test_dir))
            category = categorize_test(str(test_file))
            runtime = estimate_runtime(str(test_file))
            
            # Extract class name
            class_name = test_file.stem
            
            # Extract full package name
            package_parts = relative_path.split('/')[:-1]
            package_name = '.'.join(package_parts)
            
            test_files.append({
                'file_path': str(test_file),
                'relative_path': relative_path,
                'class_name': class_name,
                'package_name': package_name,
                'category': category,
                'runtime_estimate': runtime
            })
    
    # Sort by category and then by class name
    test_files.sort(key=lambda x: (x['category'], x['class_name']))
    
    # Write to CSV
    output_file = 'batches/test_inventory.csv'
    with open(output_file, 'w', newline='') as csvfile:
        fieldnames = ['file_path', 'relative_path', 'class_name', 'package_name', 'category', 'runtime_estimate']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        
        writer.writeheader()
        for test in test_files:
            writer.writerow(test)
    
    # Print summary
    print(f"\nTest Inventory Summary:")
    print(f"Total test files: {len(test_files)}")
    
    category_counts = {}
    runtime_counts = {}
    
    for test in test_files:
        category = test['category']
        runtime = test['runtime_estimate']
        
        category_counts[category] = category_counts.get(category, 0) + 1
        runtime_counts[runtime] = runtime_counts.get(runtime, 0) + 1
    
    print(f"\nBy Category:")
    for category, count in sorted(category_counts.items()):
        print(f"  {category}: {count} tests")
    
    print(f"\nBy Runtime Estimate:")
    for runtime, count in sorted(runtime_counts.items()):
        print(f"  {runtime}: {count} tests")
    
    print(f"\nInventory saved to: {output_file}")

if __name__ == "__main__":
    main()