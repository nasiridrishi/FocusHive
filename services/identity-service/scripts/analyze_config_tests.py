#!/usr/bin/env python3

import os
import re
from pathlib import Path
from collections import defaultdict

def analyze_config_file(file_path):
    """Analyze a config file to understand its purpose"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        file_name = os.path.basename(file_path).replace('.java', '')
        
        analysis = {
            'file_name': file_name,
            'file_path': file_path,
            'line_count': len(content.split('\n')),
            'purpose': 'unknown',
            'type': 'unknown',
            'beans_defined': [],
            'annotations': [],
            'imports': [],
            'is_actual_test': False,
            'is_config_class': False
        }
        
        # Extract annotations
        annotations = re.findall(r'@(\w+)', content)
        analysis['annotations'] = list(set(annotations))
        
        # Extract imports
        imports = re.findall(r'import\s+([^;]+);', content)
        analysis['imports'] = imports
        
        # Check if it's a test configuration class
        if '@TestConfiguration' in content or '@Configuration' in content:
            analysis['is_config_class'] = True
        
        # Check if it has actual @Test methods
        if '@Test' in content and 'void test' in content.lower():
            analysis['is_actual_test'] = True
        
        # Determine purpose based on file name and content
        name_lower = file_name.lower()
        
        if 'test' in name_lower and ('config' in name_lower or 'TestConfiguration' in content):
            if analysis['is_actual_test']:
                analysis['type'] = 'config_test'  # Tests configuration classes
                analysis['purpose'] = 'Tests configuration setup'
            else:
                analysis['type'] = 'test_config'  # Provides configuration for tests
                analysis['purpose'] = 'Provides test configuration'
        elif analysis['is_actual_test']:
            analysis['type'] = 'config_test'
            analysis['purpose'] = 'Tests configuration functionality'
        else:
            analysis['type'] = 'test_config'
            analysis['purpose'] = 'Utility configuration for tests'
        
        # Extract bean definitions
        bean_matches = re.findall(r'@Bean[^}]*?(\w+)\s*\([^}]*?\)', content, re.DOTALL)
        analysis['beans_defined'] = bean_matches
        
        # More specific purpose detection
        content_lower = content.lower()
        if 'oauth' in name_lower or 'oauth' in content_lower:
            analysis['purpose'] += ' (OAuth2 related)'
        elif 'security' in name_lower or 'security' in content_lower:
            analysis['purpose'] += ' (Security related)'
        elif 'database' in name_lower or 'jpa' in name_lower or 'postgres' in name_lower:
            analysis['purpose'] += ' (Database related)'
        elif 'container' in name_lower or 'testcontainer' in content_lower:
            analysis['purpose'] += ' (TestContainer related)'
        elif 'cache' in name_lower or 'redis' in content_lower:
            analysis['purpose'] += ' (Cache related)'
        elif 'minimal' in name_lower:
            analysis['purpose'] += ' (Minimal setup)'
        elif 'base' in name_lower:
            analysis['purpose'] += ' (Base configuration)'
        elif 'comprehensive' in name_lower or 'unified' in name_lower:
            analysis['purpose'] += ' (Comprehensive setup)'
        
        return analysis
        
    except Exception as e:
        return {
            'file_name': os.path.basename(file_path).replace('.java', ''),
            'file_path': file_path,
            'error': str(e),
            'purpose': 'Error reading file'
        }

def main():
    """Analyze all config test files"""
    
    # Find all config-related test files
    config_files = []
    
    search_patterns = [
        'src/test/**/config/*Config*.java',
        'src/test/**/*TestConfig*.java',
        'src/test/**/*ConfigTest*.java'
    ]
    
    for pattern in search_patterns:
        config_files.extend(Path('.').glob(pattern))
    
    # Remove duplicates
    config_files = list(set(str(f) for f in config_files))
    config_files.sort()
    
    print(f"🔍 Found {len(config_files)} config-related test files")
    print(f"{'='*80}")
    
    analyses = []
    for file_path in config_files:
        analysis = analyze_config_file(file_path)
        analyses.append(analysis)
    
    # Categorize by type and purpose
    by_type = defaultdict(list)
    by_purpose = defaultdict(list)
    
    for analysis in analyses:
        by_type[analysis.get('type', 'unknown')].append(analysis)
        by_purpose[analysis.get('purpose', 'unknown')].append(analysis)
    
    print(f"\n📊 ANALYSIS RESULTS")
    print(f"{'='*80}")
    
    print(f"\n🏷️  By Type:")
    for type_name, files in by_type.items():
        print(f"  {type_name}: {len(files)} files")
        for f in files[:5]:  # Show first 5
            print(f"    • {f['file_name']} ({f.get('line_count', 0)} lines)")
        if len(files) > 5:
            print(f"    ... and {len(files) - 5} more")
        print()
    
    print(f"\n🎯 By Purpose:")
    purpose_groups = defaultdict(list)
    for analysis in analyses:
        main_purpose = analysis.get('purpose', 'unknown').split('(')[0].strip()
        purpose_groups[main_purpose].append(analysis)
    
    for purpose, files in purpose_groups.items():
        print(f"  {purpose}: {len(files)} files")
        for f in sorted(files, key=lambda x: x.get('line_count', 0), reverse=True)[:3]:
            print(f"    • {f['file_name']} ({f.get('line_count', 0)} lines)")
        print()
    
    # Identify potential redundancy
    print(f"\n🚨 REDUNDANCY ANALYSIS")
    print(f"{'='*80}")
    
    # Group by similar purposes
    similar_purposes = defaultdict(list)
    for analysis in analyses:
        purpose = analysis.get('purpose', '')
        if 'OAuth' in purpose:
            similar_purposes['OAuth2'].append(analysis)
        elif 'Security' in purpose:
            similar_purposes['Security'].append(analysis)
        elif 'Database' in purpose:
            similar_purposes['Database'].append(analysis)
        elif 'TestContainer' in purpose:
            similar_purposes['TestContainer'].append(analysis)
        elif 'test configuration' in purpose:
            similar_purposes['Test Configuration'].append(analysis)
    
    for category, files in similar_purposes.items():
        if len(files) > 2:  # More than 2 is potentially redundant
            print(f"\n⚠️  {category} configs ({len(files)} files):")
            for f in files:
                actual_test = "✓" if f.get('is_actual_test') else "✗"
                config_class = "✓" if f.get('is_config_class') else "✗"
                print(f"    • {f['file_name']} (Test:{actual_test} Config:{config_class} {f.get('line_count', 0)} lines)")
    
    # Identify potential deletions
    print(f"\n🗑️  CLEANUP RECOMMENDATIONS")
    print(f"{'='*80}")
    
    # Find config classes that don't define beans and aren't tests
    empty_configs = []
    test_configs_without_tests = []
    very_small_configs = []
    
    for analysis in analyses:
        if analysis.get('line_count', 0) < 30:
            very_small_configs.append(analysis)
        
        if analysis.get('type') == 'test_config' and not analysis.get('beans_defined') and analysis.get('line_count', 0) < 50:
            empty_configs.append(analysis)
        
        if 'TestConfig' in analysis.get('file_name', '') and not analysis.get('is_actual_test') and analysis.get('line_count', 0) < 100:
            test_configs_without_tests.append(analysis)
    
    if empty_configs:
        print(f"\n💡 Potentially empty config classes ({len(empty_configs)}):")
        for config in empty_configs:
            print(f"  • {config['file_name']} ({config.get('line_count', 0)} lines) - {config.get('purpose', '')}")
    
    if very_small_configs:
        print(f"\n💡 Very small config files ({len(very_small_configs)} files <30 lines):")
        for config in very_small_configs:
            print(f"  • {config['file_name']} ({config.get('line_count', 0)} lines)")
    
    # Summary
    actual_tests = [a for a in analyses if a.get('is_actual_test')]
    config_classes = [a for a in analyses if a.get('is_config_class') and not a.get('is_actual_test')]
    
    print(f"\n📋 SUMMARY")
    print(f"{'='*80}")
    print(f"Total config files: {len(analyses)}")
    print(f"Actual test files (testing config): {len(actual_tests)}")
    print(f"Configuration classes (for tests): {len(config_classes)}")
    print(f"Average file size: {sum(a.get('line_count', 0) for a in analyses) // len(analyses)} lines")
    
    largest_files = sorted(analyses, key=lambda x: x.get('line_count', 0), reverse=True)[:5]
    print(f"\nLargest config files:")
    for f in largest_files:
        print(f"  • {f['file_name']}: {f.get('line_count', 0)} lines - {f.get('purpose', '')}")

if __name__ == "__main__":
    main()