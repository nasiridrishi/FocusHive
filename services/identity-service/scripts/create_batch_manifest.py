#!/usr/bin/env python3

import csv
import yaml
from collections import defaultdict

def main():
    # Read the inventory
    test_files = []
    with open('batches/test_inventory.csv', 'r') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            test_files.append(row)
    
    # Group by category and runtime
    batches = []
    
    # Define batch strategy - combine small categories, split large ones
    category_groups = {
        # Fast unit tests - combine multiple categories
        'unit-fast': {
            'categories': ['dto', 'entity', 'util', 'validation'],
            'runtime_filters': ['short'],
            'max_size': 25
        },
        
        # Unit tests - medium runtime
        'unit-medium': {
            'categories': ['dto', 'entity', 'util', 'validation'],
            'runtime_filters': ['medium'],
            'max_size': 20
        },
        
        # Service layer tests
        'service': {
            'categories': ['service'],
            'runtime_filters': ['short', 'medium'],
            'max_size': 15
        },
        
        # Service layer tests - long running
        'service-long': {
            'categories': ['service'],
            'runtime_filters': ['long'],
            'max_size': 10
        },
        
        # Controllers
        'controllers': {
            'categories': ['controller'],
            'runtime_filters': ['short', 'medium'],
            'max_size': 15
        },
        
        # Security tests - split by runtime
        'security-unit': {
            'categories': ['security'],
            'runtime_filters': ['short', 'medium'],
            'max_size': 12
        },
        
        'security-integration': {
            'categories': ['security'],
            'runtime_filters': ['long', 'very-long'],
            'max_size': 8
        },
        
        # Configuration tests
        'config': {
            'categories': ['config'],
            'runtime_filters': ['short', 'medium'],
            'max_size': 20
        },
        
        'config-integration': {
            'categories': ['config'],
            'runtime_filters': ['long'],
            'max_size': 10
        },
        
        # Integration tests - split by runtime
        'integration-medium': {
            'categories': ['integration'],
            'runtime_filters': ['medium'],
            'max_size': 10
        },
        
        'integration-long': {
            'categories': ['integration'],
            'runtime_filters': ['long'],
            'max_size': 8
        },
        
        'integration-very-long': {
            'categories': ['integration'],
            'runtime_filters': ['very-long'],
            'max_size': 5
        },
        
        # Performance tests - separate batches
        'performance': {
            'categories': ['performance'],
            'runtime_filters': ['medium', 'long'],
            'max_size': 8
        },
        
        'performance-long': {
            'categories': ['performance'],
            'runtime_filters': ['very-long'],
            'max_size': 4
        },
        
        # Infrastructure tests
        'infrastructure': {
            'categories': ['repository', 'cache', 'monitoring', 'network', 'interceptor'],
            'runtime_filters': ['short', 'medium', 'long'],
            'max_size': 15
        },
        
        # Compliance and resilience
        'compliance-resilience': {
            'categories': ['compliance', 'resilience', 'concurrency', 'integrity'],
            'runtime_filters': ['short', 'medium', 'long'],
            'max_size': 12
        },
        
        # Miscellaneous
        'misc': {
            'categories': ['other', 'diagnosis', 'exception', 'startup', 'unit'],
            'runtime_filters': ['short', 'medium', 'long'],
            'max_size': 15
        }
    }
    
    batch_counter = 1
    manifest = []
    
    for batch_name, config in category_groups.items():
        # Filter tests based on category and runtime
        candidate_tests = [
            test for test in test_files
            if test['category'] in config['categories'] 
            and test['runtime_estimate'] in config['runtime_filters']
        ]
        
        if not candidate_tests:
            continue
        
        # Split into sub-batches if needed
        max_size = config['max_size']
        sub_batches = [candidate_tests[i:i + max_size] for i in range(0, len(candidate_tests), max_size)]
        
        for i, sub_batch in enumerate(sub_batches):
            actual_batch_name = f"{batch_name}" if len(sub_batches) == 1 else f"{batch_name}-{i+1}"
            
            # Create test patterns for Gradle
            class_patterns = []
            for test in sub_batch:
                # Convert from file path to class pattern
                # e.g., com/focushive/identity/dto/LoginRequestUnitTest.java -> com.focushive.identity.dto.LoginRequestUnitTest
                class_name = test['package_name'].replace('/', '.') + '.' + test['class_name']
                class_patterns.append(class_name)
            
            # Create file list for reference
            test_files_list = [test['relative_path'] for test in sub_batch]
            
            manifest_entry = {
                'name': actual_batch_name,
                'description': f"Tests for {', '.join(config['categories'])} ({', '.join(config['runtime_filters'])} runtime)",
                'categories': config['categories'],
                'runtime_filters': config['runtime_filters'],
                'test_count': len(sub_batch),
                'estimated_runtime_minutes': estimate_batch_runtime(sub_batch),
                'class_patterns': class_patterns,
                'test_files': test_files_list
            }
            
            manifest.append(manifest_entry)
            batch_counter += 1
    
    # Write batch manifest
    with open('batches/batch_manifest.yaml', 'w') as yaml_file:
        yaml.dump({'batches': manifest}, yaml_file, default_flow_style=False, indent=2)
    
    # Print summary
    print(f"\nBatch Manifest Summary:")
    print(f"Total batches: {len(manifest)}")
    print(f"Total tests covered: {sum(batch['test_count'] for batch in manifest)}")
    
    for batch in manifest:
        print(f"  {batch['name']}: {batch['test_count']} tests (~{batch['estimated_runtime_minutes']} min)")
    
    print(f"\nBatch manifest saved to: batches/batch_manifest.yaml")

def estimate_batch_runtime(tests):
    """Estimate batch runtime in minutes based on test runtime estimates"""
    runtime_minutes = {
        'short': 0.1,      # 6 seconds
        'medium': 0.5,     # 30 seconds
        'long': 2.0,       # 2 minutes
        'very-long': 5.0   # 5 minutes
    }
    
    total_minutes = sum(runtime_minutes.get(test['runtime_estimate'], 0.5) for test in tests)
    return round(total_minutes, 1)

if __name__ == "__main__":
    main()