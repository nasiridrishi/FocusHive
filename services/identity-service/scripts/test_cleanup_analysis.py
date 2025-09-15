#!/usr/bin/env python3

import os
import re
import csv
from pathlib import Path
from collections import defaultdict, Counter
import subprocess

def analyze_test_files():
    """Analyze test files for potential redundancy and cleanup opportunities"""
    
    # Read the test inventory
    test_files = []
    with open('batches/test_inventory.csv', 'r') as f:
        reader = csv.DictReader(f)
        test_files = list(reader)
    
    analysis = {
        'total_files': len(test_files),
        'category_distribution': Counter(),
        'potential_duplicates': [],
        'config_overload': [],
        'container_test_redundancy': [],
        'unit_vs_integration_duplicates': [],
        'oversized_test_files': [],
        'test_config_proliferation': [],
        'recommended_deletions': [],
        'recommended_consolidations': [],
        'cleanup_summary': {}
    }
    
    # Category analysis
    for test in test_files:
        analysis['category_distribution'][test['category']] += 1
    
    # Group by similar names to find potential duplicates
    name_groups = defaultdict(list)
    for test in test_files:
        base_name = test['class_name'].replace('Test', '').replace('UnitTest', '').replace('IntegrationTest', '')
        name_groups[base_name].append(test)
    
    # Identify potential duplicates
    for base_name, tests in name_groups.items():
        if len(tests) > 1:
            # Check if they're testing the same thing but with different approaches
            test_types = [t['class_name'] for t in tests]
            
            # Look for entity duplicates (both Test and UnitTest versions)
            entity_tests = [t for t in tests if 'entity' in t['category']]
            if len(entity_tests) > 1:
                analysis['potential_duplicates'].append({
                    'base_name': base_name,
                    'tests': [t['class_name'] for t in entity_tests],
                    'category': 'entity',
                    'reason': 'Multiple entity tests for same class'
                })
            
            # Look for controller test proliferation
            controller_tests = [t for t in tests if 'controller' in t['category']]
            if len(controller_tests) > 2:  # More than 2 is likely excessive
                analysis['potential_duplicates'].append({
                    'base_name': base_name,
                    'tests': [t['class_name'] for t in controller_tests],
                    'category': 'controller',
                    'reason': 'Excessive controller test variations'
                })
    
    # Analyze config test proliferation
    config_tests = [t for t in test_files if t['category'] == 'config']
    config_patterns = defaultdict(list)
    
    for test in config_tests:
        # Extract config type patterns
        name = test['class_name'].lower()
        if 'oauth' in name:
            config_patterns['oauth'].append(test)
        elif 'security' in name:
            config_patterns['security'].append(test)
        elif 'test' in name and 'config' in name:
            config_patterns['test_config'].append(test)
        elif 'minimal' in name:
            config_patterns['minimal'].append(test)
        elif 'base' in name:
            config_patterns['base'].append(test)
        else:
            config_patterns['other'].append(test)
    
    # Identify config overload
    for pattern, tests in config_patterns.items():
        if len(tests) > 3:  # More than 3 similar config classes is likely excessive
            analysis['config_overload'].append({
                'pattern': pattern,
                'count': len(tests),
                'tests': [t['class_name'] for t in tests],
                'reason': f'Too many {pattern} configuration classes'
            })
    
    # Analyze TestContainers redundancy
    container_tests = [t for t in test_files if 'container' in t['class_name'].lower() or 'testcontainer' in t['class_name'].lower()]
    if len(container_tests) > 5:  # Likely too many container setup tests
        analysis['container_test_redundancy'] = {
            'count': len(container_tests),
            'tests': [t['class_name'] for t in container_tests],
            'reason': 'Excessive TestContainer setup/verification tests'
        }
    
    # Find unit vs integration test overlaps
    for base_name, tests in name_groups.items():
        unit_tests = [t for t in tests if 'unit' in t['class_name'].lower()]
        integration_tests = [t for t in tests if 'integration' in t['class_name'].lower() or t['category'] == 'integration']
        
        if unit_tests and integration_tests:
            analysis['unit_vs_integration_duplicates'].append({
                'base_name': base_name,
                'unit_tests': [t['class_name'] for t in unit_tests],
                'integration_tests': [t['class_name'] for t in integration_tests],
                'reason': 'May have overlapping test coverage'
            })
    
    # Identify oversized test files (by line count if available)
    large_test_candidates = []
    for test in test_files:
        try:
            file_path = test['file_path']
            if os.path.exists(file_path):
                line_count = sum(1 for _ in open(file_path, 'r', encoding='utf-8', errors='ignore'))
                if line_count > 500:  # Large test files
                    large_test_candidates.append({
                        'class_name': test['class_name'],
                        'line_count': line_count,
                        'category': test['category']
                    })
        except:
            pass
    
    analysis['oversized_test_files'] = sorted(large_test_candidates, key=lambda x: x['line_count'], reverse=True)
    
    # Generate specific recommendations
    generate_recommendations(analysis, test_files)
    
    return analysis

