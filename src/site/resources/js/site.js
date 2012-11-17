/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/* Executed on page load. */
$(document).ready(function() {

	//
	// This is a hack to enable google-code-prettify to work with maven.
	//
	// The problem is that maven, when building the site, replaces:
	// <pre class="prettyprint">...</pre>
	// with:
	// <div class="prettyprint"><pre>...</pre></div>
	//
	// Effectively, it removes the class parameter from the <pre> element, which
	// is required for google-code-prettify to work.
	// 
	// This hack restores the class of all <pre> elements which are the child of 
	// a <div class="prettyprint">.
	//
	$('pre').each(function() {
		var parent = $(this).parent();
		
		if (parent.hasClass('prettyprint')) {
			parent.removeClass('prettyprint');
			$(this).addClass('prettyprint');
		}
		
		if (parent.hasClass('linenums')) {
			parent.removeClass('linenums');
			$(this).addClass('linenums');
		}
	})
	
	// Hack to add default visuals to tables
	$('table').each(function() {
		if ($(this).hasClass('bodyTable')) {
			
			// Remove border="1" which is added by maven
			this.border = 0;
			
			// Add bootstrap table styling
			$(this).addClass('table table-striped table-bordered');
		}
	});
	
	// Render tabs
	$('.auto-tabs').each(function(groupid) {

		// Find tab bar
		$(this).find('ul').each(function() {

			// Add styling
			$(this).addClass('nav nav-tabs');

			// Go tab bar items
			$(this).find('li').each(function(itemid) {
			
				// Set first tab as active
				if (itemid == 0) {
					$(this).addClass('active');
				}
				
				// Replace text with a link to tab contents
				var name = $(this).html();
				var link = $('<a>')
					.attr('href', '#' + 'tab-' + groupid + '-' + itemid)
					.attr('data-toggle', 'tab')
					.html(name);
				$(this).html(link);
			});
		});
		
		// Find tab contents
		$(this).find('.tab-content .tab-pane').each(function(itemid) {
			
			// Set first tab as active
			if (itemid == 0) {
				$(this).addClass('active');
			}
			
			// Set the tab id
			$(this).attr('id', 'tab-' + groupid + '-' + itemid);
		});
	});
	
	// Make external links open in new tab
	$('a.external').attr('target', '_blank');
	
	// Trigger prettyprint
	prettyPrint();
});
