/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.tooling.reporting.html

/**
 * Embedded CSS and JavaScript assets for HTML test reports.
 * All assets are inlined to create self-contained HTML files.
 */
internal object HtmlAssets {
  
  /**
   * Main CSS styles for the test report.
   * Includes responsive design, modern styling, and status colors.
   */
  val css = """
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }
    
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      line-height: 1.6;
      color: #e9ecef;
      background-color: #212529;
    }
    
    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }
    
    .header {
      background: #343a40;
      border-radius: 8px;
      padding: 30px;
      margin-bottom: 24px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }
    
    .header h1 {
      font-size: 2rem;
      margin-bottom: 12px;
      color: #f8f9fa;
    }
    
    .header .timestamp {
      color: #adb5bd;
      font-size: 0.9rem;
    }
    
    .summary {
      margin-bottom: 24px;
    }
    
    .summary-bar {
      background: #343a40;
      border-radius: 8px;
      padding: 16px 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 32px;
      flex-wrap: wrap;
      font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
    }
    
    .summary-group {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .summary-group.test-counts {
      flex: 1;
    }
    
    .summary-group.timing {
      flex-shrink: 0;
    }
    
    .summary-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.9rem;
    }
    
    .summary-label {
      color: #adb5bd;
      font-weight: 500;
    }
    
    .summary-value {
      font-weight: 600;
      font-size: 0.9rem;
    }
    
    .summary-item.total .summary-value { color: #f8f9fa; }
    .summary-item.passed .summary-value { color: #28a745; }
    .summary-item.failed .summary-value { color: #dc3545; }
    .summary-item.skipped .summary-value { color: #ffc107; }
    .summary-item.pass-rate .summary-value { color: #007bff; }
    .summary-item.duration .summary-value { color: #6f42c1; }
    
    .controls {
      background: #343a40;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 24px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      align-items: center;
    }
    
    .search-box {
      flex: 1;
      min-width: 250px;
      padding: 10px 16px;
      border: 2px solid #6c757d;
      border-radius: 6px;
      font-size: 14px;
      background: #495057;
      color: #f8f9fa;
      transition: border-color 0.2s ease;
    }
    
    .search-box:focus {
      outline: none;
      border-color: #007bff;
    }
    
    .filter-buttons {
      display: flex;
      gap: 8px;
    }
    
    .filter-btn {
      padding: 8px 16px;
      border: 2px solid;
      border-radius: 6px;
      background: #495057;
      cursor: pointer;
      font-size: 14px;
      transition: all 0.2s ease;
      font-weight: 500;
    }
    
    .filter-btn.all { border-color: #6c757d; color: #6c757d; }
    .filter-btn.passed { border-color: #28a745; color: #28a745; }
    .filter-btn.failed { border-color: #dc3545; color: #dc3545; }
    .filter-btn.skipped { border-color: #ffc107; color: #ffc107; }
    
    .filter-btn.active {
      color: white;
    }
    
    .filter-btn.all.active { background: #6c757d; }
    .filter-btn.passed.active { background: #28a745; }
    .filter-btn.failed.active { background: #dc3545; }
    .filter-btn.skipped.active { background: #ffc107; }
    
    .expand-controls {
      display: flex;
      gap: 8px;
    }
    
    .expand-btn {
      padding: 8px 12px;
      border: 1px solid #6c757d;
      border-radius: 4px;
      background: #495057;
      cursor: pointer;
      font-size: 12px;
      color: #f8f9fa;
      transition: background-color 0.2s ease;
    }
    
    .expand-btn:hover {
      background: #6c757d;
    }
    
    .test-results {
      background: #343a40;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      overflow: hidden;
    }
    
    .test-group {
      border-bottom: 1px solid #495057;
    }
    
    .test-group:last-child {
      border-bottom: none;
    }
    
    .group-header {
      padding: 16px 20px;
      background: #495057;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: space-between;
      transition: background-color 0.2s ease;
    }
    
    .group-header:hover {
      background: #6c757d;
    }
    
    .group-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
    }
    
    .group-toggle {
      transition: transform 0.2s ease;
    }
    
    .group-toggle.collapsed {
      transform: rotate(-90deg);
    }
    
    .group-stats {
      display: flex;
      gap: 12px;
      font-size: 0.85rem;
    }
    
    .stat {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .stat-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }
    
    .stat-dot.passed { background: #28a745; }
    .stat-dot.failed { background: #dc3545; }
    .stat-dot.skipped { background: #ffc107; }
    
    .group-content {
      display: none;
    }
    
    .group-content.expanded {
      display: block;
    }
    
    .test-case {
      padding: 12px 20px;
      border-top: 1px solid #495057;
      transition: background-color 0.2s ease;
      cursor: pointer;
    }
    
    .test-case:first-child {
      border-top: none;
    }
    
    .test-case:hover {
      background: #495057;
    }
    
    .test-case.hidden {
      display: none;
    }
    
    .test-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    
    .test-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }
    
    .test-status-indicator {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: bold;
      flex-shrink: 0;
    }

    .test-status-indicator.passed {
      background: #28a745;
      color: white;
    }
    .test-status-indicator.failed {
      background: #dc3545;
      color: white;
    }
    .test-status-indicator.skipped {
      background: #ffc107;
      color: #333;
    }

    .test-passed {
      color: #28a745;
    }
    .test-failed {
      color: #dc3545;
    }
    .test-skipped {
      color: #ffc107;
    }
    
    .test-name-info {
      flex: 1;
      min-width: 0;
    }
    
    .test-name {
      font-weight: 500;
      margin-bottom: 2px;
    }
    
    .test-meta {
      font-size: 0.8rem;
      color: #adb5bd;
    }
    
    .test-duration {
      font-size: 0.85rem;
      color: #adb5bd;
      font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
      flex-shrink: 0;
    }
    
    .test-details {
      margin-top: 12px;
      display: none;
    }
    
    .test-details.expanded {
      display: block;
    }
    
    .test-error {
      background: #3d1a1a;
      border: 1px solid #5a2a2a;
      border-radius: 6px;
      padding: 16px;
      margin-top: 12px;
    }
    
    .error-message {
      font-weight: 600;
      color: #f5c2c7;
      margin-bottom: 8px;
    }
    
    .stack-trace {
      background: #2d1a1a;
      border: 1px solid #5a2a2a;
      border-radius: 4px;
      padding: 12px;
      font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
      font-size: 0.8rem;
      color: #f5c2c7;
      white-space: pre-wrap;
      overflow-x: auto;
      max-height: 300px;
      overflow-y: auto;
    }
    
    .skip-reason {
      background: #3d3a1a;
      border: 1px solid #5a5a2a;
      border-radius: 6px;
      padding: 12px;
      margin-top: 12px;
      color: #fff3cd;
      font-style: italic;
    }
    
    .no-results {
      text-align: center;
      padding: 60px 20px;
      color: #adb5bd;
    }
    
    .no-results-icon {
      font-size: 3rem;
      margin-bottom: 16px;
      opacity: 0.5;
    }
    
    /* Mobile responsive */
    @media (max-width: 768px) {
      .container {
        padding: 12px;
      }
      
      .summary-bar {
        gap: 16px;
      }
      
      .controls {
        flex-direction: column;
        align-items: stretch;
      }
      
      .search-box {
        min-width: auto;
      }
      
      .filter-buttons,
      .expand-controls {
        justify-content: center;
      }
      
      .group-header,
      .test-case {
        padding: 12px 16px;
      }
      
      .test-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
      }
    }
    
    @media (max-width: 480px) {
      .summary-bar {
        gap: 12px;
      }
      
      .summary-item {
        font-size: 0.8rem;
      }
      
      .filter-buttons {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 8px;
      }
    }
  """.trimIndent()
  