def generate_recommendations(analysis, test_files):
    """Generate specific recommendations for test cleanup"""
    
    recommendations = []
    
    # Config cleanup recommendations
    if analysis['config_overload']:
        recommendations.append({
            'type': 'CONFIG_CONSOLIDATION',
            'priority': 'HIGH',
            'description': 'Consolidate redundant test configuration classes',
            'action': 'Merge similar config classes into unified configurations',
            'estimated_savings': f"{sum(len(item['tests']) for item in analysis['config_overload']) - len(analysis['config_overload'])} files"
        })
    
    # Entity test consolidation
    entity_duplicates = [d for d in analysis['potential_duplicates'] if d['category'] == 'entity']
    if entity_duplicates:
        recommendations.append({
            'type': 'ENTITY_TEST_CONSOLIDATION', 
            'priority': 'MEDIUM',
            'description': 'Consolidate duplicate entity tests',
            'action': 'Choose either Test or UnitTest version for each entity, not both',
            'estimated_savings': f"{sum(len(d['tests'])-1 for d in entity_duplicates)} files"
        })
    
    # Controller test cleanup
    controller_duplicates = [d for d in analysis['potential_duplicates'] if d['category'] == 'controller']
    if controller_duplicates:
        recommendations.append({
            'type': 'CONTROLLER_TEST_CONSOLIDATION',
            'priority': 'HIGH', 
            'description': 'Reduce excessive controller test variations',
            'action': 'Keep one comprehensive controller test per controller, remove minimal/specialized versions',
            'estimated_savings': f"{sum(len(d['tests'])-1 for d in controller_duplicates)} files"
        })
    
    # Container test cleanup
    if analysis['container_test_redundancy']:
        recommendations.append({
            'type': 'CONTAINER_TEST_CLEANUP',
            'priority': 'MEDIUM',
            'description': 'Consolidate TestContainer setup tests',
            'action': 'Keep 1-2 comprehensive container tests, remove verification/demo tests',
            'estimated_savings': f"{analysis['container_test_redundancy']['count'] - 2} files"
        })
    
    # Large file refactoring
    if len(analysis['oversized_test_files']) > 5:
        recommendations.append({
            'type': 'LARGE_FILE_REFACTORING',
            'priority': 'LOW',
            'description': 'Break down oversized test files',
            'action': 'Split large test files into focused test classes',
            'estimated_savings': 'Better maintainability, not fewer files'
        })
    
    analysis['recommendations'] = recommendations
    
    # Calculate total potential file reduction
    total_reduction = 0
    for rec in recommendations:
        if 'estimated_savings' in rec and 'files' in rec['estimated_savings']:
            try:
                total_reduction += int(rec['estimated_savings'].split()[0])
            except:
                pass
    
    analysis['cleanup_summary'] = {
        'current_file_count': len(test_files),
        'potential_file_reduction': total_reduction,
        'percentage_reduction': f"{(total_reduction / len(test_files)) * 100:.1f}%" if total_reduction > 0 else "0%",
        'recommended_final_count': len(test_files) - total_reduction
    }

def generate_specific_deletion_list(analysis):
    """Generate specific list of files that could be deleted"""
    
    deletion_candidates = []
    
    # Entity test duplicates - prefer UnitTest over Test
    for duplicate in analysis['potential_duplicates']:
        if duplicate['category'] == 'entity':
            tests = duplicate['tests']
            # Keep UnitTest, delete plain Test
            for test_name in tests:
                if test_name.endswith('Test') and not test_name.endswith('UnitTest'):
                    if any(t.endswith('UnitTest') for t in tests):
                        deletion_candidates.append({
                            'file': test_name,
                            'reason': 'Duplicate entity test - keep UnitTest version',
                            'category': 'entity'
                        })
    
    # Config cleanup - keep base configs, delete specialized ones
    config_to_keep = ['BaseTestConfig', 'TestConfig', 'MinimalTestConfig', 'UnifiedTestConfig']
    for overload in analysis['config_overload']:
        tests = overload['tests']
        for test_name in tests:
            if test_name not in config_to_keep and 'TestConfig' in test_name:
                deletion_candidates.append({
                    'file': test_name,
                    'reason': f'Redundant {overload["pattern"]} config class',
                    'category': 'config'
                })
    
    # Container test cleanup
    container_tests_to_keep = ['PostgreSQLTestContainerConfig', 'TestContainersConfig']
    if analysis['container_test_redundancy']:
        for test_name in analysis['container_test_redundancy']['tests']:
            if test_name not in container_tests_to_keep:
                deletion_candidates.append({
                    'file': test_name,
                    'reason': 'Redundant TestContainer setup/verification test',
                    'category': 'container'
                })
    
    # Controller test cleanup - keep one main test per controller
    controller_keeps = ['AuthControllerTest', 'PersonaControllerTest', 'PrivacyControllerTest']
    for duplicate in analysis['potential_duplicates']:
        if duplicate['category'] == 'controller':
            tests = duplicate['tests']
            base_name = duplicate['base_name']
            main_test = f"{base_name}Test"
            
            for test_name in tests:
                if test_name != main_test and test_name not in controller_keeps:
                    deletion_candidates.append({
                        'file': test_name,
                        'reason': f'Redundant controller test variation - keep {main_test}',
                        'category': 'controller'
                    })
    
    return deletion_candidates

def main():
    """Main analysis function"""
    
    print("🔍 Analyzing test suite for cleanup opportunities...")
    
    analysis = analyze_test_files()
    
    print(f"\n📊 TEST SUITE ANALYSIS RESULTS")
    print(f"{'='*60}")
    
    print(f"\n📈 Current State:")
    print(f"  Total test files: {analysis['total_files']}")
    
    print(f"\n📋 Category Distribution:")
    for category, count in analysis['category_distribution'].most_common():
        print(f"  {category}: {count} tests")
    
    print(f"\n🚨 Issues Identified:")
    
    if analysis['potential_duplicates']:
        print(f"\n  Potential Duplicates: {len(analysis['potential_duplicates'])} groups")
        for dup in analysis['potential_duplicates'][:5]:  # Show first 5
            print(f"    • {dup['base_name']}: {len(dup['tests'])} variations ({dup['reason']})")
    
    if analysis['config_overload']:
        print(f"\n  Config Overload: {len(analysis['config_overload'])} patterns")
        for overload in analysis['config_overload']:
            print(f"    • {overload['pattern']}: {overload['count']} classes")
    
    if analysis['container_test_redundancy']:
        print(f"\n  Container Test Redundancy: {analysis['container_test_redundancy']['count']} tests")
    
    if analysis['oversized_test_files']:
        print(f"\n  Oversized Test Files: {len(analysis['oversized_test_files'])} files >500 lines")
        for large in analysis['oversized_test_files'][:3]:  # Show top 3
            print(f"    • {large['class_name']}: {large['line_count']} lines")
    
    print(f"\n🎯 Recommendations:")
    if 'recommendations' in analysis:
        for rec in analysis['recommendations']:
            print(f"  {rec['priority']}: {rec['description']}")
            print(f"    └─ {rec['action']}")
            print(f"    └─ Savings: {rec['estimated_savings']}")
            print()
    
    print(f"💡 Cleanup Summary:")
    summary = analysis['cleanup_summary']
    print(f"  Current files: {summary['current_file_count']}")
    print(f"  Potential reduction: {summary['potential_file_reduction']} files ({summary['percentage_reduction']})")
    print(f"  Recommended target: {summary['recommended_final_count']} files")
    
    # Generate specific deletion list
    deletion_candidates = generate_specific_deletion_list(analysis)
    
    print(f"\n🗑️  Specific Deletion Candidates ({len(deletion_candidates)} files):")
    
    # Group by category
    by_category = defaultdict(list)
    for candidate in deletion_candidates:
        by_category[candidate['category']].append(candidate)
    
    for category, candidates in by_category.items():
        print(f"\n  {category.upper()} ({len(candidates)} files):")
        for candidate in candidates:
            print(f"    ❌ {candidate['file']} - {candidate['reason']}")
    
    # Save detailed results
    os.makedirs('reports', exist_ok=True)
    
    import json
    with open('reports/test_cleanup_analysis.json', 'w') as f:
        json.dump(analysis, f, indent=2, default=str)
    
    # Save deletion candidates as CSV
    if deletion_candidates:
        with open('reports/deletion_candidates.csv', 'w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=['file', 'reason', 'category'])
            writer.writeheader()
            writer.writerows(deletion_candidates)
    
    print(f"\n📄 Detailed analysis saved to:")
    print(f"  • reports/test_cleanup_analysis.json")
    if deletion_candidates:
        print(f"  • reports/deletion_candidates.csv")
    
    print(f"\n🔧 Next Steps:")
    print(f"  1. Review the deletion candidates list")
    print(f"  2. Backup the project before making changes")  
    print(f"  3. Start with config consolidation (highest impact)")
    print(f"  4. Remove entity test duplicates")
    print(f"  5. Consolidate controller test variations")
    print(f"  6. Clean up container test redundancy")
    print(f"  7. Re-run test batches to verify nothing breaks")

if __name__ == "__main__":
    main()