  /**
   * JavaScript for interactive functionality.
   * Includes search, filtering, collapsible sections, and expand/collapse all.
   */
  val javascript = """
    class TestReportController {
      constructor() {
        this.searchInput = document.getElementById('search');
        this.filterButtons = document.querySelectorAll('.filter-btn');
        this.expandAllBtn = document.getElementById('expand-all');
        this.collapseAllBtn = document.getElementById('collapse-all');
        this.testGroups = document.querySelectorAll('.test-group');
        this.testCases = document.querySelectorAll('.test-case');
        
        this.currentFilter = 'all';
        this.searchTerm = '';
        
        this.initializeEventListeners();
        this.initializeGroupToggles();
        this.initializeTestToggles();
        this.updateFilterCounts();
      }
      
      initializeEventListeners() {
        // Search functionality
        this.searchInput.addEventListener('input', (e) => {
          this.searchTerm = e.target.value.toLowerCase();
          this.applyFilters();
        });
        
        // Filter buttons
        this.filterButtons.forEach(btn => {
          btn.addEventListener('click', () => {
            this.setActiveFilter(btn.dataset.filter);
          });
        });
        
        // Expand/collapse controls
        this.expandAllBtn.addEventListener('click', () => this.expandAll());
        this.collapseAllBtn.addEventListener('click', () => this.collapseAll());
      }
      
      initializeGroupToggles() {
        this.testGroups.forEach(group => {
          const header = group.querySelector('.group-header');
          const content = group.querySelector('.group-content');
          const toggle = group.querySelector('.group-toggle');
          
          header.addEventListener('click', () => {
            const isExpanded = content.classList.contains('expanded');
            
            if (isExpanded) {
              content.classList.remove('expanded');
              toggle.classList.add('collapsed');
            } else {
              content.classList.add('expanded');
              toggle.classList.remove('collapsed');
            }
          });
        });
      }
      
      initializeTestToggles() {
        this.testCases.forEach(testCase => {
          const details = testCase.querySelector('.test-details');
          if (details) {
            testCase.addEventListener('click', (e) => {
              e.stopPropagation();
              details.classList.toggle('expanded');
            });
          }
        });
      }
      
      setActiveFilter(filter) {
        this.currentFilter = filter;
        
        // Update button states
        this.filterButtons.forEach(btn => {
          btn.classList.toggle('active', btn.dataset.filter === filter);
        });
        
        this.applyFilters();
      }
      
      applyFilters() {
        let visibleCount = 0;
        
        this.testCases.forEach(testCase => {
          const testName = testCase.querySelector('.test-name').textContent.toLowerCase();
          const testClass = testCase.querySelector('.test-meta').textContent.toLowerCase();
          const testStatus = testCase.dataset.status;
          
          // Apply search filter
          const matchesSearch = this.searchTerm === '' || 
            testName.includes(this.searchTerm) || 
            testClass.includes(this.searchTerm);
          
          // Apply status filter
          const matchesFilter = this.currentFilter === 'all' || testStatus === this.currentFilter;
          
          const isVisible = matchesSearch && matchesFilter;
          testCase.classList.toggle('hidden', !isVisible);
          
          if (isVisible) visibleCount++;
        });
        
        // Show/hide groups based on whether they have visible tests
        this.testGroups.forEach(group => {
          const visibleTests = group.querySelectorAll('.test-case:not(.hidden)');
          const hasVisibleTests = visibleTests.length > 0;
          group.style.display = hasVisibleTests ? 'block' : 'none';
        });
        
        // Show no results message
        this.toggleNoResults(visibleCount === 0);
      }
      
      toggleNoResults(show) {
        let noResultsDiv = document.getElementById('no-results');
        
        if (show && !noResultsDiv) {
          noResultsDiv = document.createElement('div');
          noResultsDiv.id = 'no-results';
          noResultsDiv.className = 'no-results';
          noResultsDiv.innerHTML = `
            <div class="no-results-icon">🔍</div>
            <p>No tests match your current filters</p>
          `;
          document.querySelector('.test-results').appendChild(noResultsDiv);
        } else if (!show && noResultsDiv) {
          noResultsDiv.remove();
        }
      }
      
      expandAll() {
        this.testGroups.forEach(group => {
          const content = group.querySelector('.group-content');
          const toggle = group.querySelector('.group-toggle');
          
          content.classList.add('expanded');
          toggle.classList.remove('collapsed');
        });
      }
      
      collapseAll() {
        this.testGroups.forEach(group => {
          const content = group.querySelector('.group-content');
          const toggle = group.querySelector('.group-toggle');
          
          content.classList.remove('expanded');
          toggle.classList.add('collapsed');
        });
      }
      
      updateFilterCounts() {
        const counts = {
          all: this.testCases.length,
          passed: document.querySelectorAll('[data-status="passed"]').length,
          failed: document.querySelectorAll('[data-status="failed"]').length,
          skipped: document.querySelectorAll('[data-status="skipped"]').length
        };
        
        this.filterButtons.forEach(btn => {
          const filter = btn.dataset.filter;
          const count = counts[filter] || 0;
          const label = btn.textContent.split(' (')[0];
          btn.textContent = `${'$'}{label} (${'$'}{count})`;
        });
      }
    }
    
    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', () => {
      new TestReportController();
    });
  """.trimIndent()
}